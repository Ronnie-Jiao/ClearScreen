package com.clearscreen.prototype.backend

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.clearscreen.prototype.BuildConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.util.ArrayDeque
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Accessibility-based ad action detector.
 *
 * The detector deliberately uses a layered approach: explicit action labels and
 * resource ids first, then the surrounding screen/ancestor context, and finally
 * the target geometry. This borrows the useful ideas behind open-source helpers
 * without copying a third-party rules engine or its licensed rule data.
 */
class ClearScreenAccessibilityService : AccessibilityService() {
  private lateinit var store: ClearScreenStore
  private val handler = Handler(Looper.getMainLooper())
  // ML Kit may dispatch completion callbacks after the service is destroyed.
  // Keep this daemon worker alive long enough to drain them, then let it time out.
  private val ocrExecutor = ThreadPoolExecutor(
    0,
    1,
    OCR_EXECUTOR_KEEP_ALIVE_SECONDS,
    TimeUnit.SECONDS,
    LinkedBlockingQueue(),
    { task -> Thread(task, "clearscreen-ocr").apply { isDaemon = true } },
  ).apply { allowCoreThreadTimeOut(true) }
  private val textRecognizerDelegate = lazy {
    TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
  }
  private val textRecognizer by textRecognizerDelegate
  private val recentActions = LinkedHashMap<String, Long>()
  private val scheduledAdRetries = HashSet<String>()
  private var mediaWatch: MediaWatch? = null
  private var observedPlayerPackage: String? = null
  private var observedPlayerVisible = false
  private var lastMediaOcrAt = 0L
  private val mediaWatchTick = object : Runnable {
    override fun run() = pollMediaWatch()
  }
  private var lastActionAt = 0L
  private var lastOcrRequestAt = 0L
  private var ocrScreenshotPending = false
  private var ocrSignatureRetryPending = false
  private var serviceDestroyed = false

  override fun onServiceConnected() {
    super.onServiceConnected()
    store = ClearScreenStore(this)
    serviceDestroyed = false
    running = true
    Log.i(
      TAG,
      "onServiceConnected component=${ComponentName(this, ClearScreenAccessibilityService::class.java).flattenToString()} " +
        "pid=${Process.myPid()}",
    )
    handler.postDelayed({ scanCurrentForegroundPackage() }, INITIAL_FOREGROUND_SCAN_DELAY_MS)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null || !::store.isInitialized || !store.isMasterEnabled()) return
    val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
    if (mediaWatch != null && !isCurrentForegroundPackage(mediaWatch!!.packageName)) {
      stopMediaWatch()
      observedPlayerVisible = false
    }
    if (!isEligibleForegroundPackage(packageName)) return

    val rule = store.getAppRule(packageName)
    if (!rule.skipEnabled || rule.whitelist) return

    val now = System.currentTimeMillis()
    if (now - lastActionAt < ACTION_COOLDOWN_MS) return

    val handled = tryHandlePackage(packageName)
    if (handled) {
      scheduledAdRetries.remove(packageName)
    } else if (isDynamicAdEvent(event.eventType)) {
      scheduleDynamicAdRetries(packageName)
      requestOcrScan(packageName)
      maybeStartMediaWatch(packageName, event.eventType)
    }
  }

  override fun onInterrupt() = Unit

  override fun onUnbind(intent: Intent?): Boolean {
    stopMediaWatch()
    observedPlayerVisible = false
    running = false
    Log.w(TAG, "onUnbind pid=${Process.myPid()}")
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    serviceDestroyed = true
    Log.w(TAG, "onDestroy pid=${Process.myPid()}")
    handler.removeCallbacksAndMessages(null)
    recentActions.clear()
    scheduledAdRetries.clear()
    stopMediaWatch()
    observedPlayerVisible = false
    if (textRecognizerDelegate.isInitialized()) runCatching { textRecognizer.close() }
    // Do not shut down ocrExecutor here: pending ML Kit task listeners are
    // dispatched on it, and rejecting those callbacks can crash the service.
    running = false
    super.onDestroy()
  }

  private fun scanCurrentForegroundPackage() {
    if (serviceDestroyed || !::store.isInitialized || !store.isMasterEnabled()) return
    val packageName = rootInActiveWindow?.packageName?.toString()
      ?.takeIf(::isEligibleForegroundPackage)
      ?: return
    val rule = store.getAppRule(packageName)
    if (!rule.skipEnabled || rule.whitelist) return
    if (!tryHandlePackage(packageName)) {
      requestOcrScan(packageName)
      maybeStartMediaWatch(packageName, AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    }
  }

  private fun tryHandlePackage(packageName: String, ocrFrame: OcrFrame? = null): Boolean {
    if (serviceDestroyed || !::store.isInitialized || !store.isMasterEnabled()) return false
    if (!isEligibleForegroundPackage(packageName)) return false
    val rule = store.getAppRule(packageName)
    if (!rule.skipEnabled || rule.whitelist) return false

    val now = System.currentTimeMillis()
    if (now - lastActionAt < ACTION_COOLDOWN_MS) return false
    val root = findRootForPackage(packageName) ?: return false
    val action = findAdAction(root, ocrFrame) ?: return false
    if (!action.target.isVisibleToUser || !action.target.isEnabled || !action.target.isClickable ||
      action.target.packageName?.toString() != packageName
    ) return false
    val fingerprint = action.targetFingerprint(packageName)
    if (wasRecentlyAttempted(fingerprint, now)) return false

    lastActionAt = now
    // Only click a deliberately identified control. ACTION_DISMISS is exposed by
    // many ordinary dialogs and would close user-required popups even when no ad
    // close button was found.
    val clicked = action.target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    if (clicked) rememberAction(fingerprint, now)

    if (BuildConfig.DEBUG) {
      Log.d(
        TAG,
        "action=${action.ruleId} score=${action.score} package=$packageName " +
          "label=${action.label} clicked=$clicked",
      )
    }

    if (!clicked) {
      appendActionEvent(packageName, action, "failed", "处理失败")
      return false
    }

    // performAction(true) only means that Android accepted the request. Verify
    // the same target shortly afterwards so a stale/non-operational node does
    // not inflate the success counter.
    handler.postDelayed({
      if (!::store.isInitialized || !store.isMasterEnabled()) return@postDelayed
      if (!isEligibleForegroundPackage(packageName)) {
        appendActionEvent(packageName, action, "failed", "前台已切换，关闭结果未确认")
        return@postDelayed
      }
      val currentRoot = findRootForPackage(packageName)
      if (currentRoot == null) {
        appendActionEvent(packageName, action, "failed", "页面不可读取，关闭结果未确认")
        return@postDelayed
      }
      val currentAction = currentRoot?.let { findAdAction(it, ocrFrame) }
      val stillPresent = currentAction?.targetFingerprint(packageName) == fingerprint
      if (stillPresent) {
        appendActionEvent(packageName, action, "failed", "动作未生效")
      } else {
        appendActionEvent(packageName, action, "success", action.successText)
        if (action.category == DetectionCategory.FEED) {
          handler.postDelayed({
            if (!::store.isInitialized || !store.isMasterEnabled() ||
              !isCurrentForegroundPackage(packageName)
            ) return@postDelayed
            if (!tryHandlePackage(packageName)) requestOcrScan(packageName)
          }, FEED_RESCAN_DELAY_MS)
        }
      }
    }, ACTION_VERIFY_DELAY_MS)
    return true
  }

  private fun scheduleDynamicAdRetries(packageName: String) {
    if (!scheduledAdRetries.add(packageName)) return
    handler.postDelayed(
      { runDynamicAdRetry(packageName, 0) },
      DYNAMIC_AD_RETRY_DELAYS_MS.first(),
    )
  }

  private fun isEligibleForegroundPackage(packageName: String): Boolean =
    packageName.isNotBlank() && packageName != applicationContext.packageName &&
      !AdDecisionPolicy.isProtectedPackage(packageName) && isCurrentForegroundPackage(packageName)

  private fun maybeStartMediaWatch(packageName: String, eventType: Int) {
    if (!store.isSettingEnabled(ClearScreenStore.KEY_SKIP_SPLASH_VIDEO, true) ||
      !isEligibleForegroundPackage(packageName) || mediaWatch?.packageName == packageName
    ) return
    val root = findRootForPackage(packageName) ?: return
    val snapshot = AccessibilitySnapshot.capture(root)
    val playerVisible = hasPlayerSurface(snapshot)
    val playerEntry = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
      (playerVisible && (observedPlayerPackage != packageName || !observedPlayerVisible))
    observedPlayerPackage = packageName
    observedPlayerVisible = playerVisible
    if (!AdDecisionPolicy.shouldMonitorMedia(
        hasVideoAdCue = hasVideoAdCue(snapshot),
        hasPlayerSurface = playerVisible,
        playerEntry = playerEntry,
      )
    ) return
    val now = System.currentTimeMillis()
    mediaWatch = MediaWatch(packageName, now + MEDIA_WATCH_MAX_MS)
    lastMediaOcrAt = now
    handler.removeCallbacks(mediaWatchTick)
    handler.postDelayed(mediaWatchTick, MEDIA_WATCH_INTERVAL_MS)
  }

  private fun pollMediaWatch() {
    val watch = mediaWatch ?: return
    val now = System.currentTimeMillis()
    if (serviceDestroyed || !::store.isInitialized || !store.isMasterEnabled() ||
      !store.isSettingEnabled(ClearScreenStore.KEY_SKIP_SPLASH_VIDEO, true) ||
      !isEligibleForegroundPackage(watch.packageName) ||
      !store.getAppRule(watch.packageName).let { it.skipEnabled && !it.whitelist } ||
      now >= watch.deadlineAt
    ) {
      stopMediaWatch()
      return
    }
    val root = findRootForPackage(watch.packageName) ?: run { stopMediaWatch(); return }
    val snapshot = AccessibilitySnapshot.capture(root)
    val playerVisible = hasPlayerSurface(snapshot)
    observedPlayerVisible = playerVisible
    if (!hasVideoAdCue(snapshot) && !playerVisible) {
      stopMediaWatch()
      return
    }
    val handled = tryHandlePackage(watch.packageName)
    if (!handled && now - lastMediaOcrAt >= MEDIA_WATCH_OCR_INTERVAL_MS) {
      lastMediaOcrAt = now
      requestOcrScan(watch.packageName)
    }
    if (mediaWatch === watch) handler.postDelayed(mediaWatchTick, MEDIA_WATCH_INTERVAL_MS)
  }

  private fun stopMediaWatch() {
    mediaWatch = null
    handler.removeCallbacks(mediaWatchTick)
  }

  private fun hasVideoAdCue(snapshot: AccessibilitySnapshot): Boolean =
    snapshot.nodes.any { node ->
      node.isVisible && (
        hasVideoAdSignal(node.label) || isCountdownSkipLabel(node.compactLabel) ||
          VIDEO_AD_RESOURCE_MARKERS.any(node.resourceId::contains)
        )
    }

  private fun hasPlayerSurface(snapshot: AccessibilitySnapshot): Boolean =
    snapshot.nodes.any { node ->
      node.isVisible && node.ancestorRegions.none { region ->
        SCROLL_CONTAINER_CLASSES.any(region.className::contains)
      } && (node.className.contains("surfaceview") || node.className.contains("textureview") ||
        PLAYER_RESOURCE_MARKERS.any(node.resourceId::contains))
    }

  private fun requestOcrScan(packageName: String, retryOnSignatureMismatch: Boolean = true) {
    if (serviceDestroyed || Build.VERSION.SDK_INT < Build.VERSION_CODES.R || ocrScreenshotPending) return
    if (!isEligibleForegroundPackage(packageName)) return
    if (ocrSignatureRetryPending && retryOnSignatureMismatch) return
    if (!store.isSettingEnabled(ClearScreenStore.KEY_SKIP_SPLASH_VIDEO, true) &&
      !store.isSettingEnabled(ClearScreenStore.KEY_CLOSE_POPUP_ADS, true) &&
      !store.isSettingEnabled(ClearScreenStore.KEY_CLOSE_FEED_ADS, true)
    ) return

    val now = System.currentTimeMillis()
    if (now - lastOcrRequestAt < OCR_MIN_INTERVAL_MS) return
    val root = findRootForPackage(packageName) ?: return
    val beforeCapture = AccessibilitySnapshot.capture(root)
    val rootBounds = beforeCapture.rootBounds
    if (!hasUsableBounds(rootBounds)) return
    if (beforeCapture.nodes.any { it.isVisible && (it.isPassword || (it.isEditable && it.isFocused)) }) return

    lastOcrRequestAt = now
    ocrScreenshotPending = true
    try {
      takeScreenshot(
        Display.DEFAULT_DISPLAY,
        ocrExecutor,
        object : TakeScreenshotCallback {
          override fun onSuccess(screenshot: ScreenshotResult) {
            val hardwareBuffer = screenshot.hardwareBuffer
            val hardwareBitmap = runCatching {
              Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshot.colorSpace)
            }.getOrNull()
            hardwareBuffer.close()
            if (serviceDestroyed) {
              hardwareBitmap?.let { runCatching { it.recycle() } }
              return
            }
            val bitmap = hardwareBitmap?.let { wrapped ->
              val copy = runCatching { wrapped.copy(Bitmap.Config.ARGB_8888, false) }.getOrNull()
              runCatching { wrapped.recycle() }
              copy
            }
            if (bitmap == null) {
              handler.post { ocrScreenshotPending = false }
              return
            }

            val recognitionTask = try {
              textRecognizer.process(InputImage.fromBitmap(bitmap, 0))
            } catch (error: Exception) {
              runCatching { bitmap.recycle() }
              handler.post { ocrScreenshotPending = false }
              if (BuildConfig.DEBUG) Log.d(TAG, "on-device OCR unavailable code=${error.javaClass.simpleName}")
              return
            }
            recognitionTask
              .addOnSuccessListener(ocrExecutor) { recognized ->
                val lines = recognized.textBlocks.flatMap { block ->
                  block.lines.mapNotNull { line ->
                    val bounds = line.boundingBox ?: return@mapNotNull null
                    OcrLine(
                      text = line.text,
                      compactText = normalizeLabel(line.text),
                      screenBounds = mapOcrBounds(bounds, bitmap.width, bitmap.height, rootBounds),
                    )
                  }
                }
                val elements = recognized.textBlocks.asSequence()
                  .flatMap { block -> block.lines.asSequence() }
                  .flatMap { line -> line.elements.asSequence() }
                  .mapNotNull { element ->
                    val bounds = element.boundingBox ?: return@mapNotNull null
                    OcrLine(
                      text = element.text,
                      compactText = normalizeLabel(element.text),
                      screenBounds = mapOcrBounds(bounds, bitmap.width, bitmap.height, rootBounds),
                    )
                  }
                  .take(MAX_OCR_TEXT_ELEMENTS)
                  .toList()
                handler.post {
                  ocrScreenshotPending = false
                  if (serviceDestroyed) return@post
                  if (isCurrentForegroundPackage(packageName) && lines.isNotEmpty()) {
                    val currentSignature = findRootForPackage(packageName)?.let { currentRoot ->
                      AccessibilitySnapshot.capture(currentRoot).signature
                    }
                    if (currentSignature != beforeCapture.signature) {
                      // Never pair OCR text with a different accessibility tree. One
                      // fresh capture can recover a changing page without relaxing
                      // the screen-identity check or reusing stale evidence.
                      if (retryOnSignatureMismatch && !ocrSignatureRetryPending) {
                        ocrSignatureRetryPending = true
                        handler.postDelayed({
                          ocrSignatureRetryPending = false
                          if (isCurrentForegroundPackage(packageName)) {
                            requestOcrScan(packageName, retryOnSignatureMismatch = false)
                          }
                        }, OCR_SIGNATURE_RETRY_DELAY_MS)
                      }
                      return@post
                    }
                    val frame = OcrFrame(
                      packageName = packageName,
                      capturedAt = now,
                      accessibilitySignature = beforeCapture.signature,
                      lines = lines,
                      elements = elements,
                    )
                    tryHandlePackage(packageName, frame)
                  }
                }
              }
              .addOnFailureListener(ocrExecutor) { error ->
                if (BuildConfig.DEBUG) Log.d(TAG, "on-device OCR failed code=${error.javaClass.simpleName}")
                handler.post { ocrScreenshotPending = false }
              }
              .addOnCompleteListener(ocrExecutor) { runCatching { bitmap.recycle() } }
          }

          override fun onFailure(errorCode: Int) {
            if (BuildConfig.DEBUG) Log.d(TAG, "screenshot capture unavailable code=$errorCode")
            handler.post { ocrScreenshotPending = false }
          }
        },
      )
    } catch (error: Exception) {
      ocrScreenshotPending = false
      if (BuildConfig.DEBUG) Log.d(TAG, "screenshot capture unavailable code=${error.javaClass.simpleName}")
    }
  }

  private fun isCurrentForegroundPackage(packageName: String): Boolean =
    rootInActiveWindow?.packageName?.toString() == packageName

  private fun mapOcrBounds(bounds: Rect, imageWidth: Int, imageHeight: Int, rootBounds: Rect): Rect {
    val width = imageWidth.coerceAtLeast(1)
    val height = imageHeight.coerceAtLeast(1)
    val rootWidth = rootBounds.width().coerceAtLeast(1)
    val rootHeight = rootBounds.height().coerceAtLeast(1)
    return Rect(
      rootBounds.left + bounds.left * rootWidth / width,
      rootBounds.top + bounds.top * rootHeight / height,
      rootBounds.left + bounds.right * rootWidth / width,
      rootBounds.top + bounds.bottom * rootHeight / height,
    )
  }

  private fun runDynamicAdRetry(packageName: String, attempt: Int) {
    if (!scheduledAdRetries.contains(packageName)) return
    if (tryHandlePackage(packageName) || attempt >= DYNAMIC_AD_RETRY_DELAYS_MS.lastIndex) {
      scheduledAdRetries.remove(packageName)
      return
    }

    val nextAttempt = attempt + 1
    val delay = DYNAMIC_AD_RETRY_DELAYS_MS[nextAttempt] - DYNAMIC_AD_RETRY_DELAYS_MS[attempt]
    handler.postDelayed(
      { runDynamicAdRetry(packageName, nextAttempt) },
      delay,
    )
  }

  private fun findAdAction(
    root: AccessibilityNodeInfo,
    ocrFrame: OcrFrame? = null,
  ): DetectionAction? {
    val snapshot = AccessibilitySnapshot.capture(root)
    if (snapshot.nodes.isEmpty()) return null
    if (AdDecisionPolicy.isProtectedConfirmation(
        snapshot.nodes.asSequence().filter { it.isVisible }.map { it.compactLabel }.toList()
      )
    ) return null
    val currentOcrFrame = ocrFrame?.takeIf { frame ->
      frame.packageName == snapshot.packageName &&
        frame.accessibilitySignature == snapshot.signature &&
        System.currentTimeMillis() - frame.capturedAt <= OCR_FRAME_MAX_AGE_MS
    }

    val candidates = snapshot.nodes.mapNotNull { node ->
      findSkipCandidate(node, snapshot, currentOcrFrame)
    } + snapshot.nodes.mapNotNull { node ->
      findPopupCloseCandidate(node, snapshot, currentOcrFrame)
    } + snapshot.nodes.mapNotNull { node ->
      findEmbeddedAdCloseCandidate(node, snapshot, currentOcrFrame)
    } + snapshot.nodes.mapNotNull { node ->
      findFeedCloseCandidate(node, snapshot, currentOcrFrame)
    }

    return candidates
      .filter { it.score >= it.minimumScore }
      .maxWithOrNull(
        compareBy<DetectionAction> { it.score }
          .thenByDescending { it.actionPriority }
          .thenBy { it.targetBounds.top },
      )
  }

  private fun findSkipCandidate(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
    ocrFrame: OcrFrame?,
  ): DetectionAction? {
    if (!store.isSettingEnabled(ClearScreenStore.KEY_SKIP_SPLASH_VIDEO, true)) return null
    if (!node.isVisible || !node.isEnabled) return null
    val compact = node.compactLabel
    val resourceId = node.resourceId
    val ocrAtNode = ocrFrame?.skipText?.firstOrNull { line ->
      node.bounds.contains(line.screenBounds.centerX(), line.screenBounds.centerY())
    }
    if (!isStrongSkipLabel(compact) && !isCountdownSkipLabel(compact) &&
      compact != "跳过" && compact != "skip" && !hasSkipResourceId(resourceId) &&
      ocrAtNode == null
    ) return null
    val target = clickableTarget(node.node) ?: return null
    if (!target.isVisibleToUser || !target.isEnabled) return null

    val targetBounds = Rect().also(target::getBoundsInScreen)
    val ocrSkipLine = ocrAtNode?.takeIf { line ->
      targetBounds.contains(line.screenBounds.centerX(), line.screenBounds.centerY())
    }
    val candidateText = ocrSkipLine?.compactText ?: compact
    val strongLabel = isStrongSkipLabel(candidateText)
    val countdownLabel = isCountdownSkipLabel(candidateText)
    val plainLabel = candidateText == "跳过" || candidateText == "skip"
    val explicitResource = hasSkipResourceId(resourceId)
    if (!strongLabel && !countdownLabel && !plainLabel && !explicitResource) return null

    val labelBounds = ocrSkipLine?.screenBounds ?: node.bounds
    if (!hasUsableBounds(labelBounds)) return null
    if (!hasUsableBounds(targetBounds)) return null
    if (!hasCompactSkipTarget(targetBounds, labelBounds, snapshot.rootBounds)) return null
    val labelNearTopRight = isNearTopRight(labelBounds, snapshot.rootBounds)
    val targetNearTopRight = isNearTopRight(targetBounds, snapshot.rootBounds)
    val nearTopRight = labelNearTopRight || targetNearTopRight
    val veryNearTopRight = isVeryNearTopRight(labelBounds, snapshot.rootBounds) ||
      isVeryNearTopRight(targetBounds, snapshot.rootBounds)
    val labelCompactControl = isCompactControl(labelBounds, snapshot.rootBounds)
    val targetCompactControl = isCompactControl(targetBounds, snapshot.rootBounds)
    val compactControl = labelCompactControl || targetCompactControl
    val ocrSplashAdContext = hasOcrBackedSplashAdContext(
      candidateText = candidateText,
      targetBounds = targetBounds,
      labelBounds = labelBounds,
      snapshot = snapshot,
      frame = ocrFrame,
    )
    val topCenterInterstitialContext = plainLabel && hasTopCenterThirdPartyInterstitialContext(
      targetBounds = targetBounds,
      labelBounds = labelBounds,
      snapshot = snapshot,
      frame = ocrFrame,
    )
    val ancestorHasAdSignal = node.ancestorRegions.any { region ->
      isLocalAdRegion(region, snapshot.rootBounds) &&
        hasStrongAdSignal(region.label, region.resourceId)
    }
    val localStrongAdContext = hasStrongAdSignal(node.label, resourceId) || ancestorHasAdSignal
    val videoAdContext = node.ancestorRegions.any { region ->
      isLocalAdRegion(region, snapshot.rootBounds) &&
        (hasVideoAdSignal(region.label) ||
          VIDEO_AD_RESOURCE_MARKERS.any(region.resourceId::contains))
    } || hasVideoAdSignal(node.label) || hasLocalVideoAdEvidence(node, snapshot, ocrFrame)
    val strongSkipResource = resourceId.contains("ad_skip") || resourceId.contains("skip_ad") ||
      resourceId.contains("skipad") || (hasAdResourceId(resourceId) && resourceId.contains("skip"))
    val edgeSkip = plainLabel && veryNearTopRight && compactControl && strongSkipResource
    // A resource id is the weakest signal on its own: ordinary navigation
    // containers are commonly named "xxx_jump", "skip_*" or "countdown_*".
    // Treat the id as a supporting marker that still needs geometry or ad context.
    val resourceOnly = explicitResource && !strongLabel && !countdownLabel && !plainLabel

    var score = when {
      countdownLabel -> 116
      strongLabel -> 112
      explicitResource -> 96
      else -> 82
    }
    var ruleId = when {
      countdownLabel -> "builtin.skip.countdown"
      strongLabel -> "builtin.skip.strong-label"
      explicitResource -> "builtin.skip.resource-id"
      else -> "builtin.skip.short-label"
    }

    if (explicitResource) score += 28
    if (nearTopRight) score += 20
    if (compactControl) score += 8
    if (topCenterInterstitialContext) score += 32
    if (ancestorHasAdSignal) score += 18
    if (localStrongAdContext) score += 24
    if (videoAdContext) score += 18
    if (ocrSplashAdContext) score += 30

    if (plainLabel && !edgeSkip && !videoAdContext &&
      !ancestorHasAdSignal && !localStrongAdContext && !topCenterInterstitialContext &&
      !ocrSplashAdContext
    ) return null
    if (strongLabel && !candidateText.contains("广告") && !candidateText.contains("ad") &&
      !videoAdContext && !localStrongAdContext && !ancestorHasAdSignal && !ocrSplashAdContext
    ) return null
    if (countdownLabel && !ocrSplashAdContext && !AdDecisionPolicy.allowCountdownSkip(
        hasAdWord = candidateText.contains("广告") || candidateText.contains("ad"),
        nearTopRight = veryNearTopRight && compactControl,
        hasSkipResource = strongSkipResource,
        localAdContext = localStrongAdContext || ancestorHasAdSignal,
        videoAdContext = videoAdContext,
        verifiedTopCenterAd = topCenterInterstitialContext,
      )
    ) return null
    // A plain jump/skip/countdown id is common in ordinary navigation. Only an
    // ad-specific id or local ad evidence may authorize an otherwise unlabelled node.
    if (resourceOnly &&
      !strongSkipResource && !ancestorHasAdSignal && !localStrongAdContext && !videoAdContext &&
      !ocrSplashAdContext
    ) return null

    if (localStrongAdContext || videoAdContext) {
      ruleId += ".ad-context"
    }
    if (edgeSkip && videoAdContext) {
      ruleId = "builtin.skip.video-ad.edge"
    }
    if (topCenterInterstitialContext) {
      ruleId = "builtin.skip.splash.top-center-third-party"
    }
    if (ocrSplashAdContext && !topCenterInterstitialContext) {
      ruleId = "builtin.skip.splash.ocr-creative"
    }
    return DetectionAction(
      target = target,
      targetBounds = targetBounds,
      score = score,
      minimumScore = if (plainLabel && !topCenterInterstitialContext) 102 else 96,
      actionPriority = 2,
      ruleId = ruleId,
      label = (ocrSkipLine?.text ?: node.label).take(MAX_LOG_LABEL_LENGTH),
      successText = "已跳过开屏广告",
      category = DetectionCategory.SPLASH_VIDEO,
    )
  }

  /**
   * A narrow OCR-backed exception for ad SDKs that place a plain "跳过" button
   * at the top-center instead of the usual top-right. Position alone is never
   * enough: the same fresh screen must also expose the explicit third-party
   * redirect banner and the distinctive browser/video/reward copy seen in this
   * interstitial layout.
   */
  /**
   * Generic OCR fallback for splash pages whose UI tree exposes only a plain
   * skip/countdown control. Require a compact top-right control, a large visual
   * creative, and either an explicit ad marker or a cluster of offer text plus
   * an action. This intentionally does not match ordinary page-level banners.
   */
  private fun hasOcrBackedSplashAdContext(
    candidateText: String,
    targetBounds: Rect,
    labelBounds: Rect,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): Boolean {
    val currentFrame = frame ?: return false
    if (currentFrame.packageName != snapshot.packageName ||
      currentFrame.accessibilitySignature != snapshot.signature ||
      System.currentTimeMillis() - currentFrame.capturedAt > OCR_FRAME_MAX_AGE_MS ||
      !isSkipOcrLabel(normalizeLabel(candidateText)) ||
      !isVeryNearTopRight(targetBounds, snapshot.rootBounds) ||
      !isCompactControl(targetBounds, snapshot.rootBounds) ||
      !hasCompactSkipTarget(targetBounds, labelBounds, snapshot.rootBounds)
    ) return false

    if (!hasLargeSplashCreative(snapshot)) return false
    val explicitAdMarker = currentFrame.lines.any { hasStrongAdSignal(it.text) } ||
      hasStrongAdSignal(joinOcrLineText(currentFrame.lines))
    val offerSignals = countDistinctOcrMarkers(currentFrame.lines, SPLASH_OFFER_MARKERS)
    val hasPromotionalAction = hasOcrPromotionalAction(currentFrame.lines, includeSplashActions = true)
    return AdDecisionPolicy.allowOcrBackedSplashSkip(
      hasSkipLabel = true,
      compactTopRight = true,
      hasLargeCreative = true,
      hasExplicitAdMarker = explicitAdMarker,
      promotionalSignalCount = offerSignals,
      hasPromotionalAction = hasPromotionalAction,
    )
  }

  private fun hasLargeSplashCreative(snapshot: AccessibilitySnapshot): Boolean {
    val root = snapshot.rootBounds
    val rootArea = root.width().toLong().coerceAtLeast(1L) *
      root.height().toLong().coerceAtLeast(1L)
    return snapshot.nodes.any { node ->
      if (!node.isVisible || node.depth == 0) return@any false
      val visualClass = node.className.contains("imageview") ||
        node.className.contains("surfaceview") ||
        node.className.contains("textureview") ||
        node.className.contains("webview")
      if (!visualClass) return@any false
      val area = node.bounds.width().toLong().coerceAtLeast(0L) *
        node.bounds.height().toLong().coerceAtLeast(0L)
      node.bounds.width() >= root.width() * MIN_SPLASH_CREATIVE_WIDTH_RATIO &&
        node.bounds.height() >= root.height() * MIN_SPLASH_CREATIVE_HEIGHT_RATIO &&
        area.toDouble() / rootArea >= MIN_SPLASH_CREATIVE_AREA_RATIO
    }
  }

  private fun hasTopCenterThirdPartyInterstitialContext(
    targetBounds: Rect,
    labelBounds: Rect,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): Boolean {
    if (frame == null || frame.packageName != snapshot.packageName ||
      frame.accessibilitySignature != snapshot.signature ||
      System.currentTimeMillis() - frame.capturedAt > OCR_FRAME_MAX_AGE_MS ||
      !isCompactTopCenterSkipTarget(targetBounds, labelBounds, snapshot.rootBounds)
    ) return false

    val root = snapshot.rootBounds
    val width = root.width().coerceAtLeast(1)
    val height = root.height().coerceAtLeast(1)
    val hasThirdPartyRedirectBanner = frame.lines.any { line ->
      line.screenBounds.centerY() <= root.top + height * TOP_CENTER_SKIP_CONTEXT_TOP_RATIO &&
        TOP_CENTER_THIRD_PARTY_HINTS.any(line.compactText::contains)
    }
    if (!hasThirdPartyRedirectBanner) return false

    val posterLines = frame.lines.filter { line ->
      val centerX = line.screenBounds.centerX()
      val centerY = line.screenBounds.centerY()
      centerX in (root.left + width * 0.08f).toInt()..(root.left + width * 0.92f).toInt() &&
        centerY in (root.top + height * TOP_CENTER_SKIP_CONTEXT_BODY_MIN_RATIO).toInt()..
        (root.top + height * TOP_CENTER_SKIP_CONTEXT_BODY_MAX_RATIO).toInt()
    }
    // Poster copy is often split into multiple OCR lines on tall splash ads;
    // combine only lines inside the same bounded central poster area.
    val posterText = posterLines
      .sortedWith(compareBy<OcrLine> { it.screenBounds.centerY() }.thenBy { it.screenBounds.left })
      .joinToString(separator = "") { it.compactText }
    val browserVideoPitch = posterText.contains("浏览器") &&
      (posterText.contains("看剧") || posterText.contains("追剧"))
    val rewardOffer = TOP_CENTER_INTERSTITIAL_REWARD_MARKERS.any(posterText::contains)
    val promotionalAction = TOP_CENTER_INTERSTITIAL_ACTION_MARKERS.any(posterText::contains)
    return browserVideoPitch && rewardOffer && promotionalAction
  }

  private fun hasLocalVideoAdEvidence(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): Boolean {
    val rootArea = snapshot.rootBounds.width().toLong().coerceAtLeast(1L) *
      snapshot.rootBounds.height().toLong().coerceAtLeast(1L)
    return node.ancestorRegions.asSequence()
      .filter { region ->
        region.path != "0" && hasUsableBounds(region.bounds) &&
          region.bounds.width().toLong() * region.bounds.height().toLong() <= rootArea * 0.85 &&
          SCROLL_CONTAINER_CLASSES.none(region.className::contains)
      }
      .any { region ->
        snapshot.nodes.any { candidate ->
          candidate.isVisible && candidate.path != node.path &&
            AdDecisionPolicy.isDescendantOf(candidate.path, region.path) &&
            (hasVideoAdSignal(candidate.label) ||
              VIDEO_AD_RESOURCE_MARKERS.any(candidate.resourceId::contains))
        } || frame?.lines?.any { line ->
          region.bounds.contains(line.screenBounds.centerX(), line.screenBounds.centerY()) &&
            hasVideoAdSignal(line.text)
        } == true
      }
  }

  private fun findPopupCloseCandidate(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
    ocrFrame: OcrFrame?,
  ): DetectionAction? {
    if (!store.isSettingEnabled(ClearScreenStore.KEY_CLOSE_POPUP_ADS, true)) return null
    if (!node.isVisible || !node.isEnabled) return null
    val target = clickableTarget(node.node) ?: return null
    if (!target.isVisibleToUser || !target.isEnabled) return null

    val compact = node.compactLabel
    val resourceId = node.resourceId
    val closeLabel = isPopupCloseLabel(compact)
    val targetResourceId = target.viewIdResourceName.orEmpty().lowercase(Locale.ROOT)
    val explicitAdResource = hasAdCloseResourceId(resourceId) ||
      hasAdCloseResourceId(targetResourceId)

    val targetBounds = Rect().also(target::getBoundsInScreen)
    if (!hasUsableBounds(targetBounds)) return null
    val centeredPosterEvidence = findKnownCenteredPosterAdEvidence(
      node = node,
      target = target,
      targetBounds = targetBounds,
      targetResourceId = targetResourceId,
      snapshot = snapshot,
      frame = ocrFrame,
    )
    val caiyunInterstitialEvidence = findKnownCaiyunInterstitialAdEvidence(
      node = node,
      target = target,
      targetBounds = targetBounds,
      targetResourceId = targetResourceId,
      snapshot = snapshot,
    )
    val nearTopRight = isNearTopRight(targetBounds, snapshot.rootBounds)
    val nearPopupEdge = isNearPopupEdge(targetBounds, snapshot.rootBounds)
    val compactControl = isIndependentCloseTarget(targetBounds, snapshot.rootBounds)
    val localStrongAdContext = explicitAdResource ||
      hasStrongAdSignal(node.label, resourceId) ||
      node.ancestorRegions.any { region ->
        isLocalAdRegion(region, snapshot.rootBounds) &&
          hasStrongAdSignal(region.label, region.resourceId)
      }
    val fullScreenAdContext = hasFullScreenAdEvidenceWithinOverlay(node, snapshot)
    val ocrOverlayContext = hasOcrOverlayEvidence(node, targetBounds, snapshot, ocrFrame)
    val belongsToAdOverlay = node.ancestorRegions.any { region ->
      region.path != "0" && isLocalAdRegion(region, snapshot.rootBounds) && isOverlayLayer(region)
    }
    val unlabeledEdgeClose = isLikelyUnlabeledEdgeClose(
      node = node,
      target = target,
      targetResourceId = targetResourceId,
      nearPopupEdge = nearPopupEdge,
      compactControl = compactControl,
      ocrCloseGlyph = ocrFrame?.closeGlyphText?.any {
        isCompactOcrCloseGlyph(it, targetBounds, snapshot.rootBounds)
      } == true,
    )
    if (!closeLabel && !explicitAdResource && !unlabeledEdgeClose &&
      centeredPosterEvidence == null && caiyunInterstitialEvidence == null
    ) return null
    val genericCloseLabel = isGenericPopupCloseLabel(compact)

    // Do not use the whole screen's commercial/benefit text as evidence. A
    // normal dialog can contain words such as "会员" or "福利" elsewhere.
    // Require an ad marker on this control, its ancestors, or a separately
    // verified full-screen ad context. The latter is deliberately stricter:
    // it needs a large overlay/panel plus a cluster of ad/commercial labels.
    if (!localStrongAdContext && !fullScreenAdContext && !ocrOverlayContext &&
      centeredPosterEvidence == null && caiyunInterstitialEvidence == null
    ) return null
    if (!belongsToAdOverlay && !fullScreenAdContext && !ocrOverlayContext &&
      !explicitAdResource && centeredPosterEvidence == null && caiyunInterstitialEvidence == null
    ) return null
    if (!nearPopupEdge || !compactControl) return null

    // Generic "关闭"/X controls are only safe in the conventional ad position.
    // Ad-specific labels such as "关闭广告" may also appear on the popup edge.
    if (genericCloseLabel && !nearTopRight && !explicitAdResource && !ocrOverlayContext) return null
    if (unlabeledEdgeClose && !nearPopupEdge && centeredPosterEvidence == null &&
      caiyunInterstitialEvidence == null
    ) return null

    var score = when {
      explicitAdResource -> 124
      caiyunInterstitialEvidence != null -> 122
      centeredPosterEvidence != null -> 118
      isAdSpecificCloseLabel(compact) -> 114
      ocrOverlayContext -> 110
      fullScreenAdContext -> 108
      else -> 100
    }
    if (explicitAdResource) score += 30
    if (nearTopRight) score += 18
    if (nearPopupEdge) score += 18
    if (compactControl) score += 8
    if (localStrongAdContext) score += 26
    if (fullScreenAdContext) score += 24

    return DetectionAction(
      target = target,
      targetBounds = targetBounds,
      score = score,
      minimumScore = if (explicitAdResource || centeredPosterEvidence != null ||
        caiyunInterstitialEvidence != null
      ) 120 else 112,
      actionPriority = 1,
      ruleId = when {
        explicitAdResource -> "builtin.popup.close.resource-id"
        caiyunInterstitialEvidence != null -> "builtin.popup.close.caiyun-interstitial.bottom-center"
        centeredPosterEvidence != null -> "builtin.popup.close.centered-poster"
        ocrOverlayContext -> "builtin.popup.close.ocr-overlay"
        fullScreenAdContext -> "builtin.popup.close.fullscreen-ad"
        else -> "builtin.popup.close.context"
      },
      label = node.label.ifBlank { node.className }.take(MAX_LOG_LABEL_LENGTH),
      successText = "已关闭弹窗广告",
      category = DetectionCategory.POPUP,
      evidenceFingerprint = caiyunInterstitialEvidence ?: centeredPosterEvidence ?: ocrFrame?.lines
        ?.filter { isAdRelatedOcrText(it.text) }
        ?.joinToString("|") { it.compactText }?.take(240).orEmpty(),
    )
  }

  private fun findFeedCloseCandidate(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
    ocrFrame: OcrFrame?,
  ): DetectionAction? {
    if (!store.isSettingEnabled(ClearScreenStore.KEY_CLOSE_FEED_ADS, true)) return null
    if (ocrFrame == null || !node.isEnabled || !node.isVisible) return null
    val target = clickableTarget(node.node) ?: return null
    if (!target.isVisibleToUser || !target.isEnabled) return null

    val targetBounds = Rect().also(target::getBoundsInScreen)
    if (!hasUsableBounds(targetBounds)) return null
    val compact = node.compactLabel
    val ocrCloseLine = ocrFrame?.closeGlyphText?.firstOrNull { line ->
      isCompactOcrCloseGlyph(line, targetBounds, snapshot.rootBounds)
    }
    val adSpecificClose = isAdSpecificCloseLabel(compact) ||
      hasAdCloseResourceId(node.resourceId) ||
      hasAdCloseResourceId(target.viewIdResourceName.orEmpty().lowercase(Locale.ROOT))
    val genericClose = isGenericPopupCloseLabel(compact) ||
      isLikelyUnlabeledCloseIcon(node, target) ||
      (node.label.isBlank() && ocrCloseLine != null)
    if (!adSpecificClose && !genericClose) return null

    val card = node.ancestorRegions.asSequence()
      .filter { region -> isPotentialFeedCard(region, node.bounds, targetBounds, snapshot.rootBounds) }
      .mapNotNull { region ->
        val evidence = findFeedAdEvidence(region, snapshot, ocrFrame) ?: return@mapNotNull null
        if (ocrCloseLine != null && (!evidence.first ||
            !region.bounds.contains(ocrCloseLine.screenBounds.centerX(), ocrCloseLine.screenBounds.centerY())
          )
        ) return@mapNotNull null
        if (!isCloseAtCardEdge(
            node = node,
            targetBounds = targetBounds,
            cardBounds = region.bounds,
            adSpecificClose = adSpecificClose,
            closeVisualBounds = ocrCloseLine?.screenBounds,
          )
        ) return@mapNotNull null
        Triple(region, evidence.first, evidence.second)
      }
      .minByOrNull { it.first.bounds.width().toLong() * it.first.bounds.height().toLong() }
      ?: return null

    val region = card.first
    val targetArea = targetBounds.width().toLong() * targetBounds.height().toLong()
    val regionArea = region.bounds.width().toLong().coerceAtLeast(1L) *
      region.bounds.height().toLong().coerceAtLeast(1L)
    if (targetArea > regionArea * MAX_FEED_CLOSE_TARGET_AREA_RATIO) return null
    if (targetBounds.width() > region.bounds.width() * MAX_FEED_CLOSE_TARGET_WIDTH_RATIO ||
      targetBounds.height() > region.bounds.height() * MAX_FEED_CLOSE_TARGET_HEIGHT_RATIO
    ) return null

    return DetectionAction(
      target = target,
      targetBounds = targetBounds,
      score = if (card.second) 152 else 132,
      minimumScore = 120,
      actionPriority = 1,
      ruleId = "builtin.feed.close.ocr-card",
      label = node.label.ifBlank { node.className }.take(MAX_LOG_LABEL_LENGTH),
      successText = "已关闭信息流广告",
      category = DetectionCategory.FEED,
      evidenceFingerprint = "${region.resourceId}|${card.third}".take(300),
    )
  }

  /** A small, independently clickable X on an explicitly marked ad card.
   * Covers player pause overlays and inline banners when the app exposes both
   * the ad badge and close control in the same accessibility subtree.
   */
  private fun findEmbeddedAdCloseCandidate(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
    ocrFrame: OcrFrame?,
  ): DetectionAction? {
    if (!store.isSettingEnabled(ClearScreenStore.KEY_CLOSE_POPUP_ADS, true) ||
      !node.isVisible || !node.isEnabled || !node.node.isClickable
    ) return null
    val target = clickableTarget(node.node) ?: return null
    if (target != node.node) return null
    val targetBounds = node.bounds
    if (!hasUsableBounds(targetBounds)) return null
    val closeId = node.resourceId
    val explicitAdClose = isAdSpecificCloseLabel(node.compactLabel) ||
      hasAdCloseResourceId(closeId)
    val closeSemantic = explicitAdClose ||
      isGenericPopupCloseLabel(node.compactLabel) ||
      hasGenericCloseResourceId(closeId) ||
      ocrFrame?.closeGlyphText?.any {
        isCompactOcrCloseGlyph(it, targetBounds, snapshot.rootBounds)
      } == true
    if (!closeSemantic) return null

    val rootArea = snapshot.rootBounds.width().toLong().coerceAtLeast(1L) *
      snapshot.rootBounds.height().toLong().coerceAtLeast(1L)
    val closeArea = targetBounds.width().toLong() * targetBounds.height().toLong()
    val regionWithBadge = node.ancestorRegions.asSequence()
      .filter { region ->
        region.path != "0" && hasUsableBounds(region.bounds) &&
          region.bounds.contains(targetBounds.centerX(), targetBounds.centerY()) &&
          SCROLL_CONTAINER_CLASSES.none(region.className::contains)
      }
      .mapNotNull { region ->
        val regionArea = region.bounds.width().toLong() * region.bounds.height().toLong()
        val badge = snapshot.nodes.firstOrNull { candidate ->
          candidate.isVisible && candidate.path != node.path &&
            candidate.ancestorRegions.any { it.path == region.path } &&
            isExplicitAdBadge(candidate.compactLabel) &&
            candidate.bounds.width().toLong() * candidate.bounds.height().toLong() <= regionArea / 10
        } ?: return@mapNotNull null
        val cardWidth = region.bounds.width().coerceAtLeast(1)
        val cardHeight = region.bounds.height().coerceAtLeast(1)
        val x = (targetBounds.centerX() - region.bounds.left).toDouble() / cardWidth
        val y = (targetBounds.centerY() - region.bounds.top).toDouble() / cardHeight
        val allowed = AdDecisionPolicy.allowEmbeddedAdClose(
          directClickable = true,
          sameSubtree = AdDecisionPolicy.isDescendantOf(badge.path, region.path),
          explicitAdBadge = true,
          closeSemantic = closeSemantic,
          atVerifiedCardEdge = AdDecisionPolicy.isVerifiedCardEdge(x, y, explicitAdClose),
          cardAreaRatio = regionArea.toDouble() / rootArea,
          closeAreaRatio = closeArea.toDouble() / regionArea.coerceAtLeast(1L),
          closeWidthRatio = targetBounds.width().toDouble() / cardWidth,
          closeHeightRatio = targetBounds.height().toDouble() / cardHeight,
        )
        if (allowed) Pair(region, badge) else null
      }
      .minByOrNull { (region, _) -> region.bounds.width().toLong() * region.bounds.height().toLong() }
      ?: return null

    return DetectionAction(
      target = target,
      targetBounds = targetBounds,
      score = 168,
      minimumScore = 160,
      actionPriority = 1,
      ruleId = "builtin.popup.close.embedded-ad-card",
      label = node.label.ifBlank { node.resourceId }.take(MAX_LOG_LABEL_LENGTH),
      successText = "已关闭广告卡片",
      category = DetectionCategory.POPUP,
      evidenceFingerprint = "${regionWithBadge.first.path}|${regionWithBadge.second.compactLabel}",
    )
  }

  private fun isExplicitAdBadge(label: String): Boolean = label in EXPLICIT_AD_BADGE_LABELS

  private fun hasOcrOverlayEvidence(
    node: ScreenNode,
    targetBounds: Rect,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): Boolean {
    if (frame == null || frame.packageName != node.node.packageName?.toString() ||
      System.currentTimeMillis() - frame.capturedAt > OCR_FRAME_MAX_AGE_MS
    ) return false
    if (!isNearOcrPopupScreenEdge(targetBounds, snapshot.rootBounds)) return false

    // OCR may authorize a generic X only from the same overlay subtree. A
    // separate feed banner elsewhere on the page must never authorize this X.
    val overlayRegion = node.ancestorRegions.asSequence()
      .filter { region ->
        (isOverlayLayer(region) || isPotentialOcrAdContainer(region, snapshot.rootBounds)) &&
          region.bounds.contains(targetBounds.centerX(), targetBounds.centerY())
      }
      .filter { region ->
        val localLines = frame.lines.filter { line ->
          region.bounds.contains(line.screenBounds.centerX(), line.screenBounds.centerY())
        }
        val promoMarkers = (COMMERCIAL_AD_MARKERS + OCR_PROMOTIONAL_OFFER_MARKERS).distinct()
        localLines.any { hasStrongAdSignal(it.text) } ||
          hasStrongAdSignal(joinOcrLineText(localLines)) ||
          countDistinctOcrMarkers(localLines, COMMERCIAL_AD_MARKERS) >=
          MIN_OCR_OVERLAY_COMMERCIAL_SIGNALS ||
          (countDistinctOcrMarkers(localLines, promoMarkers) >=
            MIN_OCR_OVERLAY_COMMERCIAL_SIGNALS && hasOcrPromotionalAction(localLines))
      }
      .minByOrNull { it.bounds.width().toLong() * it.bounds.height().toLong() }
      ?: return false
    val overlayLines = frame.lines.filter { line ->
      overlayRegion.bounds.contains(line.screenBounds.centerX(), line.screenBounds.centerY())
    }
    val root = snapshot.rootBounds
    val rootArea = root.width().toLong().coerceAtLeast(1L) * root.height().toLong().coerceAtLeast(1L)
    val overlayArea = overlayRegion.bounds.width().toLong().coerceAtLeast(0L) *
      overlayRegion.bounds.height().toLong().coerceAtLeast(0L)
    val boundedOverlay = overlayArea.toDouble() / rootArea <= MAX_AD_CONTEXT_REGION_RATIO
    val centralLines = overlayLines.filter { line ->
      val centerX = line.screenBounds.centerX()
      val centerY = line.screenBounds.centerY()
      centerX in (root.left + root.width() * 0.12f).toInt()..(root.left + root.width() * 0.88f).toInt() &&
        centerY in (root.top + root.height() * 0.12f).toInt()..(root.top + root.height() * 0.90f).toInt()
    }
    val adMarker = overlayLines.any { hasStrongAdSignal(it.text) } ||
      hasStrongAdSignal(joinOcrLineText(overlayLines))
    val commercialCount = countDistinctOcrMarkers(centralLines, COMMERCIAL_AD_MARKERS)
    val callToAction = hasOcrPromotionalAction(centralLines)
    val overlayHintCount = overlayLines.count { line -> SCREEN_OVERLAY_HINTS.any(line.compactText::contains) }
    val explicitOverlayCopy = overlayHintCount >= 2 && adMarker &&
      (commercialCount >= 2 || callToAction)
    val densePosterCopy = centralLines.size >= MIN_OCR_OVERLAY_LINES &&
      adMarker && commercialCount >= MIN_OCR_OVERLAY_COMMERCIAL_SIGNALS && callToAction
    val semanticOcrEvidence = (boundedOverlay &&
      (adMarker || commercialCount >= MIN_OCR_OVERLAY_COMMERCIAL_SIGNALS)) ||
      explicitOverlayCopy || densePosterCopy
    if (isOverlayLayer(overlayRegion)) return semanticOcrEvidence

    val localPromoMarkers = (COMMERCIAL_AD_MARKERS + OCR_PROMOTIONAL_OFFER_MARKERS).distinct()
    val localPromoCount = countDistinctOcrMarkers(overlayLines, localPromoMarkers)
    val regionArea = overlayRegion.bounds.width().toLong().coerceAtLeast(0L) *
      overlayRegion.bounds.height().toLong().coerceAtLeast(0L)
    return AdDecisionPolicy.allowOcrBackedPopupClose(
      hasCloseSemantic = isPopupCloseLabel(node.compactLabel) ||
        hasAdCloseResourceId(node.resourceId) ||
        hasGenericCloseResourceId(node.resourceId) ||
        frame.closeGlyphText.any { isCompactOcrCloseGlyph(it, targetBounds, root) },
      isBoundedLocalRegion = regionArea.toDouble() / rootArea in
        MIN_OCR_POPUP_REGION_AREA_RATIO..MAX_AD_CONTEXT_REGION_RATIO,
      hasExplicitAdMarker = overlayLines.any { hasStrongAdSignal(it.text) },
      promotionalSignalCount = localPromoCount,
      hasPromotionalAction = hasOcrPromotionalAction(overlayLines),
      isIndependentClose = isIndependentCloseTarget(targetBounds, root),
      isAtRegionEdge = isNearAdRegionEdge(targetBounds, overlayRegion.bounds),
    )
  }

  private fun isPotentialOcrAdContainer(region: AncestorRegion, rootBounds: Rect): Boolean {
    if (region.path == "0" || SCROLL_CONTAINER_CLASSES.any(region.className::contains) ||
      !hasUsableBounds(region.bounds)
    ) return false
    val rootArea = rootBounds.width().toLong().coerceAtLeast(1L) *
      rootBounds.height().toLong().coerceAtLeast(1L)
    val regionArea = region.bounds.width().toLong() * region.bounds.height().toLong()
    return regionArea.toDouble() / rootArea in
      MIN_OCR_POPUP_REGION_AREA_RATIO..MAX_AD_CONTEXT_REGION_RATIO &&
      region.bounds.width() >= rootBounds.width() * MIN_OCR_POPUP_REGION_WIDTH_RATIO &&
      region.bounds.height() >= rootBounds.height() * MIN_OCR_POPUP_REGION_HEIGHT_RATIO
  }

  private fun isNearAdRegionEdge(targetBounds: Rect, regionBounds: Rect): Boolean {
    if (!hasUsableBounds(targetBounds) || !hasUsableBounds(regionBounds) ||
      !regionBounds.contains(targetBounds.centerX(), targetBounds.centerY())
    ) return false
    val x = (targetBounds.centerX() - regionBounds.left).toFloat() /
      regionBounds.width().coerceAtLeast(1)
    val y = (targetBounds.centerY() - regionBounds.top).toFloat() /
      regionBounds.height().coerceAtLeast(1)
    return (x >= OCR_POPUP_CLOSE_RIGHT_RATIO && y <= OCR_POPUP_CLOSE_TOP_RATIO) ||
      ((x <= OCR_POPUP_CLOSE_SIDE_RATIO || x >= 1f - OCR_POPUP_CLOSE_SIDE_RATIO) &&
        y in OCR_POPUP_CLOSE_SIDE_Y_MIN..OCR_POPUP_CLOSE_SIDE_Y_MAX)
  }

  private fun isNearOcrPopupScreenEdge(bounds: Rect, rootBounds: Rect): Boolean {
    if (isNearTopRight(bounds, rootBounds)) return true
    val width = rootBounds.width().coerceAtLeast(1)
    val height = rootBounds.height().coerceAtLeast(1)
    val centerX = bounds.centerX()
    val centerY = bounds.centerY()
    val nearSide = (centerX <= rootBounds.left + width * OCR_POPUP_SCREEN_SIDE_RATIO ||
      centerX >= rootBounds.left + width * (1f - OCR_POPUP_SCREEN_SIDE_RATIO)) &&
      centerY in (rootBounds.top + height * OCR_POPUP_CLOSE_SIDE_Y_MIN).toInt()..
        (rootBounds.top + height * OCR_POPUP_CLOSE_SIDE_Y_MAX).toInt()
    return nearSide
  }

  private fun joinOcrLineText(lines: List<OcrLine>): String = lines
    .sortedWith(compareBy<OcrLine> { it.screenBounds.centerY() }.thenBy { it.screenBounds.left })
    .joinToString(separator = "") { it.compactText }

  private fun countDistinctOcrMarkers(lines: List<OcrLine>, markers: List<String>): Int =
    countDistinctMatchingMarkers(
      lines.map { it.compactText } + joinOcrLineText(lines),
      markers,
    )

  private fun hasOcrPromotionalAction(
    lines: List<OcrLine>,
    includeSplashActions: Boolean = false,
  ): Boolean {
    if (lines.any(::hasPromotionalCallToAction)) return true
    val combinedText = joinOcrLineText(lines)
    val markers = if (includeSplashActions) {
      PROMOTIONAL_CTA_MARKERS + SPLASH_ACTION_MARKERS
    } else {
      PROMOTIONAL_CTA_MARKERS
    }
    return markers.any { marker -> combinedText.contains(normalizeLabel(marker)) }
  }

  /**
   * Handles the observed Caiyun Weather SVIP campaign without relaxing generic
   * popup rules. That window exposes a large `entrance_image` poster and an
   * unlabeled `close` ImageView centered just below it, but no popup/overlay
   * ancestor or ad label. OCR must identify the campaign entirely inside that
   * poster, from a fresh frame captured for the same accessibility tree.
   */
  private fun findKnownCenteredPosterAdEvidence(
    node: ScreenNode,
    target: AccessibilityNodeInfo,
    targetBounds: Rect,
    targetResourceId: String,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): String? {
    val packageName = node.node.packageName?.toString().orEmpty()
    if (packageName != KNOWN_CENTERED_POSTER_PACKAGE || node.label.isNotBlank() ||
      !node.className.endsWith("imageview")
    ) return null
    if (!target.isVisibleToUser || !target.isEnabled || !node.node.isClickable ||
      target != node.node
    ) return null
    if (node.resourceId != KNOWN_CENTERED_POSTER_CLOSE_ID &&
      targetResourceId != KNOWN_CENTERED_POSTER_CLOSE_ID
    ) return null

    val currentFrame = frame ?: return null
    if (currentFrame.packageName != packageName ||
      currentFrame.accessibilitySignature != snapshot.signature ||
      System.currentTimeMillis() - currentFrame.capturedAt > OCR_FRAME_MAX_AGE_MS
    ) return null

    val rootBounds = snapshot.rootBounds
    val rootWidth = rootBounds.width().coerceAtLeast(1)
    val rootHeight = rootBounds.height().coerceAtLeast(1)
    val rootArea = rootWidth.toLong() * rootHeight.toLong()
    val closeArea = targetBounds.width().toLong() * targetBounds.height().toLong()
    val closeCenterXRatio =
      (targetBounds.centerX() - rootBounds.left).toFloat() / rootWidth
    val closeCenterYRatio =
      (targetBounds.centerY() - rootBounds.top).toFloat() / rootHeight
    if (targetBounds.width() > rootWidth * CENTERED_POSTER_CLOSE_MAX_WIDTH_RATIO ||
      targetBounds.height() > rootHeight * CENTERED_POSTER_CLOSE_MAX_HEIGHT_RATIO ||
      closeArea.toDouble() / rootArea > CENTERED_POSTER_CLOSE_MAX_AREA_RATIO ||
      closeCenterXRatio !in CENTERED_POSTER_CLOSE_CENTER_X_MIN..CENTERED_POSTER_CLOSE_CENTER_X_MAX ||
      closeCenterYRatio !in CENTERED_POSTER_CLOSE_CENTER_Y_MIN..CENTERED_POSTER_CLOSE_CENTER_Y_MAX
    ) return null

    return snapshot.nodes.asSequence()
      .filter { poster ->
          poster.isVisible &&
          poster.className.endsWith("imageview") &&
          poster.resourceId == KNOWN_CENTERED_POSTER_IMAGE_ID &&
          poster.ancestorRegions.lastOrNull() != null &&
          poster.ancestorRegions.lastOrNull() == node.ancestorRegions.lastOrNull() &&
          isKnownCenteredPosterGeometry(poster.bounds, targetBounds, rootBounds)
      }
      .mapNotNull { poster ->
        val posterLines = currentFrame.lines.filter { line ->
          poster.bounds.contains(line.screenBounds.centerX(), line.screenBounds.centerY())
        }
        val posterText = posterLines.joinToString(separator = "") { it.compactText }
        val hasSvipCampaignCopy = posterText.contains("svip") &&
          posterText.contains("年卡") &&
          posterText.contains("会员") &&
          (posterText.contains("抽奖") || posterText.contains("奖品")) &&
          posterText.contains("立即查看")
        val commercialSignals = countDistinctMatchingMarkers(
          posterLines.map { it.compactText },
          COMMERCIAL_AD_MARKERS,
        )
        if (posterLines.size < MIN_CENTERED_POSTER_OCR_LINES ||
          commercialSignals < MIN_OCR_OVERLAY_COMMERCIAL_SIGNALS ||
          !hasSvipCampaignCopy
        ) {
          null
        } else {
          posterLines.asSequence()
            .map { it.compactText }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_FEED_FINGERPRINT_LINES)
            .joinToString("|")
            .take(240)
            .ifBlank { null }
        }
      }
      .firstOrNull()
  }

  /**
   * Handles the observed Caiyun Weather SDK interstitial layout only when its
   * explicit ad badge, interstitial container, action hint, and bottom-center
   * close ImageView are all present in the same accessibility window.
   */
  private fun findKnownCaiyunInterstitialAdEvidence(
    node: ScreenNode,
    target: AccessibilityNodeInfo,
    targetBounds: Rect,
    targetResourceId: String,
    snapshot: AccessibilitySnapshot,
  ): String? {
    val packageName = node.node.packageName?.toString().orEmpty()
    if (packageName != KNOWN_CAIYUN_INTERSTITIAL_PACKAGE || node.label.isNotBlank() ||
      !node.className.endsWith("imageview") ||
      (node.resourceId != KNOWN_CAIYUN_INTERSTITIAL_CLOSE_ID &&
        targetResourceId != KNOWN_CAIYUN_INTERSTITIAL_CLOSE_ID)
    ) return null
    if (!target.isVisibleToUser || !target.isEnabled || !node.node.isClickable ||
      target != node.node
    ) return null

    val interstitialRegion = node.ancestorRegions.firstOrNull { region ->
      region.resourceId == KNOWN_CAIYUN_INTERSTITIAL_CONTAINER_ID &&
        region.bounds.contains(targetBounds.centerX(), targetBounds.centerY())
    } ?: return null
    if (node.ancestorRegions.none { region ->
        region.resourceId == KNOWN_CAIYUN_INTERSTITIAL_AD_VIEW_ID &&
          region.bounds.contains(targetBounds.centerX(), targetBounds.centerY())
      }
    ) return null

    val rootBounds = interstitialRegion.bounds
    val rootWidth = rootBounds.width().coerceAtLeast(1)
    val rootHeight = rootBounds.height().coerceAtLeast(1)
    val closeArea = targetBounds.width().toLong() * targetBounds.height().toLong()
    val rootArea = rootWidth.toLong() * rootHeight.toLong()
    val closeCenterX = (targetBounds.centerX() - rootBounds.left).toFloat() / rootWidth
    val closeCenterY = (targetBounds.centerY() - rootBounds.top).toFloat() / rootHeight
    val closeBottomGap = (rootBounds.bottom - targetBounds.bottom).toFloat() / rootHeight
    if (targetBounds.width() > rootWidth * CAIYUN_INTERSTITIAL_CLOSE_MAX_WIDTH_RATIO ||
      targetBounds.height() > rootHeight * CAIYUN_INTERSTITIAL_CLOSE_MAX_HEIGHT_RATIO ||
      closeArea.toDouble() / rootArea > CAIYUN_INTERSTITIAL_CLOSE_MAX_AREA_RATIO ||
      closeCenterX !in CAIYUN_INTERSTITIAL_CLOSE_CENTER_X_MIN..CAIYUN_INTERSTITIAL_CLOSE_CENTER_X_MAX ||
      closeCenterY !in CAIYUN_INTERSTITIAL_CLOSE_CENTER_Y_MIN..CAIYUN_INTERSTITIAL_CLOSE_CENTER_Y_MAX ||
      closeBottomGap !in 0f..CAIYUN_INTERSTITIAL_CLOSE_MAX_BOTTOM_GAP_RATIO
    ) return null

    fun isInSameInterstitialWindow(candidate: ScreenNode): Boolean =
      candidate.isVisible && AdDecisionPolicy.isDescendantOf(candidate.path, interstitialRegion.path)

    val explicitAdBadge = snapshot.nodes.firstOrNull { candidate ->
      isInSameInterstitialWindow(candidate) &&
        candidate.resourceId == KNOWN_CAIYUN_AD_BADGE_ID &&
        candidate.compactLabel == normalizeLabel("广告")
    } ?: return null
    val adActionHint = snapshot.nodes.firstOrNull { candidate ->
      isInSameInterstitialWindow(candidate) &&
        candidate.resourceId == KNOWN_CAIYUN_AD_ACTION_HINT_ID &&
        candidate.compactLabel.contains("跳转") &&
        (candidate.compactLabel.contains("详情页") || candidate.compactLabel.contains("第三方应用"))
    } ?: return null

    return listOf(
      KNOWN_CAIYUN_INTERSTITIAL_CLOSE_ID,
      explicitAdBadge.compactLabel,
      adActionHint.compactLabel,
    ).joinToString("|").take(240)
  }

  private fun isKnownCenteredPosterGeometry(
    posterBounds: Rect,
    closeBounds: Rect,
    rootBounds: Rect,
  ): Boolean {
    if (!hasUsableBounds(posterBounds) || !hasUsableBounds(closeBounds)) return false
    val rootWidth = rootBounds.width().coerceAtLeast(1)
    val rootHeight = rootBounds.height().coerceAtLeast(1)
    val rootArea = rootWidth.toLong() * rootHeight.toLong()
    val posterArea = posterBounds.width().toLong() * posterBounds.height().toLong()
    val posterAreaRatio = posterArea.toDouble() / rootArea
    val posterCenterXRatio = (posterBounds.centerX() - rootBounds.left).toFloat() / rootWidth
    val verticalGapRatio = (closeBounds.top - posterBounds.bottom).toFloat() / rootHeight
    val maxCenterDelta = (rootWidth * CENTERED_POSTER_MAX_CENTER_DELTA_RATIO).toInt()

    return posterBounds.width() >= rootWidth * CENTERED_POSTER_MIN_IMAGE_WIDTH_RATIO &&
      posterBounds.height() >= rootHeight * CENTERED_POSTER_MIN_IMAGE_HEIGHT_RATIO &&
      posterAreaRatio in CENTERED_POSTER_MIN_IMAGE_AREA_RATIO..CENTERED_POSTER_MAX_IMAGE_AREA_RATIO &&
      posterCenterXRatio in CENTERED_POSTER_IMAGE_CENTER_X_MIN..CENTERED_POSTER_IMAGE_CENTER_X_MAX &&
      kotlin.math.abs(posterBounds.centerX() - closeBounds.centerX()) <= maxCenterDelta &&
      verticalGapRatio in CENTERED_POSTER_CLOSE_GAP_MIN..CENTERED_POSTER_CLOSE_GAP_MAX
  }

  private fun hasFullScreenAdEvidenceWithinOverlay(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
  ): Boolean {
    val overlayRegion = node.ancestorRegions.asSequence()
      .filter { region ->
        isOverlayLayer(region) && isLargeOverlayBounds(region.bounds, snapshot.rootBounds) &&
          region.bounds.contains(node.bounds.centerX(), node.bounds.centerY())
      }
      .minByOrNull { it.bounds.width().toLong() * it.bounds.height().toLong() }
      ?: return false
    val rootArea = snapshot.rootBounds.width().toLong().coerceAtLeast(1L) *
      snapshot.rootBounds.height().toLong().coerceAtLeast(1L)
    val overlayArea = overlayRegion.bounds.width().toLong().coerceAtLeast(0L) *
      overlayRegion.bounds.height().toLong().coerceAtLeast(0L)
    // A full-screen root can contain unrelated page banners. For this broad
    // fallback, require OCR's denser poster-copy rule instead of mixing all
    // visible page labels into a single ad score.
    if (overlayArea.toDouble() / rootArea > MAX_AD_CONTEXT_REGION_RATIO) return false
    val overlayNodes = snapshot.nodes.filter { candidate ->
      candidate.isVisible && AdDecisionPolicy.isDescendantOf(candidate.path, overlayRegion.path)
    }
    return AdContext.from(overlayNodes, snapshot.rootBounds).hasFullScreenAdEvidence
  }

  private fun isPotentialFeedCard(
    region: AncestorRegion,
    nodeBounds: Rect,
    targetBounds: Rect,
    rootBounds: Rect,
  ): Boolean {
    if (!hasUsableBounds(region.bounds) || !region.bounds.contains(nodeBounds.centerX(), nodeBounds.centerY()) ||
      !region.bounds.contains(targetBounds.centerX(), targetBounds.centerY())
    ) return false
    if (hasPopupSignal(region.compactLabel, region.resourceId, region.className) ||
      region.resourceId.contains("overlay") || region.className.contains("overlay") ||
      SCROLL_CONTAINER_CLASSES.any(region.className::contains)
    ) return false
    val rootArea = rootBounds.width().toLong().coerceAtLeast(1L) *
      rootBounds.height().toLong().coerceAtLeast(1L)
    val regionArea = region.bounds.width().toLong() * region.bounds.height().toLong()
    val ratio = regionArea.toDouble() / rootArea
    return ratio in MIN_FEED_CARD_AREA_RATIO..MAX_FEED_CARD_AREA_RATIO &&
      region.bounds.width() >= rootBounds.width() * MIN_FEED_CARD_WIDTH_RATIO &&
      region.bounds.height() >= rootBounds.height() * MIN_FEED_CARD_HEIGHT_RATIO
  }

  private fun findFeedAdEvidence(
    region: AncestorRegion,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame,
  ): Pair<Boolean, String>? {
    if (frame.packageName != snapshot.nodes.firstOrNull()?.node?.packageName?.toString() ||
      System.currentTimeMillis() - frame.capturedAt > OCR_FRAME_MAX_AGE_MS
    ) return null
    val ocrLines = frame.lines.filter { line ->
      region.bounds.contains(line.screenBounds.centerX(), line.screenBounds.centerY())
    }
    val treeLabels = snapshot.nodes.asSequence()
      .filter { it.isVisible && AdDecisionPolicy.isDescendantOf(it.path, region.path) }
      .map { it.label }
      .filter { it.isNotBlank() }
      .toList()
    val explicitAdMarker = ocrLines.any { hasStrongAdSignal(it.text) } ||
      treeLabels.any(::hasStrongAdSignal)
    val commercialSignals = countDistinctMatchingMarkers(
      ocrLines.map { it.compactText } + treeLabels.map(::normalizeLabel),
      COMMERCIAL_AD_MARKERS,
    )
    val hasCallToAction = ocrLines.any(::hasPromotionalCallToAction) ||
      treeLabels.any { hasPromotionalCallToActionText(it) }
    // Prefer an explicit in-card marker (for example the "推荐广告" badge in
    // the weather-page sample). Without it, require three distinct commercial
    // signals plus a call to action, all still inside this same card.
    if (!explicitAdMarker &&
      (commercialSignals < MIN_FEED_COMMERCIAL_SIGNALS || !hasCallToAction)
    ) return null

    val fingerprint = ocrLines.asSequence().map { it.compactText }
      .filter { it.isNotBlank() }
      .distinct()
      .take(MAX_FEED_FINGERPRINT_LINES)
      .joinToString(";")
    return explicitAdMarker to fingerprint.ifBlank { "accessible-ad-marker" }
  }

  private fun isCloseAtCardEdge(
    node: ScreenNode,
    targetBounds: Rect,
    cardBounds: Rect,
    adSpecificClose: Boolean,
    closeVisualBounds: Rect? = null,
  ): Boolean {
    val width = cardBounds.width().coerceAtLeast(1)
    val height = cardBounds.height().coerceAtLeast(1)
    val visualBounds = closeVisualBounds?.takeIf(::hasUsableBounds) ?: targetBounds
    val x = (visualBounds.centerX() - cardBounds.left).toFloat() / width
    val y = (visualBounds.centerY() - cardBounds.top).toFloat() / height
    val nearTopRight = x >= FEED_CLOSE_RIGHT_EDGE_RATIO && y <= FEED_CLOSE_TOP_EDGE_RATIO
    if (nearTopRight) return true
    if (!adSpecificClose) return false
    val nearAnyEdge = x <= FEED_CLOSE_SIDE_EDGE_RATIO || x >= 1f - FEED_CLOSE_SIDE_EDGE_RATIO ||
      y <= FEED_CLOSE_TOP_EDGE_RATIO || y >= 1f - FEED_CLOSE_TOP_EDGE_RATIO
    return nearAnyEdge && (node.compactLabel.contains("关闭") || node.compactLabel.contains("close"))
  }

  private fun isLikelyUnlabeledCloseIcon(node: ScreenNode, target: AccessibilityNodeInfo): Boolean {
    if (node.label.isNotBlank()) return false
    val resourceId = node.resourceId + " " + target.viewIdResourceName.orEmpty().lowercase(Locale.ROOT)
    return AdDecisionPolicy.allowUnlabeledClose(hasGenericCloseResourceId(resourceId), false)
  }

  private fun isCompactOcrCloseGlyph(line: OcrLine, targetBounds: Rect, rootBounds: Rect): Boolean {
    if (line.compactText !in OCR_CLOSE_GLYPH_LABELS ||
      !targetBounds.contains(line.screenBounds.centerX(), line.screenBounds.centerY())
    ) return false
    val width = line.screenBounds.width()
    val height = line.screenBounds.height()
    if (width <= 0 || height <= 0) return false
    val aspectRatio = width.toFloat() / height
    return width <= rootBounds.width() * MAX_OCR_CLOSE_GLYPH_WIDTH_RATIO &&
      height <= rootBounds.height() * MAX_OCR_CLOSE_GLYPH_HEIGHT_RATIO &&
      aspectRatio in MIN_OCR_CLOSE_GLYPH_ASPECT_RATIO..MAX_OCR_CLOSE_GLYPH_ASPECT_RATIO
  }

  private fun hasPromotionalCallToAction(line: OcrLine): Boolean =
    hasPromotionalCallToActionText(line.compactText)

  private fun hasPromotionalCallToActionText(value: String): Boolean =
    PROMOTIONAL_CTA_MARKERS.any(normalizeLabel(value)::contains)

  private fun countDistinctMatchingMarkers(texts: List<String>, markers: List<String>): Int =
    markers.count { marker -> texts.any { it.contains(marker) } }

  private fun isAdRelatedOcrText(text: String): Boolean {
    val compact = normalizeLabel(text)
    return hasStrongAdSignal(text) || hasCommercialAdSignal(compact) ||
      hasPromotionalCallToActionText(compact)
  }

  private fun isLocalRegion(bounds: Rect, rootBounds: Rect, maxAreaRatio: Double): Boolean {
    if (!hasUsableBounds(bounds) || !hasUsableBounds(rootBounds)) return false
    val rootArea = rootBounds.width().toLong() * rootBounds.height().toLong()
    val regionArea = bounds.width().toLong() * bounds.height().toLong()
    return regionArea.toDouble() / rootArea <= maxAreaRatio
  }

  private fun isLocalAdRegion(region: AncestorRegion, rootBounds: Rect): Boolean =
    isLocalRegion(region.bounds, rootBounds, MAX_AD_CONTEXT_REGION_RATIO) &&
      SCROLL_CONTAINER_CLASSES.none(region.className::contains)

  private fun findRootForPackage(packageName: String): AccessibilityNodeInfo? {
    if (!isCurrentForegroundPackage(packageName)) return null
    val active = rootInActiveWindow
    if (active != null) {
      val activePackage = active.packageName?.toString()
      if (activePackage == packageName) return active
    }
    return runCatching {
      windows.asSequence()
        .mapNotNull { window -> runCatching { window.root }.getOrNull() }
        .firstOrNull { root -> root.packageName?.toString() == packageName }
    }.getOrNull()
  }

  private fun isIndependentCloseTarget(bounds: Rect, rootBounds: Rect): Boolean {
    if (!hasUsableBounds(bounds) || !hasUsableBounds(rootBounds)) return false
    val rootArea = rootBounds.width().toLong() * rootBounds.height().toLong()
    val targetArea = bounds.width().toLong() * bounds.height().toLong()
    return bounds.width() <= rootBounds.width() * 0.18f &&
      bounds.height() <= rootBounds.height() * 0.12f &&
      targetArea.toDouble() / rootArea <= 0.02
  }

  private fun clickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    var current: AccessibilityNodeInfo? = node
    repeat(MAX_CLICKABLE_ANCESTORS) {
      if (current?.isVisibleToUser == true && current.isEnabled && current.isClickable) {
        return current
      }
      current = current?.parent
    }
    return null
  }

  private fun wasRecentlyAttempted(key: String, now: Long): Boolean =
    recentActions[key]?.let { now - it < ACTION_REPEAT_GUARD_MS } == true

  private fun rememberAction(key: String, now: Long) {
    recentActions[key] = now
    while (recentActions.size > MAX_REMEMBERED_ACTIONS) {
      recentActions.remove(recentActions.entries.first().key)
    }
  }

  private fun appendActionEvent(
    packageName: String,
    action: DetectionAction,
    result: String,
    text: String,
  ) {
    store.appendEvent(
      type = "skip",
      packageName = packageName,
      ruleId = action.ruleId,
      result = result,
      text = text,
    )
  }

  private data class PendingNode(
    val node: AccessibilityNodeInfo,
    val path: String,
    val depth: Int,
    val ancestorLabels: List<String>,
    val ancestorRegions: List<AncestorRegion>,
  )

  private data class MediaWatch(val packageName: String, val deadlineAt: Long)

  private data class AccessibilitySnapshot(
    val nodes: List<ScreenNode>,
    val rootBounds: Rect,
    val packageName: String,
    val signature: String,
  ) {
    companion object {
      fun capture(root: AccessibilityNodeInfo): AccessibilitySnapshot {
        val nodes = mutableListOf<ScreenNode>()
        val pending = ArrayDeque<PendingNode>()
        pending.add(PendingNode(root, "0", 0, emptyList(), emptyList()))
        val rootBounds = Rect().also(root::getBoundsInScreen)

        while (pending.isNotEmpty() && nodes.size < MAX_NODES_PER_SNAPSHOT) {
          val current = pending.removeFirst()
          val node = current.node
          val label = readNodeLabel(node)
          val resourceId = node.viewIdResourceName.orEmpty().lowercase(Locale.ROOT)
          val className = node.className?.toString().orEmpty().lowercase(Locale.ROOT)
          val bounds = Rect().also(node::getBoundsInScreen)
          val visible = node.isVisibleToUser && hasUsableBounds(bounds)
          nodes += ScreenNode(
            node = node,
            path = current.path,
            label = label,
            compactLabel = normalizeLabel(label),
            resourceId = resourceId,
            className = className,
            ancestorLabels = current.ancestorLabels,
            ancestorRegions = current.ancestorRegions,
            bounds = bounds,
            isVisible = visible,
            isEnabled = node.isEnabled,
            isEditable = node.isEditable,
            isPassword = node.isPassword,
            isFocused = node.isFocused,
            depth = current.depth,
          )

          val nextAncestors = if (label.isBlank()) {
            current.ancestorLabels
          } else {
            (current.ancestorLabels + label).takeLast(MAX_ANCESTOR_LABELS)
          }
          val nextAncestorRegions = (current.ancestorRegions + AncestorRegion(
            path = current.path,
            label = label,
            compactLabel = normalizeLabel(label),
            resourceId = resourceId,
            className = className,
            bounds = bounds,
          )).takeLast(MAX_ANCESTOR_REGIONS)
          if (current.depth >= MAX_TREE_DEPTH) continue
          for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
              pending.add(PendingNode(child, "${current.path}.$index", current.depth + 1,
                nextAncestors, nextAncestorRegions))
            }
          }
        }

        return AccessibilitySnapshot(
          nodes = nodes,
          rootBounds = rootBounds,
          packageName = root.packageName?.toString().orEmpty(),
          signature = nodes.asSequence()
            .filter { it.isVisible }
            .joinToString("|") { node ->
              "${node.path}:${node.compactLabel}:${node.resourceId}:${node.className}:" +
                "${node.bounds.left},${node.bounds.top},${node.bounds.right},${node.bounds.bottom}"
            }
            .hashCode()
            .toString(16),
        )
      }
    }
  }

  private data class ScreenNode(
    val node: AccessibilityNodeInfo,
    val path: String,
    val label: String,
    val compactLabel: String,
    val resourceId: String,
    val className: String,
    val ancestorLabels: List<String>,
    val ancestorRegions: List<AncestorRegion>,
    val bounds: Rect,
    val isVisible: Boolean,
    val isEnabled: Boolean,
    val isEditable: Boolean,
    val isPassword: Boolean,
    val isFocused: Boolean,
    val depth: Int,
  )

  private data class AncestorRegion(
    val path: String,
    val label: String,
    val compactLabel: String,
    val resourceId: String,
    val className: String,
    val bounds: Rect,
  )

  private data class OcrLine(
    val text: String,
    val compactText: String,
    val screenBounds: Rect,
  )

  private data class OcrFrame(
    val packageName: String,
    val capturedAt: Long,
    val accessibilitySignature: String,
    val lines: List<OcrLine>,
    val elements: List<OcrLine>,
  ) {
    val skipText: List<OcrLine> by lazy {
      (elements + lines).filter { isSkipOcrLabel(it.compactText) }
    }
    val closeGlyphText: List<OcrLine> by lazy {
      lines.filter { it.compactText in OCR_CLOSE_GLYPH_LABELS }
    }
  }

  private enum class DetectionCategory {
    SPLASH_VIDEO,
    POPUP,
    FEED,
  }

  private data class AdContext(
    val strongSignals: Int,
    val mediumSignals: Int,
    val commercialSignals: Int,
    val popupSignals: Int,
    val largeOverlaySignals: Int,
  ) {
    val hasStrongAdSignal: Boolean get() = strongSignals > 0
    val hasCommercialAdSignal: Boolean get() =
      commercialSignals >= 2 || (commercialSignals > 0 && popupSignals > 0)
    val hasAdSignal: Boolean get() =
      hasStrongAdSignal || mediumSignals >= 2 || hasCommercialAdSignal
    val hasFullScreenAdEvidence: Boolean
      get() {
        // Some ad SDKs expose the marketing copy but omit the literal "广告"
        // node. Four independent commercial markers are a safe fallback only
        // when the same screen also has a medium ad action and a large overlay.
        val hasAdTextCluster =
          (strongSignals > 0 && commercialSignals >= 2) || commercialSignals >= 4
        return hasAdTextCluster && mediumSignals > 0 && largeOverlaySignals > 0
      }
    companion object {
      fun from(nodes: List<ScreenNode>, rootBounds: Rect): AdContext {
        var strong = 0
        var medium = 0
        var commercial = 0
        var popup = 0
        var largeOverlay = 0
        val rootArea = rootBounds.width().toLong().coerceAtLeast(1L) *
          rootBounds.height().toLong().coerceAtLeast(1L)
        nodes.forEach { node ->
          if (!node.isVisible) return@forEach
          if (hasStrongAdSignal(node.label, node.resourceId)) strong++
          medium += countMatchingMarkers(node.compactLabel, MEDIUM_AD_MARKERS)
          commercial += countMatchingMarkers(node.compactLabel, COMMERCIAL_AD_MARKERS)
          if (hasPopupSignal(node.compactLabel, node.resourceId, node.className)) popup++
          val nodeArea = node.bounds.width().toLong().coerceAtLeast(0L) *
            node.bounds.height().toLong().coerceAtLeast(0L)
          if (node.depth > 0 &&
            nodeArea >= rootArea * LARGE_OVERLAY_MIN_AREA_RATIO &&
            node.bounds.width() >= rootBounds.width() * LARGE_OVERLAY_MIN_WIDTH_RATIO &&
            node.bounds.height() >= rootBounds.height() * LARGE_OVERLAY_MIN_HEIGHT_RATIO &&
            isOverlayLayer(node)
          ) {
            largeOverlay++
          }
        }
        return AdContext(
          strongSignals = strong.coerceAtMost(3),
          mediumSignals = medium.coerceAtMost(8),
          commercialSignals = commercial.coerceAtMost(8),
          popupSignals = popup.coerceAtMost(4),
          largeOverlaySignals = largeOverlay.coerceAtMost(4),
        )
      }
    }
  }

  private data class DetectionAction(
    val target: AccessibilityNodeInfo,
    val targetBounds: Rect,
    val score: Int,
    val minimumScore: Int,
    val actionPriority: Int,
    val ruleId: String,
    val label: String,
    val successText: String,
    val category: DetectionCategory,
    val evidenceFingerprint: String = "",
  ) {
    fun targetFingerprint(packageName: String): String = buildString {
      append(packageName)
      append('|')
      append(category.name)
      append('|')
      append(label)
      append('|')
      append(evidenceFingerprint)
      append('|')
      append(targetBounds.left)
      append(',')
      append(targetBounds.top)
      append(',')
      append(targetBounds.right)
      append(',')
      append(targetBounds.bottom)
    }
  }

  companion object {
    @Volatile
    var running: Boolean = false

    private const val TAG = "ClearScreenA11y"
    private const val ACTION_COOLDOWN_MS = 480L
    private const val ACTION_VERIFY_DELAY_MS = 650L
    private const val ACTION_REPEAT_GUARD_MS = 1_500L
    private const val INITIAL_FOREGROUND_SCAN_DELAY_MS = 600L
    private const val OCR_EXECUTOR_KEEP_ALIVE_SECONDS = 30L
    private const val MAX_CLICKABLE_ANCESTORS = 6
    private const val MAX_NODES_PER_SNAPSHOT = 1_200
    private const val MAX_TREE_DEPTH = 32
    private const val MAX_ANCESTOR_LABELS = 8
    private const val MAX_ANCESTOR_REGIONS = 12
    private const val MAX_REMEMBERED_ACTIONS = 80
    private const val MAX_LOG_LABEL_LENGTH = 80
    private const val OCR_MIN_INTERVAL_MS = 1_200L
    private const val OCR_SIGNATURE_RETRY_DELAY_MS = 1_350L
    private const val OCR_FRAME_MAX_AGE_MS = 2_500L
    private const val MAX_OCR_TEXT_ELEMENTS = 900
    private const val FEED_RESCAN_DELAY_MS = 850L
    private const val MEDIA_WATCH_MAX_MS = 45_000L
    private const val MEDIA_WATCH_INTERVAL_MS = 900L
    private const val MEDIA_WATCH_OCR_INTERVAL_MS = 2_400L
    private const val MAX_FEED_CLOSE_TARGET_AREA_RATIO = 0.05
    private const val KNOWN_CENTERED_POSTER_PACKAGE = "com.nowcasting.activity"
    private const val KNOWN_CENTERED_POSTER_IMAGE_ID = "com.nowcasting.activity:id/entrance_image"
    private const val KNOWN_CENTERED_POSTER_CLOSE_ID = "com.nowcasting.activity:id/close"
    private const val CENTERED_POSTER_CLOSE_MAX_WIDTH_RATIO = 0.12f
    private const val CENTERED_POSTER_CLOSE_MAX_HEIGHT_RATIO = 0.10f
    private const val CENTERED_POSTER_CLOSE_MAX_AREA_RATIO = 0.015
    private const val CENTERED_POSTER_CLOSE_CENTER_X_MIN = 0.44f
    private const val CENTERED_POSTER_CLOSE_CENTER_X_MAX = 0.56f
    private const val CENTERED_POSTER_CLOSE_CENTER_Y_MIN = 0.62f
    private const val CENTERED_POSTER_CLOSE_CENTER_Y_MAX = 0.88f
    private const val CENTERED_POSTER_MIN_IMAGE_WIDTH_RATIO = 0.70f
    private const val CENTERED_POSTER_MIN_IMAGE_HEIGHT_RATIO = 0.30f
    private const val CENTERED_POSTER_MIN_IMAGE_AREA_RATIO = 0.24
    private const val CENTERED_POSTER_MAX_IMAGE_AREA_RATIO = 0.65
    private const val CENTERED_POSTER_IMAGE_CENTER_X_MIN = 0.42f
    private const val CENTERED_POSTER_IMAGE_CENTER_X_MAX = 0.58f
    private const val CENTERED_POSTER_MAX_CENTER_DELTA_RATIO = 0.05f
    private const val CENTERED_POSTER_CLOSE_GAP_MIN = -0.02f
    private const val CENTERED_POSTER_CLOSE_GAP_MAX = 0.09f
    private const val MIN_CENTERED_POSTER_OCR_LINES = 3
    private const val KNOWN_CAIYUN_INTERSTITIAL_PACKAGE = "com.nowcasting.activity"
    private const val KNOWN_CAIYUN_INTERSTITIAL_CLOSE_ID = "com.nowcasting.activity:id/cll_close_click"
    private const val KNOWN_CAIYUN_INTERSTITIAL_CONTAINER_ID = "com.nowcasting.activity:id/cll_interstitial_ad_gray"
    private const val KNOWN_CAIYUN_INTERSTITIAL_AD_VIEW_ID = "com.nowcasting.activity:id/cll_gdt_ad_view"
    private const val KNOWN_CAIYUN_AD_BADGE_ID = "com.nowcasting.activity:id/cll_ad_tips"
    private const val KNOWN_CAIYUN_AD_ACTION_HINT_ID = "com.nowcasting.activity:id/cll_ad_tip_title"
    private const val CAIYUN_INTERSTITIAL_CLOSE_MAX_WIDTH_RATIO = 0.14f
    private const val CAIYUN_INTERSTITIAL_CLOSE_MAX_HEIGHT_RATIO = 0.12f
    private const val CAIYUN_INTERSTITIAL_CLOSE_MAX_AREA_RATIO = 0.02
    private const val CAIYUN_INTERSTITIAL_CLOSE_CENTER_X_MIN = 0.42f
    private const val CAIYUN_INTERSTITIAL_CLOSE_CENTER_X_MAX = 0.58f
    private const val CAIYUN_INTERSTITIAL_CLOSE_CENTER_Y_MIN = 0.90f
    private const val CAIYUN_INTERSTITIAL_CLOSE_CENTER_Y_MAX = 1.03f
    private const val CAIYUN_INTERSTITIAL_CLOSE_MAX_BOTTOM_GAP_RATIO = 0.04f
    private const val MAX_FEED_CLOSE_TARGET_WIDTH_RATIO = 0.28f
    private const val MAX_FEED_CLOSE_TARGET_HEIGHT_RATIO = 0.24f
    private const val MAX_SKIP_TARGET_WIDTH_RATIO = 0.34f
    private const val MAX_SKIP_TARGET_HEIGHT_RATIO = 0.12f
    private const val MAX_SKIP_TARGET_AREA_RATIO = 0.04
    private const val TOP_CENTER_SKIP_CONTEXT_TOP_RATIO = 0.22f
    private const val TOP_CENTER_SKIP_CONTEXT_BODY_MIN_RATIO = 0.12f
    private const val TOP_CENTER_SKIP_CONTEXT_BODY_MAX_RATIO = 0.88f
    private const val TOP_CENTER_SKIP_MAX_WIDTH_RATIO = 0.28f
    private const val TOP_CENTER_SKIP_MAX_HEIGHT_RATIO = 0.12f
    private const val TOP_CENTER_SKIP_MAX_AREA_RATIO = 0.03
    private const val MIN_FEED_CARD_AREA_RATIO = 0.025
    private const val MAX_FEED_CARD_AREA_RATIO = 0.45
    private const val MIN_FEED_CARD_WIDTH_RATIO = 0.24f
    private const val MIN_FEED_CARD_HEIGHT_RATIO = 0.08f
    private const val MIN_FEED_COMMERCIAL_SIGNALS = 3
    private const val MIN_OCR_OVERLAY_LINES = 4
    private const val MIN_OCR_OVERLAY_COMMERCIAL_SIGNALS = 3
    private const val MIN_OCR_POPUP_REGION_AREA_RATIO = 0.025
    private const val MIN_OCR_POPUP_REGION_WIDTH_RATIO = 0.30f
    private const val MIN_OCR_POPUP_REGION_HEIGHT_RATIO = 0.08f
    private const val OCR_POPUP_CLOSE_RIGHT_RATIO = 0.62f
    private const val OCR_POPUP_CLOSE_TOP_RATIO = 0.42f
    private const val OCR_POPUP_CLOSE_SIDE_RATIO = 0.12f
    private const val OCR_POPUP_SCREEN_SIDE_RATIO = 0.18f
    private const val OCR_POPUP_CLOSE_SIDE_Y_MIN = 0.08f
    private const val OCR_POPUP_CLOSE_SIDE_Y_MAX = 0.92f
    private const val MIN_SPLASH_CREATIVE_WIDTH_RATIO = 0.60f
    private const val MIN_SPLASH_CREATIVE_HEIGHT_RATIO = 0.45f
    private const val MIN_SPLASH_CREATIVE_AREA_RATIO = 0.35
    private const val MAX_FEED_FINGERPRINT_LINES = 8
    private const val FEED_CLOSE_RIGHT_EDGE_RATIO = 0.64f
    private const val FEED_CLOSE_TOP_EDGE_RATIO = 0.40f
    private const val FEED_CLOSE_SIDE_EDGE_RATIO = 0.20f
    private const val MAX_OCR_CLOSE_GLYPH_WIDTH_RATIO = 0.08f
    private const val MAX_OCR_CLOSE_GLYPH_HEIGHT_RATIO = 0.06f
    private const val MIN_OCR_CLOSE_GLYPH_ASPECT_RATIO = 0.40f
    private const val MAX_OCR_CLOSE_GLYPH_ASPECT_RATIO = 2.50f
    private const val LARGE_OVERLAY_MIN_AREA_RATIO = 0.24
    private const val MAX_AD_CONTEXT_REGION_RATIO = 0.78
    private const val LARGE_OVERLAY_MIN_WIDTH_RATIO = 0.58f
    private const val LARGE_OVERLAY_MIN_HEIGHT_RATIO = 0.32f
    private val DYNAMIC_AD_RETRY_DELAYS_MS = longArrayOf(240L, 600L, 1_000L)

    private val COUNTDOWN_SKIP_PATTERN = Regex(
      "^(?:\\d{1,3}(?:秒|s)?跳过(?:广告|视频|此广告)?|跳过(?:广告|视频|此广告)?\\d{1,3}(?:秒|s)?|\\d{1,3}(?:秒|s)?skip(?:ad|thisad|video)?)$",
    )
    private val STRONG_SKIP_LABELS = setOf(
      "跳过广告",
      "跳过此广告",
      "跳过视频",
      "skipad",
      "skipthisad",
      "skipvideo",
    )
    private val POPUP_CLOSE_LABELS = setOf(
      "关闭",
      "关闭广告",
      "关闭弹窗",
      "close",
      "closead",
      "dismiss",
      "×",
      "✕",
      "✖",
      "✗",
      "╳",
      "⨯",
      "x",
    )
    private val OCR_CLOSE_GLYPH_LABELS = setOf("x", "×", "✕", "✖", "✗", "╳", "⨯")
    private val STRONG_AD_MARKERS = listOf(
      "广告",
      "广告位",
      "开屏",
      "推广",
      "赞助",
      "sponsored",
      "advertisement",
      "interstitial",
      "splashad",
    )
    // Negated phrases contain the literal marker but mean the opposite. Treating
    // them as ad evidence made an ordinary profile page ("免广告·铃声·全景声" plus
    // VIP/会员 entries) look like a full-screen ad and let nearby icons be clicked.
    // "关闭广告"/"跳过广告" are deliberately NOT listed here: they are the control's
    // own label and are a legitimate ad signal (for example a popup whose only
    // textual marker is its "关闭广告" button).
    private val NEGATED_AD_MARKERS = listOf(
      "免广告",
      "无广告",
      "去广告",
      "屏蔽广告",
      "拦截广告",
      "noads",
      "noad",
      "adfree",
    )
    // Real overlay layers are popup/ad containers, not the page's own content
    // scroller. Counting a RecyclerView as a "整页遮罩" was enough to satisfy the
    // full-screen ad rule on ordinary pages.
    private val SCROLL_CONTAINER_CLASSES = listOf(
      "recyclerview",
      "listview",
      "scrollview",
      "viewpager",
      "webview",
    )
    private val VIDEO_AD_CONTEXT_MARKERS = listOf(
      "互动广告",
      "视频广告",
      "广告视频",
      "贴片广告",
      "片头广告",
      "片中广告",
      "暂停广告",
      "广告剩余",
      "广告倒计时",
      "wifi预加载",
      "videoad",
      "prerollad",
      "midrollad",
    )
    private val VIDEO_AD_RESOURCE_MARKERS = listOf(
      "video_ad", "ad_video", "preroll", "pre_roll", "midroll", "mid_roll",
      "ad_player", "player_ad", "adplayer",
    )
    private val PLAYER_RESOURCE_MARKERS = listOf(
      "player_view", "playerview", "video_view", "videoview", "exo_content_frame",
      "ijkvideoview", "player_surface", "video_surface",
    )
    private val EXPLICIT_AD_BADGE_LABELS = setOf(
      "广告", "推荐广告", "广告推广", "赞助内容", "sponsored",
    )
    private val SCREEN_OVERLAY_HINTS = listOf(
      "翻转手机",
      "跳转详情页",
      "跳转第三方应用",
      "跳转第三方app",
      "关闭后继续",
      "广告倒计时",
    )
    private val TOP_CENTER_THIRD_PARTY_HINTS = listOf(
      "跳转第三方页面",
      "跳转第三方应用",
      "跳转第三方app",
    )
    private val TOP_CENTER_INTERSTITIAL_REWARD_MARKERS = listOf(
      "领金币",
      "金币",
      "有惊喜",
      "惊喜",
    )
    private val TOP_CENTER_INTERSTITIAL_ACTION_MARKERS = listOf(
      "领金币",
      "扭一扭",
      "立即下载",
      "立即打开",
    )
    private val PROMOTIONAL_CTA_MARKERS = listOf(
      "戳我下载",
      "点击立即下载",
      "立即办理",
      "立即打开",
      "立即下载",
      "点击跳转",
      "免费领取",
      "立即领取",
      "领券",
      "去使用",
      "立即使用",
      "点击使用",
      "去看看",
      "查看详情",
      "立即购买",
      "马上抢",
      "立即抢购",
      "立即体验",
      "开通会员",
      "详情请见",
      "了解详情",
    )
    private val OCR_PROMOTIONAL_OFFER_MARKERS = listOf(
      "优惠券",
      "福利",
      "惊喜",
      "限时",
      "补贴",
      "立减",
      "满减",
      "抽奖",
      "红包",
      "礼包",
      "券",
    )
    private val SPLASH_OFFER_MARKERS = listOf(
      "国家补贴",
      "国补",
      "补贴",
      "低至",
      "折起",
      "优惠",
      "限时",
      "焕新",
      "换新",
      "叠券",
      "超级88",
      "热卖中",
      "礼包",
      "福利",
      "活动",
      "直降",
    )
    private val SPLASH_ACTION_MARKERS = listOf(
      "领国家补贴",
      "立即领取",
      "点击跳转",
      "详情请见",
      "活动与规则",
      "活动规则",
      "查看详情",
      "去使用",
      "点击立即下载",
      "戳我下载",
      "立即购买",
      "立即打开",
    )
    private val MEDIUM_AD_MARKERS = listOf(
      "立即打开",
      "立即下载",
      "点击跳转",
      "详情页面",
      "免费领取",
      "立即办理",
      "办理",
      "福利",
      "优惠",
      "促销",
      "特惠",
      "红包",
      "svip",
    )
    private val COMMERCIAL_AD_MARKERS = listOf(
      "vip",
      "会员",
      "年卡",
      "权益",
      "特权",
      "商城",
      "折扣",
      "促销",
      "特惠",
      "优惠",
      "优惠券",
      "礼包",
      "福利",
      "惊喜",
      "限时",
      "补贴",
      "立减",
      "满减",
      "抽奖",
      "红包",
      "券",
      "开通",
      "充值",
      "流量",
      "云盘",
      "套餐",
      "5g",
      "gb",
      "tb",
      "立即购买",
      "购买",
    )
    private val POPUP_MARKERS = listOf(
      "落地页",
      "第三方app",
      "第三方应用",
      "弹窗",
      "dialog",
      "popup",
      "modal",
      "interstitial",
    )
    private val SKIP_RESOURCE_MARKERS = listOf(
      "skip",
      "jump",
      "countdown",
      "ad_skip",
      "skip_ad",
      "skipad",
    )
    private val AD_CLOSE_RESOURCE_MARKERS = listOf(
      "ad_close",
      "close_ad",
      "closebtn_ad",
      "adclose",
      "interstitial_close",
      "interstitialclose",
      "splash_close",
    )
    private val GENERIC_CLOSE_RESOURCE_MARKERS = listOf(
      "close",
      "dismiss",
      "guanbi",
    )

    fun isEnabled(context: Context): Boolean {
      val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
      ) ?: return false
      val component = ComponentName(context, ClearScreenAccessibilityService::class.java)
      val fullName = component.flattenToString()
      val shortName = component.flattenToShortString()
      return enabled.split(':').any { rawName ->
        val name = rawName.trim()
        if (name.equals(fullName, ignoreCase = true) ||
          name.equals(shortName, ignoreCase = true)
        ) {
          true
        } else {
          // vivo may persist the short form. Let Android expand it before
          // comparing so the app and system agree on the same service.
          ComponentName.unflattenFromString(name)?.let { savedComponent ->
            savedComponent.packageName.equals(component.packageName, ignoreCase = true) &&
              savedComponent.className.equals(component.className, ignoreCase = true)
          } == true
        }
      }
    }

    private fun readNodeLabel(node: AccessibilityNodeInfo): String =
      listOfNotNull(
        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() },
      ).distinct().joinToString(" ")

    private fun normalizeLabel(value: String): String = value
      .lowercase(Locale.ROOT)
      .replace("\u3000", "")
      .replace("[\\s\\-_:：，,。.!！？?()（）\\[\\]{}<>《》【】'\"`~·|/\\\\]".toRegex(), "")

    private fun isStrongSkipLabel(compact: String): Boolean =
      compact in STRONG_SKIP_LABELS ||
        (compact.startsWith("skipad") && compact.length <= 20) ||
        (compact.startsWith("跳过") && compact.length <= 10 &&
          (compact.contains("广告") || compact.contains("视频")))

    private fun isCountdownSkipLabel(compact: String): Boolean =
      COUNTDOWN_SKIP_PATTERN.matches(compact)

    private fun hasSkipResourceId(resourceId: String): Boolean =
      SKIP_RESOURCE_MARKERS.any(resourceId::contains)

    private fun hasAdCloseResourceId(resourceId: String): Boolean =
      AD_CLOSE_RESOURCE_MARKERS.any(resourceId::contains)

    private fun isPopupCloseLabel(compact: String): Boolean =
      compact in POPUP_CLOSE_LABELS ||
        (compact.startsWith("关闭") && compact.length <= 8) ||
        (compact.startsWith("close") && compact.length <= 12)

    private fun isAdSpecificCloseLabel(compact: String): Boolean =
      compact == "关闭广告" ||
        compact == "closead" ||
        compact.contains("advertisement")

    private fun isGenericPopupCloseLabel(compact: String): Boolean =
      compact == "关闭" ||
        compact == "close" ||
        compact == "dismiss" ||
        compact in setOf("×", "✕", "✖", "✗", "╳", "⨯", "x")

    private fun hasStrongAdSignal(label: String): Boolean =
      hasStrongAdSignal(label, "")

    private fun hasStrongAdSignal(label: String, resourceId: String): Boolean {
      val compact = scrubNegatedAdMarkers(normalizeLabel(label))
      return STRONG_AD_MARKERS.any(compact::contains) ||
        hasAdResourceId(resourceId)
    }

    private fun hasAdResourceId(resourceId: String): Boolean {
      if (resourceId.isBlank()) return false
      val tokens = resourceId.lowercase(Locale.ROOT).split(Regex("[^a-z0-9]+"))
      return tokens.any { token ->
        token in setOf("ad", "ads", "adview", "adbanner", "adslot", "advertisement", "interstitial", "splashad", "splash")
      }
    }

    private fun scrubNegatedAdMarkers(compactLabel: String): String =
      NEGATED_AD_MARKERS.fold(compactLabel) { text, marker -> text.replace(marker, "") }

    private fun isOverlayLayer(node: ScreenNode): Boolean =
      isOverlayLayer(node.compactLabel, node.resourceId, node.className)

    private fun isOverlayLayer(region: AncestorRegion): Boolean =
      isOverlayLayer(region.compactLabel, region.resourceId, region.className)

    private fun isOverlayLayer(compactLabel: String, resourceId: String, className: String): Boolean {
      if (SCROLL_CONTAINER_CLASSES.any(className::contains)) return false
      val adResourceId = hasAdResourceId(resourceId) ||
        resourceId.contains("overlay") ||
        resourceId.contains("popup") ||
        resourceId.contains("dialog")
      if (adResourceId) return true
      val overlayClassName = className.contains("popup") ||
        className.contains("dialog") ||
        className.contains("overlay") ||
        className.contains("float") ||
        className.contains("interstitial") ||
        className.contains("splash")
      return overlayClassName ||
        hasPopupSignal(compactLabel, resourceId, className)
    }

    private fun isLargeOverlayBounds(bounds: Rect, rootBounds: Rect): Boolean {
      val rootArea = rootBounds.width().toLong().coerceAtLeast(1L) *
        rootBounds.height().toLong().coerceAtLeast(1L)
      val area = bounds.width().toLong().coerceAtLeast(0L) *
        bounds.height().toLong().coerceAtLeast(0L)
      return hasUsableBounds(bounds) &&
        area >= rootArea * LARGE_OVERLAY_MIN_AREA_RATIO &&
        bounds.width() >= rootBounds.width() * LARGE_OVERLAY_MIN_WIDTH_RATIO &&
        bounds.height() >= rootBounds.height() * LARGE_OVERLAY_MIN_HEIGHT_RATIO
    }

    private fun hasVideoAdSignal(label: String): Boolean {
      val compact = normalizeLabel(label)
      return VIDEO_AD_CONTEXT_MARKERS.any(compact::contains) ||
        (compact.contains("preload") && STRONG_AD_MARKERS.any(compact::contains))
    }

    private fun isSkipOcrLabel(compact: String): Boolean =
      isStrongSkipLabel(compact) || isCountdownSkipLabel(compact) ||
        compact == "跳过" || compact == "skip"


    private fun isDynamicAdEvent(eventType: Int): Boolean =
      eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
        eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
        eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
        eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED

    private fun hasMediumAdSignal(compactLabel: String): Boolean =
      MEDIUM_AD_MARKERS.any(compactLabel::contains)

    private fun hasCommercialAdSignal(compactLabel: String): Boolean =
      COMMERCIAL_AD_MARKERS.any(compactLabel::contains)

    private fun countMatchingMarkers(compactLabel: String, markers: List<String>): Int =
      markers.count(compactLabel::contains)

    private fun isLikelyUnlabeledEdgeClose(
      node: ScreenNode,
      target: AccessibilityNodeInfo,
      targetResourceId: String,
      nearPopupEdge: Boolean,
      compactControl: Boolean,
      ocrCloseGlyph: Boolean,
    ): Boolean {
      if (node.label.isNotBlank() || !nearPopupEdge || !compactControl) return false
      val className = (node.className + " " + target.className?.toString().orEmpty())
        .lowercase(Locale.ROOT)
      val closeResource = hasGenericCloseResourceId(node.resourceId) ||
        hasGenericCloseResourceId(targetResourceId)
      val iconClass = className.contains("imageview") ||
        className.contains("imagebutton") ||
        className.endsWith("button")
      return AdDecisionPolicy.allowUnlabeledClose(closeResource, iconClass && ocrCloseGlyph)
    }

    private fun hasGenericCloseResourceId(resourceId: String): Boolean =
      GENERIC_CLOSE_RESOURCE_MARKERS.any(resourceId::contains)

    private fun hasPopupSignal(
      compactLabel: String,
      resourceId: String,
      className: String,
    ): Boolean =
      POPUP_MARKERS.any(compactLabel::contains) ||
        POPUP_MARKERS.any(resourceId::contains) ||
        POPUP_MARKERS.any(className::contains)

    private fun hasUsableBounds(bounds: Rect): Boolean =
      bounds.width() > 0 && bounds.height() > 0

    private fun isNearTopRight(bounds: Rect, rootBounds: Rect): Boolean {
      val width = rootBounds.width().coerceAtLeast(1)
      val height = rootBounds.height().coerceAtLeast(1)
      return bounds.centerX() >= rootBounds.left + width * 0.55f &&
        bounds.top <= rootBounds.top + height * 0.48f &&
        bounds.right >= rootBounds.left + width * 0.70f
    }

    /**
     * Strict top-edge variant used as the sole positional fallback for weak
     * signals (resource-id only / unlabeled icon). Ad skip and close controls sit
     * on the very top edge, while ordinary page entries can be 40% down and still
     * match the looser [isNearTopRight] geometry.
     */
    private fun isVeryNearTopRight(bounds: Rect, rootBounds: Rect): Boolean {
      val width = rootBounds.width().coerceAtLeast(1)
      val height = rootBounds.height().coerceAtLeast(1)
      return bounds.centerX() >= rootBounds.left + width * 0.60f &&
        bounds.top <= rootBounds.top + height * 0.30f &&
        bounds.right >= rootBounds.left + width * 0.75f
    }

    private fun isNearPopupEdge(bounds: Rect, rootBounds: Rect): Boolean {
      val width = rootBounds.width().coerceAtLeast(1)
      val height = rootBounds.height().coerceAtLeast(1)
      val centerX = bounds.centerX()
      val centerY = bounds.centerY()
      val nearBottomCenter = centerY >= rootBounds.top + height * 0.62f &&
        centerX >= rootBounds.left + width * 0.28f &&
        centerX <= rootBounds.left + width * 0.72f
      val nearSide = (centerX <= rootBounds.left + width * 0.18f ||
        centerX >= rootBounds.left + width * 0.82f) &&
        centerY >= rootBounds.top + height * 0.08f &&
        centerY <= rootBounds.top + height * 0.92f
      return isNearTopRight(bounds, rootBounds) || nearBottomCenter || nearSide
    }

    private fun isCompactControl(bounds: Rect, rootBounds: Rect): Boolean {
      val width = rootBounds.width().coerceAtLeast(1)
      val height = rootBounds.height().coerceAtLeast(1)
      return bounds.width() <= width * 0.36f && bounds.height() <= height * 0.22f
    }

    private fun hasCompactSkipTarget(targetBounds: Rect, labelBounds: Rect, rootBounds: Rect): Boolean {
      if (!targetBounds.contains(labelBounds.centerX(), labelBounds.centerY())) return false
      val rootArea = rootBounds.width().toLong().coerceAtLeast(1L) *
        rootBounds.height().toLong().coerceAtLeast(1L)
      val targetArea = targetBounds.width().toLong() * targetBounds.height().toLong()
      return targetBounds.width() <= rootBounds.width() * MAX_SKIP_TARGET_WIDTH_RATIO &&
        targetBounds.height() <= rootBounds.height() * MAX_SKIP_TARGET_HEIGHT_RATIO &&
        targetArea.toDouble() <= rootArea * MAX_SKIP_TARGET_AREA_RATIO
    }

    private fun isCompactTopCenterSkipTarget(
      targetBounds: Rect,
      labelBounds: Rect,
      rootBounds: Rect,
    ): Boolean {
      val width = rootBounds.width().coerceAtLeast(1)
      val height = rootBounds.height().coerceAtLeast(1)
      if (!targetBounds.contains(labelBounds.centerX(), labelBounds.centerY())) return false
      val labelCenterRatio = (labelBounds.centerX() - rootBounds.left).toFloat() / width
      val targetArea = targetBounds.width().toLong() * targetBounds.height().toLong()
      val rootArea = width.toLong() * height.toLong()
      return labelCenterRatio in 0.34f..0.66f &&
        targetBounds.top <= rootBounds.top + height * TOP_CENTER_SKIP_CONTEXT_TOP_RATIO &&
        targetBounds.width() <= width * TOP_CENTER_SKIP_MAX_WIDTH_RATIO &&
        targetBounds.height() <= height * TOP_CENTER_SKIP_MAX_HEIGHT_RATIO &&
        targetArea.toDouble() <= rootArea * TOP_CENTER_SKIP_MAX_AREA_RATIO
    }
  }
}
