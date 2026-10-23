package com.clearscreen.prototype.backend

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.clearscreen.prototype.BuildConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.util.ArrayDeque
import java.io.FileDescriptor
import java.io.PrintWriter
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
  private val privacyConsentStore by lazy { ClearScreenPrivacyConsentStore(this) }
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
  private val actionAttempts = AdActionAttemptGuard()
  private val visualAttempts = AdVisualAttemptGuard()
  private val adScans = AdScanCoordinator()
  private val eventScans = AdEventCoalescer()
  private var eventScanPosted = false
  private var lastSnapshot: AccessibilitySnapshot? = null
  private var lastSnapshotAt = 0L
  private var lastScanReason = "not-scanned"
  private var lastScanDuration = 0L
  private var scanCount = 0L
  private var lastScanLogAt = 0L
  private val eventScanTick = Runnable {
    eventScanPosted = false
    eventScans.take(SystemClock.uptimeMillis())?.let { processAccessibilityEvent(it) }
  }
  private var mediaWatch: MediaWatch? = null
  private var observedPlayerPackage: String? = null
  private var observedPlayerVisible = false
  private var lastMediaOcrAt = 0L
  private val mediaWatchTick = object : Runnable {
    override fun run() = pollMediaWatch()
  }
  private var lastActionAt = 0L
  private var lastOcrRequestAt = 0L
  private var lastWifiOcrDiagnosticAt = 0L
  // Bounded stage summaries for every opted-in host; no nodes, images or OCR text.
  private data class WifiDiagnostic(val at: Long, val summary: String)
  private val wifiDiagnostics = linkedMapOf<String, WifiDiagnostic>()
  private val controlRejections = linkedMapOf<String, Int>()
  private var observedForegroundPackage: String? = null
  private var foregroundEntryAt = 0L
  private var visualConfirmationPending = false
  private var visualGeneration = 0L
  private var pendingVisualAction: PendingVisualAction? = null
  private var calendarWindowClass = ""
  private var calendarWindowId = -1
  private var ocrScreenshotPending = false
  private var ocrSignatureRetryPending = false
  private var serviceDestroyed = false
  private var privacyPaused = false
  private val permissionProfile by lazy { ClearScreenOemProfile.current() }
  private var settingsPermissionPackage = ""
  private var settingsPermissionWindow = -1
  private val settingsPermissionScan = Runnable {
    if (serviceDestroyed || !privacyConsentStore.hasCurrentConsent()) {
      ClearScreenSettingsObserver.clear()
    } else if (ClearScreenSettingsObserver.isWatching(settingsPermissionPackage)) {
      val root = rootInActiveWindow
      try {
        if (root != null && root.packageName?.toString() == settingsPermissionPackage &&
          root.windowId == settingsPermissionWindow) {
          ClearScreenSettingsObserver.observe(root)
        } else ClearScreenSettingsObserver.rejectSample()
      } finally {
        @Suppress("DEPRECATION")
        root?.recycle()
      }
    }
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    serviceDestroyed = false
    running = true
    Log.i(
      TAG,
      "onServiceConnected component=${ComponentName(this, ClearScreenAccessibilityService::class.java).flattenToString()} " +
        "pid=${Process.myPid()}",
    )
    if (ensureConsentAndStore()) {
      handler.postDelayed({ scanCurrentForegroundPackage() }, INITIAL_FOREGROUND_SCAN_DELAY_MS)
    }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null || !ensureConsentAndStore()) return
    val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      ClearScreenSettingsObserver.onForegroundWindow(packageName)
    }
    if (packageName in permissionProfile.readableSettingsPackages) {
      // Settings remain protected from ALL ad actions. The separate opt-in
      // path reads only our own, unambiguously labelled checkable controls.
      if (ClearScreenSettingsObserver.isWatching(packageName) && event.eventType in setOf(
          AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
          AccessibilityEvent.TYPE_VIEW_CLICKED, AccessibilityEvent.TYPE_VIEW_SCROLLED,
        )) {
        settingsPermissionPackage = packageName
        settingsPermissionWindow = event.windowId
        handler.removeCallbacks(settingsPermissionScan)
        handler.postDelayed(settingsPermissionScan, 80L)
        handler.postDelayed(settingsPermissionScan, 240L)
      }
      return
    }
    if (!isDynamicAdEvent(event.eventType) || packageName == applicationContext.packageName ||
      AdDecisionPolicy.isProtectedPackage(packageName) || ProtectedUiPolicy.isProtectedPackage(packageName)
    ) return
    if (packageName == WifiPromotionRules.CALENDAR_PACKAGE_NAME &&
      event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    ) {
      calendarWindowClass = event.className?.toString().orEmpty()
      calendarWindowId = event.windowId
    }
    eventScans.offer(packageName, event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    val urgentMobileTransition = packageName == MOBILE_TRAFFIC_CARD_PACKAGE &&
      event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    if (urgentMobileTransition) {
      handler.removeCallbacks(eventScanTick)
      eventScanPosted = true
      handler.post(eventScanTick)
    } else if (!eventScanPosted) {
      eventScanPosted = true
      handler.postDelayed(eventScanTick, eventScans.delay(SystemClock.uptimeMillis()))
    }
  }

  private fun processAccessibilityEvent(event: AdEventCoalescer.Event) {
    if (!ensureConsentAndStore()) return
    if (!store.isMasterEnabled()) {
      adScans.cancel()
      stopMediaWatch()
      return
    }
    val packageName = event.packageName
    if (mediaWatch != null && !isCurrentForegroundPackage(mediaWatch!!.packageName)) {
      stopMediaWatch()
      observedPlayerVisible = false
    }
    if (!isEligibleForegroundPackage(packageName)) {
      if (isCurrentForegroundPackage(packageName)) adScans.cancel()
      return
    }

    val rule = store.getAppRule(packageName)
    if (!rule.skipEnabled || rule.whitelist) {
      adScans.cancel()
      return
    }

    val now = SystemClock.uptimeMillis()
    observeForeground(packageName)
    if (now - lastActionAt < ACTION_COOLDOWN_MS) return

    val handled = tryHandlePackage(packageName)
    if (handled) {
      adScans.cancel()
    } else {
      scheduleDynamicAdRetries(packageName, event.windowChanged)
      requestOcrScan(
        packageName,
        prioritizeTransientSplash = event.windowChanged && packageName == MOBILE_TRAFFIC_CARD_PACKAGE,
      )
      maybeStartMediaWatch(packageName, if (event.windowChanged) AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED else
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
    }
  }

  override fun onInterrupt() = Unit

  override fun onUnbind(intent: Intent?): Boolean {
    handler.removeCallbacks(settingsPermissionScan)
    ClearScreenSettingsObserver.clear()
    handler.removeCallbacks(eventScanTick)
    eventScans.clear()
    eventScanPosted = false
    lastSnapshot = null
    wifiDiagnostics.clear()
    clearVisualState()
    calendarWindowClass = ""
    calendarWindowId = -1
    adScans.cancel()
    stopMediaWatch()
    observedPlayerVisible = false
    running = false
    Log.w(TAG, "onUnbind pid=${Process.myPid()}")
    return super.onUnbind(intent)
  }

  /** Standard Android shell diagnostics; screen text is opt-in and never written by the app. */
  override fun dump(fd: FileDescriptor, writer: PrintWriter, args: Array<out String>) {
    writer.println("running=$running consent=${privacyConsentStore.hasCurrentConsent()} scans=$scanCount reason=$lastScanReason scanMs=$lastScanDuration")
    writer.flush()
    if (!privacyConsentStore.hasCurrentConsent() || !::store.isInitialized) return
    writer.println("master=${store.isMasterEnabled()} screenshotPending=$ocrScreenshotPending")
    wifiDiagnostics.forEach { (stage, diagnostic) ->
      writer.println("$stage ageMs=${SystemClock.uptimeMillis() - diagnostic.at} ${diagnostic.summary}")
    }
    val snapshot = lastSnapshot ?: run { writer.println("snapshot=unavailable"); return }
    val packageName = snapshot.packageName
    val rule = store.getAppRule(packageName)
    writer.println("snapshotPackage=$packageName window=${snapshot.windowId} skip=${rule.skipEnabled} excluded=${rule.whitelist} ageMs=${SystemClock.uptimeMillis() - lastSnapshotAt}")
    if (!args.contains("--ad-tree") || packageName != "com.nowcasting.activity") return
    writer.println("nodes=${snapshot.nodes.size} visible=${snapshot.nodes.count { it.isVisible }} signature=${snapshot.signature}")
    snapshot.nodes.filter { it.isVisible }.forEach { node ->
      writer.println("${node.path} id=${node.resourceId} class=${node.className} bounds=${node.bounds} click=${node.node.isClickable} scroll=${node.isScrollable} enabled=${node.isEnabled} label=${node.label.replace('\n', ' ').take(180)}")
    }
  }

  override fun onDestroy() {
    serviceDestroyed = true
    ClearScreenSettingsObserver.clear()
    Log.w(TAG, "onDestroy pid=${Process.myPid()}")
    handler.removeCallbacksAndMessages(null)
    eventScans.clear()
    lastSnapshot = null
    wifiDiagnostics.clear()
    clearVisualState()
    calendarWindowClass = ""
    calendarWindowId = -1
    actionAttempts.clear()
    adScans.cancel()
    stopMediaWatch()
    observedPlayerVisible = false
    if (textRecognizerDelegate.isInitialized()) runCatching { textRecognizer.close() }
    // Do not shut down ocrExecutor here: pending ML Kit task listeners are
    // dispatched on it, and rejecting those callbacks can crash the service.
    running = false
    super.onDestroy()
  }

  private fun scanCurrentForegroundPackage() {
    if (serviceDestroyed || !ensureConsentAndStore() || !store.isMasterEnabled()) return
    val packageName = rootInActiveWindow?.packageName?.toString()
      ?.takeIf(::isEligibleForegroundPackage)
      ?: return
    val rule = store.getAppRule(packageName)
    if (!rule.skipEnabled || rule.whitelist) return
    observeForeground(packageName)
    if (!tryHandlePackage(packageName)) {
      scheduleDynamicAdRetries(packageName, waitForLateControls = true)
      requestOcrScan(packageName, prioritizeTransientSplash = packageName == MOBILE_TRAFFIC_CARD_PACKAGE)
      maybeStartMediaWatch(packageName, AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    }
  }

  private fun tryHandlePackage(
    packageName: String,
    ocrFrame: OcrFrame? = null,
    followUpDepth: Int = 0,
  ): Boolean {
    val startedAt = SystemClock.uptimeMillis()
    scanCount++
    lastScanReason = "checking"
    return try { tryHandlePackageInternal(packageName, ocrFrame, followUpDepth) } finally {
      lastScanDuration = SystemClock.uptimeMillis() - startedAt
      recordWifiDiagnostic(packageName, "scan",
        "reason=$lastScanReason ocr=${ocrFrame != null} nodes=${lastSnapshot?.takeIf { it.packageName == packageName }?.nodes?.size ?: 0}")
      if (startedAt - lastScanLogAt >= 3_000L) {
        lastScanLogAt = startedAt
        Log.d(TAG, "scan package=$packageName reason=$lastScanReason nodes=${lastSnapshot?.nodes?.size ?: 0} ms=$lastScanDuration")
      }
    }
  }

  private fun rejectScan(reason: String): Boolean { lastScanReason = reason; return false }

  private fun observeForeground(packageName: String) {
    if (observedForegroundPackage == packageName) return
    clearVisualState()
    observedForegroundPackage = packageName
    foregroundEntryAt = SystemClock.uptimeMillis()
  }

  private fun clearVisualState() {
    visualGeneration++
    visualAttempts.clear()
    visualConfirmationPending = false
    pendingVisualAction = null
    observedForegroundPackage = null
    foregroundEntryAt = 0L
    controlRejections.clear()
  }

  private fun rejectControl(reason: String) {
    controlRejections[reason] = (controlRejections[reason] ?: 0) + 1
  }

  private fun recordWifiDiagnostic(packageName: String, stage: String, summary: String) {
    val prefix = when (packageName) {
      WifiPromotionRules.PACKAGE_NAME -> "wifi"
      WifiPromotionRules.CALENDAR_PACKAGE_NAME -> "calendar"
      else -> "ad." + packageName
    }
    if (!serviceDestroyed &&
      privacyConsentStore.hasCurrentConsent()
    ) {
      val key = "$prefix.$stage"
      wifiDiagnostics.remove(key)
      wifiDiagnostics[key] = WifiDiagnostic(SystemClock.uptimeMillis(), summary)
      while (wifiDiagnostics.size > 80) wifiDiagnostics.remove(wifiDiagnostics.keys.first())
    }
  }

  private fun isUnsafeCaiyunRedirectClickNode(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
  ): Boolean {
    if (snapshot.packageName != KNOWN_CAIYUN_INTERSTITIAL_PACKAGE) return false
    val resourceIds = mutableListOf(node.resourceId)
    node.ancestorRegions.mapTo(resourceIds) { it.resourceId }
    clickableTarget(node.node)?.viewIdResourceName?.let(resourceIds::add)
    return resourceIds.any { resourceId ->
      AdCreativeRules.isUnsafeCaiyunInterstitialClickId(snapshot.packageName, resourceId)
    }
  }

  private fun isUnsafeCaiyunRedirectClickTarget(
    target: AccessibilityNodeInfo,
    packageName: String,
  ): Boolean = AdCreativeRules.isUnsafeCaiyunInterstitialClickId(
    packageName, target.viewIdResourceName.orEmpty())

  private fun tryHandlePackageInternal(packageName: String, ocrFrame: OcrFrame?, followUpDepth: Int): Boolean {
    if (serviceDestroyed || !ensureConsentAndStore() || !store.isMasterEnabled()) return rejectScan("disabled")
    if (!isEligibleForegroundPackage(packageName)) return rejectScan("foreground-changed")
    val rule = store.getAppRule(packageName)
    if (!rule.skipEnabled || rule.whitelist) return rejectScan("app-excluded")
    pendingVisualAction?.takeIf { it.packageName == packageName }?.let {
      if (SystemClock.uptimeMillis() - it.at <= 4_000L) return rejectScan("visual-action-pending")
      pendingVisualAction = null
      recordWifiDiagnostic(packageName, "result", "mode=visual result=unconfirmed reason=verification-expired")
    }

    val now = SystemClock.uptimeMillis()
    if (now - lastActionAt < ACTION_COOLDOWN_MS) return rejectScan("cooldown")
    val root = findRootForPackage(packageName) ?: return rejectScan("root-unavailable")
    val snapshot = AccessibilitySnapshot.capture(root)
    lastSnapshot = snapshot
    lastSnapshotAt = SystemClock.uptimeMillis()
    if (isProtectedTimeSensitiveSnapshot(root, snapshot)) return rejectScan("protected-screen")
    var action = findAdAction(root, ocrFrame, snapshot) ?: run {
      recordWifiDiagnostic(packageName, "controls", "rejections=" + controlRejections.entries.joinToString(",") { it.key + ":" + it.value })
      return rejectScan("no-candidate")
    }
    if (isUnsafeCaiyunRedirectClickTarget(action.target, packageName)) {
      return rejectScan("unsafe-caiyun-redirect-target")
    }
    if (!action.target.isVisibleToUser || !action.target.isEnabled || !action.target.isClickable ||
      action.target.packageName?.toString() != packageName
    ) return rejectScan("target-unavailable")
    // Re-check the live foreground window before acting on a node collected
    // earlier: a timer, alarm or reminder can interrupt an advertisement.
    val liveRoot = findRootForPackage(packageName) ?: return false
    val liveSnapshot = AccessibilitySnapshot.capture(liveRoot)
    if (isProtectedTimeSensitiveSnapshot(liveRoot, liveSnapshot)) return rejectScan("protected-screen")
    if (action.ruleId == UNIFIED_CONTROL_RULE &&
      (ocrFrame == null || !isOcrScopeCurrent(ocrFrame, liveSnapshot, action.touchEvidenceScope))
    ) return rejectScan("control-evidence-stale")
    if (liveSnapshot.signature != action.screenSignature) {
      val refreshed = findAdAction(liveRoot, ocrFrame, liveSnapshot) ?: return rejectScan("screen-changed")
      if (refreshed.target != action.target || refreshed.targetBounds != action.targetBounds ||
        refreshed.category != action.category || refreshed.ruleId != action.ruleId
      ) return rejectScan("target-changed")
      action = refreshed
    }
    if (!action.target.refresh()) return rejectScan("target-unavailable")
    val refreshedBounds = Rect().also(action.target::getBoundsInScreen)
    if (!action.target.isVisibleToUser || !action.target.isEnabled || !action.target.isClickable ||
      action.target.packageName?.toString() != packageName || action.target.windowId != action.targetWindowId ||
      action.target.viewIdResourceName.orEmpty() != action.targetResourceId ||
      action.target.className?.toString().orEmpty() != action.targetClassName || refreshedBounds != action.targetBounds ||
      readNodeLabels(action.target).map {
        AdSemanticRules.ocrEvidenceLabel(it, action.category == DetectionCategory.SPLASH_VIDEO)
      } != action.targetLabels ||
      isUnsafeCaiyunRedirectClickTarget(action.target, packageName)
    ) return rejectScan("target-changed")
    if (!isAppProcessingEnabled(packageName) || !isEligibleForegroundPackage(packageName)) return false
    val fingerprint = action.targetFingerprint(packageName)
    if (!actionAttempts.tryBegin(fingerprint, now)) {
      if (action.allowVerifiedTouchFallback && ocrFrame != null) {
        return tryDispatchVerifiedControlTouch(packageName, action, ocrFrame, followUpDepth)
      }
      return rejectScan("repeat-guard")
    }
    lastActionAt = now
    visualGeneration++
    visualAttempts.clear()
    visualConfirmationPending = false
    // Only click a deliberately identified control. ACTION_DISMISS is exposed by
    // many ordinary dialogs and would close user-required popups even when no ad
    // close button was found.
    val clicked = action.target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    lastScanReason = if (clicked) "click-accepted" else "click-rejected"
    recordWifiDiagnostic(packageName, "action",
      "mode=node-click window=${root.windowId} accepted=$clicked touchEvidence=${action.allowVerifiedTouchFallback} rule=${action.ruleId}")
    Log.i(TAG, "ad-action package=$packageName rule=${action.ruleId} window=${root.windowId} bounds=${action.targetBounds} accepted=$clicked")

    if (BuildConfig.DEBUG) {
      Log.d(
        TAG,
        "action=${action.ruleId} score=${action.score} package=$packageName " +
          "label=${action.label} clicked=$clicked",
      )
    }

    if (!clicked) {
      actionAttempts.markIneffective(fingerprint)
      appendActionEvent(packageName, action, "failed", "处理失败")
      return false
    }

    scheduleActionVerification(packageName, action, ocrFrame, fingerprint, followUpDepth,
      primaryClick = true)
    return true
  }

  /** Touch only the freshly re-identified accessible control, never an OCR-only location. */
  private fun tryDispatchVerifiedControlTouch(
    packageName: String,
    previousAction: DetectionAction,
    frame: OcrFrame,
    followUpDepth: Int,
  ): Boolean {
    val root = rootInActiveWindow ?: return rejectScan("touch-root-unavailable")
    if (!isAppProcessingEnabled(packageName) || !isEligibleForegroundPackage(packageName) ||
      root.packageName?.toString() != packageName || root.windowId != previousAction.targetWindowId ||
      screenshotDisplayBounds() != frame.displayBounds
    ) return rejectScan("touch-window-changed")
    val snapshot = AccessibilitySnapshot.capture(root)
    if (!isOcrScopeCurrent(frame, snapshot, previousAction.touchEvidenceScope) ||
      snapshot.nodes.any { it.isVisible && (it.isPassword || (it.isEditable && it.isFocused)) }
    ) return rejectScan("touch-evidence-stale")
    val action = findAdAction(root, frame, snapshot) ?: return rejectScan("touch-candidate-changed")
    val fingerprint = action.targetFingerprint(packageName)
    if (!action.allowVerifiedTouchFallback || action.target != previousAction.target ||
      fingerprint != previousAction.targetFingerprint(packageName) ||
      !frame.displayBounds.contains(action.targetBounds)
    ) return rejectScan("touch-target-changed")
    val now = SystemClock.uptimeMillis()
    if (!actionAttempts.tryBeginTouchFallback(fingerprint, now)) return rejectScan("repeat-guard")

    // Some SDK controls accept ACTION_CLICK but only dismiss on a touch event.
    // This fallback has a separate one-shot budget and is a short tap, not a swipe/long press.
    lastActionAt = now
    val path = Path().apply {
      moveTo(action.targetBounds.exactCenterX(), action.targetBounds.exactCenterY())
      lineTo(action.targetBounds.exactCenterX() + 1f, action.targetBounds.exactCenterY())
    }
    val gesture = GestureDescription.Builder()
      .addStroke(GestureDescription.StrokeDescription(path, 0L, OCR_SKIP_GESTURE_DURATION_MS))
      .build()
    val dispatched = runCatching {
      dispatchGesture(gesture, object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
          if (!ensureConsentAndStore() || !isAppProcessingEnabled(packageName)) return
          scheduleActionVerification(packageName, action, frame, fingerprint, followUpDepth,
            primaryClick = false)
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
          if (!ensureConsentAndStore() || !isAppProcessingEnabled(packageName)) return
          recordWifiDiagnostic(packageName, "result", "mode=verified-touch result=cancelled")
          appendActionEvent(packageName, action, "failed", "控件触摸未完成")
        }
      }, null)
    }.getOrDefault(false)
    lastScanReason = if (dispatched) "verified-touch-dispatched" else "verified-touch-rejected"
    recordWifiDiagnostic(packageName, "action",
      "mode=verified-touch window=${root.windowId} accepted=$dispatched rule=${action.ruleId}")
    Log.i(TAG, "ad-action package=$packageName rule=${action.ruleId} window=${root.windowId} " +
      "bounds=${action.targetBounds} mode=verified-touch-fallback accepted=$dispatched")
    if (!dispatched) appendActionEvent(packageName, action, "failed", "控件触摸未能发送")
    return dispatched
  }

  private fun scheduleActionVerification(
    packageName: String,
    action: DetectionAction,
    ocrFrame: OcrFrame?,
    fingerprint: String,
    followUpDepth: Int,
    primaryClick: Boolean,
  ) {
    // performAction(true) only means that Android accepted the request. Verify
    // the same target shortly afterwards so a stale/non-operational node does
    // not inflate the success counter.
    handler.postDelayed({
      if (!ensureConsentAndStore() || !store.isMasterEnabled()) return@postDelayed
      if (!isEligibleForegroundPackage(packageName)) {
        appendActionEvent(packageName, action, "failed", "前台已切换，关闭结果未确认")
        return@postDelayed
      }
      val currentRoot = findRootForPackage(packageName)
      if (currentRoot == null) {
        appendActionEvent(packageName, action, "failed", "页面不可读取，关闭结果未确认")
        return@postDelayed
      }
      if (currentRoot.windowId != action.targetWindowId) {
        recordWifiDiagnostic(packageName, "result", "result=unconfirmed reason=window-changed")
        appendActionEvent(packageName, action, "pending", "窗口已变化，关闭结果未确认")
        return@postDelayed
      }
      val currentSnapshot = AccessibilitySnapshot.capture(currentRoot)
      val currentAction = findAdAction(currentRoot, ocrFrame, currentSnapshot)
      // OCR evidence expires quickly. Its absence is not proof that the target
      // disappeared: refresh the actual node as well before counting success.
      val targetStillVisible = runCatching {
        action.target.refresh() && action.target.isVisibleToUser &&
          action.target.packageName?.toString() == packageName &&
          action.target.windowId == currentRoot.windowId
      }.getOrDefault(false)
      // SDKs can replace an accessible node without removing its visual control.
      // A new node at the same control identity is not proof that the ad closed.
      val replacementStillVisible = currentSnapshot.nodes.any { node ->
        node.isVisible && node.node.isClickable && node.bounds == action.targetBounds &&
          node.resourceId == action.targetResourceId.lowercase(Locale.ROOT) &&
          node.className == action.targetClassName.lowercase(Locale.ROOT)
      }
      val stillPresent = targetStillVisible || replacementStillVisible ||
        currentAction?.targetFingerprint(packageName) == fingerprint
      recordWifiDiagnostic(packageName, "result",
        "mode=${if (primaryClick) "node-click" else "verified-touch"} window=${currentRoot.windowId} targetStillPresent=$stillPresent")
      if (stillPresent) {
        // Failure belongs to the attempted control, not to the OCR evidence that
        // happened to exist at dispatch. Fresh OCR may authorize the fallback later.
        actionAttempts.markIneffective(fingerprint)
        appendActionEvent(packageName, action, "failed", "动作未生效")
      } else if (action.ruleId == UNIFIED_CONTROL_RULE && currentSnapshot.nodes.any { peer ->
          peer.isVisible && (peer.path == action.touchEvidenceScope ||
            AdDecisionPolicy.isDescendantOf(peer.path, action.touchEvidenceScope)) &&
            (AdControlEvidenceRules.isAdContainer(peer.resourceId) ||
              peer.controlLabels.any(AdControlEvidenceRules::isAdBadge) ||
              (isVisualCreativeNode(peer) && ocrFrame?.nodeStates?.get(peer.path)?.bounds == peer.bounds.toBox()))
        }) {
        appendActionEvent(packageName, action, "pending", "关闭结果未确认")
      } else {
        appendActionEvent(packageName, action, "success", action.successText)
      }
      // Popups can expose an end card or another inline ad without a new event.
      // A stuck control stays reserved. Only a freshly OCR-verified control may
      // consume its single touch fallback on a later scan.
      if (followUpDepth < MAX_ACTION_FOLLOW_UPS) {
        val currentRootBounds = Rect().also(currentRoot::getBoundsInScreen)
        val needsFreshSkipFrame = stillPresent && primaryClick && (
          action.allowVerifiedTouchFallback ||
            (action.category == DetectionCategory.SPLASH_VIDEO &&
              AdGeometryRules.isBottomLeftSplashSkip(action.targetBounds.toBox(), currentRootBounds.toBox()))
          )
        val followUpDelay = if (needsFreshSkipFrame) {
          // Do not spend another fixed delay rescanning without the OCR needed
          // for the fallback; request a fresh frame as soon as the throttle permits.
          (OCR_MIN_INTERVAL_MS - (System.currentTimeMillis() - lastOcrRequestAt)).coerceAtLeast(0L)
        } else POST_ACTION_RESCAN_DELAY_MS
        handler.postDelayed(followUp@{
          if (!ensureConsentAndStore() || !isAppProcessingEnabled(packageName) ||
            !isCurrentForegroundPackage(packageName)
          ) return@followUp
          if (needsFreshSkipFrame || !tryHandlePackage(packageName, followUpDepth = followUpDepth + 1)) {
            requestOcrScan(packageName, followUpDepth = followUpDepth + 1)
          }
        }, followUpDelay)
      }
    }, ACTION_VERIFY_DELAY_MS)
  }

  private fun scheduleDynamicAdRetries(packageName: String, waitForLateControls: Boolean = false) {
    val root = findRootForPackage(packageName) ?: return
    val snapshot = snapshotForHints(root)
    val ticket = adScans.begin(
      packageName, root.windowId, SystemClock.uptimeMillis(),
      waitForLateControls || hasPendingAdUi(snapshot),
    ) ?: return
    postNextAdScan(ticket)
  }

  private fun postNextAdScan(ticket: AdScanCoordinator.Ticket) {
    val delay = adScans.nextDelay(ticket, SystemClock.uptimeMillis()) ?: return
    handler.postDelayed({ runDynamicAdRetry(ticket) }, delay)
  }

  private fun hasPendingAdUi(snapshot: AccessibilitySnapshot): Boolean =
    snapshot.nodes.any { node ->
      node.isVisible && (AdSemanticRules.selectSkipLabel(node.controlLabels) != null ||
        AdSemanticRules.selectCloseLabel(node.controlLabels) != null ||
        hasGenericCloseResourceId(node.resourceId) || isExplicitAdBadge(node.compactLabel) ||
        (node.node.isClickable && node.controlLabels.all(AdSemanticRules::isNeutralControlLabel) &&
          isIndependentCloseTarget(node.bounds, snapshot.rootBounds) &&
          node.ancestorRegions.any { region ->
            (isPotentialFeedCard(region, node.bounds, node.bounds, snapshot.rootBounds) &&
              (isCloseAtCardEdge(node, node.bounds, region.bounds, adSpecificClose = false) ||
                node.bounds.centerX() >= region.bounds.left + region.bounds.width() * 0.82)) ||
              isPotentialOcrAdBanner(region, snapshot.rootBounds, node.bounds)
          }))
    } || hasLargeSplashCreative(snapshot)

  private fun snapshotForHints(root: AccessibilityNodeInfo): AccessibilitySnapshot =
    lastSnapshot?.takeIf { it.packageName == root.packageName?.toString() && it.windowId == root.windowId &&
      SystemClock.uptimeMillis() - lastSnapshotAt <= 500L
    } ?: AccessibilitySnapshot.capture(root)

  private fun isEligibleForegroundPackage(packageName: String): Boolean =
    packageName.isNotBlank() && packageName != applicationContext.packageName &&
      !AdDecisionPolicy.isProtectedPackage(packageName) &&
      !ProtectedUiPolicy.isProtectedPackage(packageName) &&
      isCurrentForegroundPackage(packageName)

  private fun maybeStartMediaWatch(packageName: String, eventType: Int) {
    if (!ensureConsentAndStore() || !store.isSettingEnabled(ClearScreenStore.KEY_SKIP_SPLASH_VIDEO, true) ||
      !isEligibleForegroundPackage(packageName) || mediaWatch?.packageName == packageName
    ) return
    val root = findRootForPackage(packageName) ?: return
    val snapshot = snapshotForHints(root)
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
    if (serviceDestroyed || !ensureConsentAndStore() || !store.isMasterEnabled() ||
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
        isScrollingRegion(region)
      } && (node.className.contains("surfaceview") || node.className.contains("textureview") ||
        PLAYER_RESOURCE_MARKERS.any(node.resourceId::contains))
    }

  private fun isAppProcessingEnabled(packageName: String): Boolean =
    !serviceDestroyed && ::store.isInitialized && store.isMasterEnabled() &&
      store.getAppRule(packageName).let { it.skipEnabled && !it.whitelist }

  private fun isOcrWindowCurrent(frame: OcrFrame, snapshot: AccessibilitySnapshot): Boolean =
    frame.packageName == snapshot.packageName && frame.windowId == snapshot.windowId &&
      frame.rootBounds == snapshot.rootBounds &&
      System.currentTimeMillis() - frame.capturedAt <= OCR_FRAME_MAX_AGE_MS

  /**
   * The confirmed China Mobile countdown mutates nodes while its screenshot
   * content remains on the same window. For this one campaign, use the strict
   * window identity and a shorter image age; its full creative/skip evidence is
   * checked again before any coordinate gesture.
   */
  private fun isMobileTrafficSplashOcrCurrent(frame: OcrFrame, snapshot: AccessibilitySnapshot): Boolean =
    frame.packageName == MOBILE_TRAFFIC_CARD_PACKAGE && isOcrWindowCurrent(frame, snapshot) &&
      System.currentTimeMillis() - frame.capturedAt <= MOBILE_OCR_MAX_ACTION_AGE_MS

  private fun isOcrScopeCurrent(frame: OcrFrame, snapshot: AccessibilitySnapshot, path: String): Boolean {
    if (!isOcrWindowCurrent(frame, snapshot)) return false
    fun inScope(nodePath: String): Boolean = nodePath == path || AdDecisionPolicy.isDescendantOf(nodePath, path)
    val captured = frame.nodeStates.filterKeys(::inScope)
    return captured.isNotEmpty() && captured == snapshot.ocrNodeStates.filterKeys(::inScope)
  }

  private fun visualCloseTargets(snapshot: AccessibilitySnapshot): List<Rect> = snapshot.nodes.asSequence()
    .filter { node ->
      node.isVisible && node.isEnabled && node.node.isClickable &&
        AdControlEvidenceRules.isIndependentControl(node.bounds.toBox(), node.bounds.toBox(),
          snapshot.rootBounds.toBox(), snapshot.rootBounds.toBox(), resources.displayMetrics.density.toDouble(), false) &&
        node.bounds.width() <= 96 * resources.displayMetrics.density &&
        node.bounds.height() <= 96 * resources.displayMetrics.density &&
        (node.controlLabels.all(AdSemanticRules::isNeutralControlLabel) ||
          AdSemanticRules.selectCloseLabel(node.controlLabels) != null || hasGenericCloseResourceId(node.resourceId))
    }
    .filterNot { isUnsafeCaiyunRedirectClickNode(it, snapshot) }
    .sortedWith(compareByDescending<ScreenNode> { hasGenericCloseResourceId(it.resourceId) }
      .thenByDescending { isNearPopupEdge(it.bounds, snapshot.rootBounds) })
    .map { Rect(it.bounds) }
    .distinct()
    .take(40)
    .toList()

  private fun findVisualCloseGlyphs(bitmap: Bitmap, targets: List<Rect>, display: Rect): List<OcrLine> =
    targets.mapNotNull { target ->
      if (!display.contains(target)) return@mapNotNull null
      val pixelBounds = Rect(
        ((target.left - display.left).toLong() * bitmap.width / display.width()).toInt(),
        ((target.top - display.top).toLong() * bitmap.height / display.height()).toInt(),
        ((target.right - display.left).toLong() * bitmap.width / display.width()).toInt(),
        ((target.bottom - display.top).toLong() * bitmap.height / display.height()).toInt(),
      )
      val glyph = AdCloseIconDetector.findX(bitmap, pixelBounds) ?: return@mapNotNull null
      val bounds = mapOcrBounds(glyph, bitmap.width, bitmap.height, display) ?: return@mapNotNull null
      OcrLine("×", "×", bounds)
    }

  private fun scheduleOcrIdentityRetry(packageName: String, followUpDepth: Int) {
    if (ocrSignatureRetryPending) return
    ocrSignatureRetryPending = true
    handler.postDelayed({
      ocrSignatureRetryPending = false
      if (ensureConsentAndStore() && isCurrentForegroundPackage(packageName)) {
        requestOcrScan(packageName, retryOnSignatureMismatch = false, followUpDepth = followUpDepth)
      }
    }, OCR_SIGNATURE_RETRY_DELAY_MS)
  }

  private fun requestOcrScan(
    packageName: String,
    retryOnSignatureMismatch: Boolean = true,
    followUpDepth: Int = 0,
    prioritizeTransientSplash: Boolean = false,
    confirmVisualTarget: Boolean = false,
  ) {
    if (serviceDestroyed || !ensureConsentAndStore() || Build.VERSION.SDK_INT < Build.VERSION_CODES.R || ocrScreenshotPending) return
    if (!isAppProcessingEnabled(packageName)) return
    if (!isEligibleForegroundPackage(packageName)) return
    if (ocrSignatureRetryPending && retryOnSignatureMismatch) return
    if (!store.isSettingEnabled(ClearScreenStore.KEY_SKIP_SPLASH_VIDEO, true) &&
      !store.isSettingEnabled(ClearScreenStore.KEY_CLOSE_POPUP_ADS, true) &&
      !store.isSettingEnabled(ClearScreenStore.KEY_CLOSE_FEED_ADS, true)
    ) return

    val now = System.currentTimeMillis()
    val urgentMobileSplash = prioritizeTransientSplash && packageName == MOBILE_TRAFFIC_CARD_PACKAGE
    val minimumInterval = if (confirmVisualTarget) 650L else OCR_MIN_INTERVAL_MS
    if (!urgentMobileSplash && now - lastOcrRequestAt < minimumInterval) return
    val root = findRootForPackage(packageName) ?: return
    val beforeCapture = AccessibilitySnapshot.capture(root)
    if (isProtectedTimeSensitiveSnapshot(root, beforeCapture) ||
      AdDecisionPolicy.isProtectedConfirmation(beforeCapture.nodes.filter { it.isVisible }.map { it.compactLabel })
    ) { recordWifiDiagnostic(packageName, "ocr-request", "reason=protected-screen"); return }
    val rootBounds = beforeCapture.rootBounds
    val displayBounds = screenshotDisplayBounds() ?: return
    if (!hasUsableBounds(rootBounds)) return
    if (beforeCapture.nodes.any { it.isVisible && (it.isPassword || (it.isEditable && it.isFocused)) }) return
    val closeTargets = visualCloseTargets(beforeCapture)

    lastOcrRequestAt = now
    ocrScreenshotPending = true
    recordWifiDiagnostic(packageName, "ocr-request",
      "window=${beforeCapture.windowId} nodes=${beforeCapture.nodes.size} smallTargets=${closeTargets.size}")
    val ocrRequestStartedAt = SystemClock.uptimeMillis()
    try {
      takeScreenshot(
        Display.DEFAULT_DISPLAY,
        ocrExecutor,
        object : TakeScreenshotCallback {
          override fun onSuccess(screenshot: ScreenshotResult) {
            val capturedAt = System.currentTimeMillis()
            val capturedUptime = SystemClock.uptimeMillis()
            val hardwareBuffer = screenshot.hardwareBuffer
            if (!privacyConsentStore.hasCurrentConsent()) {
              hardwareBuffer.close()
              handler.post { pauseForMissingConsent() }
              return
            }
            // A per-app opt-out can arrive while the screenshot is pending.
            // Discard it before starting OCR rather than only blocking the click.
            if (!isAppProcessingEnabled(packageName)) {
              hardwareBuffer.close()
              handler.post { ocrScreenshotPending = false }
              return
            }
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
            val visualCloseGlyphs = runCatching { findVisualCloseGlyphs(bitmap, closeTargets, displayBounds) }
              .getOrDefault(emptyList())

            val textBitmap = if (packageName == MOBILE_TRAFFIC_CARD_PACKAGE &&
              maxOf(bitmap.width, bitmap.height) > MOBILE_OCR_MAX_LONG_EDGE
            ) {
              val scale = MOBILE_OCR_MAX_LONG_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height)
              runCatching {
                Bitmap.createScaledBitmap(
                  bitmap,
                  (bitmap.width * scale).toInt().coerceAtLeast(1),
                  (bitmap.height * scale).toInt().coerceAtLeast(1),
                  true,
                )
              }.getOrNull() ?: bitmap
            } else bitmap

            val recognitionTask = try {
              textRecognizer.process(InputImage.fromBitmap(textBitmap, 0))
            } catch (error: Exception) {
              if (textBitmap !== bitmap) runCatching { textBitmap.recycle() }
              runCatching { bitmap.recycle() }
              handler.post { ocrScreenshotPending = false }
              if (packageName == MOBILE_TRAFFIC_CARD_PACKAGE) {
                Log.i(TAG, "mobile-ocr-result failed=process code=${error.javaClass.simpleName} " +
                  "elapsedMs=${SystemClock.uptimeMillis() - ocrRequestStartedAt}")
              } else if (BuildConfig.DEBUG) {
                Log.d(TAG, "on-device OCR unavailable code=${error.javaClass.simpleName}")
              }
              return
            }
            recognitionTask
              .addOnSuccessListener(ocrExecutor) { recognized ->
                if (!privacyConsentStore.hasCurrentConsent()) {
                  handler.post {
                    ocrScreenshotPending = false
                    pauseForMissingConsent()
                  }
                  return@addOnSuccessListener
                }
                if (!isAppProcessingEnabled(packageName)) {
                  handler.post { ocrScreenshotPending = false }
                  return@addOnSuccessListener
                }
                val lines = recognized.textBlocks.flatMap { block ->
                  block.lines.mapNotNull { line ->
                    val bounds = line.boundingBox ?: return@mapNotNull null
                    val screenBounds = mapOcrBounds(bounds, textBitmap.width, textBitmap.height, displayBounds)
                      ?: return@mapNotNull null
                    if (!rootBounds.contains(screenBounds)) return@mapNotNull null
                    OcrLine(
                      text = line.text,
                      compactText = normalizeLabel(line.text),
                      screenBounds = screenBounds,
                    )
                  }
                }
                val elements = recognized.textBlocks.asSequence()
                  .flatMap { block -> block.lines.asSequence() }
                  .flatMap { line -> line.elements.asSequence() }
                  .mapNotNull { element ->
                    val bounds = element.boundingBox ?: return@mapNotNull null
                    val screenBounds = mapOcrBounds(bounds, textBitmap.width, textBitmap.height, displayBounds)
                      ?: return@mapNotNull null
                    if (!rootBounds.contains(screenBounds)) return@mapNotNull null
                    OcrLine(
                      text = element.text,
                      compactText = normalizeLabel(element.text),
                      screenBounds = screenBounds,
                    )
                  }
                  .take(MAX_OCR_TEXT_ELEMENTS)
                  .toList()
                val controlText = ocrControlText(lines, elements)
                val visualButtons = controlText.asSequence()
                  .filter { AdSemanticRules.controlLabel(listOf(it.text))?.kind != AdSemanticRules.ControlKind.GLYPH }
                  .filter { AdControlEvidenceRules.isIndependentControl(it.screenBounds.toBox(), it.screenBounds.toBox(),
                    rootBounds.toBox(), rootBounds.toBox(), resources.displayMetrics.density.toDouble(), false) }
                  .take(16).mapNotNull { line ->
                    val pixels = Rect(
                      ((line.screenBounds.left - displayBounds.left).toLong() * bitmap.width / displayBounds.width()).toInt(),
                      ((line.screenBounds.top - displayBounds.top).toLong() * bitmap.height / displayBounds.height()).toInt(),
                      ((line.screenBounds.right - displayBounds.left).toLong() * bitmap.width / displayBounds.width()).toInt(),
                      ((line.screenBounds.bottom - displayBounds.top).toLong() * bitmap.height / displayBounds.height()).toInt(),
                    )
                    val button = runCatching { AdVisualButtonDetector.findTextButton(bitmap, pixels) }.getOrNull()
                      ?: return@mapNotNull null
                    val mapped = mapOcrBounds(button, bitmap.width, bitmap.height, displayBounds) ?: return@mapNotNull null
                    if (!rootBounds.contains(mapped)) return@mapNotNull null
                    OcrVisualButton(line, mapped)
                  }.distinctBy { AdSemanticRules.controlLabel(listOf(it.label.text))?.kind to it.bounds }.toList()
                val rasterCreative = if (visualButtons.isEmpty()) false else runCatching {
                  AdVisualButtonDetector.hasLargeCreative(bitmap, Rect(
                    (rootBounds.left.toLong() * bitmap.width / displayBounds.width()).toInt(),
                    (rootBounds.top.toLong() * bitmap.height / displayBounds.height()).toInt(),
                    (rootBounds.right.toLong() * bitmap.width / displayBounds.width()).toInt(),
                    (rootBounds.bottom.toLong() * bitmap.height / displayBounds.height()).toInt(),
                  ))
                }.getOrDefault(false)
                handler.post {
                  ocrScreenshotPending = false
                  if (serviceDestroyed || !ensureConsentAndStore() || !isAppProcessingEnabled(packageName)) return@post
                  recordWifiDiagnostic(packageName, "ocr-result",
                    "lines=${lines.size} elements=${elements.size} xGlyphs=${visualCloseGlyphs.size} " +
                      "textControls=${controlText.size} visualButtons=${visualButtons.size} rasterCreative=$rasterCreative " +
                      "elapsedMs=${SystemClock.uptimeMillis() - ocrRequestStartedAt}")
                  if (packageName == MOBILE_TRAFFIC_CARD_PACKAGE) {
                    Log.i(TAG, "mobile-ocr-result lines=${lines.size} elements=${elements.size} " +
                      "elapsedMs=${SystemClock.uptimeMillis() - ocrRequestStartedAt}")
                  }
                  if (isCurrentForegroundPackage(packageName) && (lines.isNotEmpty() || visualCloseGlyphs.isNotEmpty())) {
                    val currentRoot = findRootForPackage(packageName)
                    val currentSnapshot = currentRoot?.let { AccessibilitySnapshot.capture(it) }
                    if (currentSnapshot == null || currentSnapshot.windowId != beforeCapture.windowId ||
                      currentSnapshot.rootBounds != rootBounds || screenshotDisplayBounds() != displayBounds
                    ) {
                      recordWifiDiagnostic(packageName, "ocr-decision", "reason=window-changed")
                      if (packageName == MOBILE_TRAFFIC_CARD_PACKAGE) {
                        Log.i(TAG, "mobile-ocr-rejected reason=window-changed " +
                          "capturedWindow=${beforeCapture.windowId} currentWindow=${currentSnapshot?.windowId ?: -1} " +
                          "elapsedMs=${SystemClock.uptimeMillis() - ocrRequestStartedAt}")
                      } else {
                        Log.d(TAG, "ocr-rejected package=$packageName reason=window-changed")
                      }
                      if (retryOnSignatureMismatch) scheduleOcrIdentityRetry(packageName, followUpDepth)
                      return@post
                    }
                    val frame = OcrFrame(
                      packageName = packageName,
                      capturedAt = capturedAt,
                      windowId = beforeCapture.windowId,
                      rootBounds = Rect(rootBounds),
                      displayBounds = Rect(displayBounds),
                      nodeStates = beforeCapture.ocrNodeStates,
                      lines = lines,
                      elements = elements,
                      visualCloseGlyphs = visualCloseGlyphs,
                      capturedUptime = capturedUptime,
                      visualButtons = visualButtons,
                      rasterCreative = rasterCreative,
                    )
                    // Evidence is validated against the actual candidate's local
                    // subtree. An unrelated weather value or a ready skip ticking
                    // from 2 to 1 must not discard every ad on the screen.
                    val handled = verifyPendingVisualAction(frame, currentSnapshot) ||
                      tryHandlePackage(packageName, frame, followUpDepth) ||
                      tryHandleOcrOnlyMobileSkip(packageName, frame, currentRoot, currentSnapshot, followUpDepth) ||
                      tryHandleVisualAdControl(packageName, frame, currentRoot, currentSnapshot)
                    recordWifiDiagnostic(packageName, "ocr-decision",
                      "handled=$handled window=${frame.windowId} windowStable=${isOcrScopeCurrent(frame, currentSnapshot, "0")} " +
                        "reason=$lastScanReason rejections=" + controlRejections.entries.joinToString(",") { it.key + ":" + it.value })
                    if (!handled && packageName == WifiPromotionRules.PACKAGE_NAME &&
                      SystemClock.uptimeMillis() - lastWifiOcrDiagnosticAt >= 3_000L
                    ) {
                      lastWifiOcrDiagnosticAt = SystemClock.uptimeMillis()
                      // Numeric diagnostics survive a transient ad without saving any screen text/image.
                      Log.i(TAG, "wifi-ocr-no-action window=${frame.windowId} nodes=${currentSnapshot.nodes.size} " +
                        "smallTargets=${closeTargets.size} xGlyphs=${frame.closeGlyphText.size} " +
                        "visualCreatives=${currentSnapshot.nodes.count { it.isVisible && isVisualCreativeNode(it) }} " +
                        "windowStable=${isOcrScopeCurrent(frame, currentSnapshot, "0")} reason=$lastScanReason")
                    }
                    if (!handled && retryOnSignatureMismatch && currentSnapshot.signature != beforeCapture.signature) {
                      scheduleOcrIdentityRetry(packageName, followUpDepth)
                    }
                  }
                }
              }
              .addOnFailureListener(ocrExecutor) { error ->
                if (BuildConfig.DEBUG) Log.d(TAG, "on-device OCR failed code=${error.javaClass.simpleName}")
                handler.post {
                  ocrScreenshotPending = false
                  recordWifiDiagnostic(packageName, "ocr-result", "reason=recognition-failed")
                }
              }
              .addOnCompleteListener(ocrExecutor) {
                if (textBitmap !== bitmap) runCatching { textBitmap.recycle() }
                runCatching { bitmap.recycle() }
              }
          }

          override fun onFailure(errorCode: Int) {
            if (packageName == MOBILE_TRAFFIC_CARD_PACKAGE) {
              Log.i(TAG, "mobile-ocr-result failed=screenshot code=$errorCode")
            } else if (BuildConfig.DEBUG) {
              Log.d(TAG, "screenshot capture unavailable code=$errorCode")
            }
            handler.post {
              ocrScreenshotPending = false
              recordWifiDiagnostic(packageName, "ocr-result", "reason=screenshot-failed code=$errorCode")
            }
          }
        },
      )
    } catch (error: Exception) {
      ocrScreenshotPending = false
      recordWifiDiagnostic(packageName, "ocr-result", "reason=capture-failed")
      if (packageName == MOBILE_TRAFFIC_CARD_PACKAGE) {
        Log.i(TAG, "mobile-ocr-result failed=capture code=${error.javaClass.simpleName}")
      } else if (BuildConfig.DEBUG) {
        Log.d(TAG, "screenshot capture unavailable code=${error.javaClass.simpleName}")
      }
    }
  }

  private fun isCurrentForegroundPackage(packageName: String): Boolean =
    rootInActiveWindow?.packageName?.toString() == packageName

  private fun visualWindowUnobstructed(root: AccessibilityNodeInfo, bounds: Rect): Boolean {
    val available = runCatching { windows }.getOrDefault(emptyList())
    val active = available.firstOrNull { it.id == root.windowId && it.isActive } ?: return false
    return available.none { window ->
      val other = Rect().also(window::getBoundsInScreen)
      window.id != active.id && window.layer > active.layer &&
        (window.type == AccessibilityWindowInfo.TYPE_APPLICATION ||
          window.type == AccessibilityWindowInfo.TYPE_SYSTEM ||
          window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD ||
          window.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) &&
        other.contains(bounds.centerX(), bounds.centerY())
    }
  }

  private fun queueVisualConfirmation(packageName: String, windowId: Int) {
    if (visualConfirmationPending) return
    visualConfirmationPending = true
    val generation = visualGeneration
    val delay = maxOf(250L, 650L - (System.currentTimeMillis() - lastOcrRequestAt))
    handler.postDelayed({
      if (generation != visualGeneration) return@postDelayed
      visualConfirmationPending = false
      if (!ensureConsentAndStore() || !isAppProcessingEnabled(packageName) ||
        !isEligibleForegroundPackage(packageName) || rootInActiveWindow?.windowId != windowId
      ) { visualAttempts.clear(); return@postDelayed }
      requestOcrScan(packageName, retryOnSignatureMismatch = false, confirmVisualTarget = true)
    }, delay)
  }

  /** No generic X coordinate clicks. Only a two-frame, bounded labelled button on a proven ad page. */
  private fun tryHandleVisualAdControl(
    packageName: String,
    frame: OcrFrame,
    root: AccessibilityNodeInfo,
    snapshot: AccessibilitySnapshot,
  ): Boolean {
    if (!isAppProcessingEnabled(packageName) || !isEligibleForegroundPackage(packageName) ||
      !store.isSettingEnabled(ClearScreenStore.KEY_SKIP_SPLASH_VIDEO, true) ||
      !isOcrScopeCurrent(frame, snapshot, "0") || screenshotDisplayBounds() != frame.displayBounds ||
      SystemClock.uptimeMillis() - frame.capturedUptime > 1_600L ||
      isProtectedTimeSensitiveSnapshot(root, snapshot) ||
      AdDecisionPolicy.isProtectedConfirmation(snapshot.nodes.filter { it.isVisible }.map { it.compactLabel }) ||
      snapshot.nodes.any { it.isVisible && (it.isEditable || it.isPassword) }
    ) { visualAttempts.clear(); return rejectScan("visual-evidence-unavailable") }
    if (pendingVisualAction != null) return false
    val candidates = frame.visualButtons.mapNotNull { button ->
      val label = AdSemanticRules.controlLabel(listOf(button.label.text)) ?: return@mapNotNull null
      if (label.kind == AdSemanticRules.ControlKind.GLYPH) return@mapNotNull null
      // A real small control (including a disabled one) belongs to the node path.
      // Never use visual coordinates to get around its state or business meaning.
      if (snapshot.nodes.any { node -> node.isVisible &&
          node.bounds.contains(button.label.screenBounds.centerX(), button.label.screenBounds.centerY()) &&
          (isUnsafeCaiyunRedirectClickNode(node, snapshot) ||
            (node.bounds.width() <= snapshot.rootBounds.width() * 0.52 &&
              node.bounds.height() <= snapshot.rootBounds.height() * 0.20 &&
              (!node.isEnabled || node.node.isClickable || AdSemanticRules.controlLabel(node.controlLabels) != null ||
                AdSemanticRules.isBusinessControlResource(node.resourceId) ||
                node.controlLabels.any(AdSemanticRules::isBlockedControlLabel)))) }
      ) { rejectControl("visual-has-node-or-blocked"); return@mapNotNull null }
      if (frame.lines.any { Rect.intersects(it.screenBounds, button.bounds) &&
          (AdSemanticRules.isBlockedControlLabel(it.text) || AdSemanticRules.hasPendingSkipHint(listOf(it.text))) }
      ) return@mapNotNull null
      val scope = findUnifiedAdScope(snapshot, frame, button.bounds, label) ?: return@mapNotNull null
      if (!scope.evidence.permitsVisualTouch(label) || !visualWindowUnobstructed(root, button.bounds)) return@mapNotNull null
      Triple(button, label, scope)
    }.distinctBy { it.second.kind to it.first.bounds }
    if (candidates.size != 1) {
      visualAttempts.clear()
      return rejectScan(if (frame.controlText.isEmpty()) "visual-no-action-text" else
        if (frame.visualButtons.isEmpty()) "visual-no-button-boundary" else "visual-ambiguous-or-insufficient")
    }
    val (button, label, scope) = candidates.single()
    val key = if (packageName == MOBILE_TRAFFIC_CARD_PACKAGE) "ocr-mobile-skip|$packageName|" + frame.windowId else
      "visual-ad|$packageName|" + frame.windowId
    if (!visualAttempts.observe(key, button.bounds.toBox(), scope.stamp xor label.kind.ordinal,
        frame.capturedUptime, SystemClock.uptimeMillis())) {
      queueVisualConfirmation(packageName, frame.windowId)
      recordWifiDiagnostic(packageName, "controls", "reason=visual-waiting-second-frame candidates=1")
      return rejectScan("visual-waiting-second-frame")
    }
    // Re-read the window and scoped tree after classification, immediately before dispatch.
    val liveRoot = findRootForPackage(packageName) ?: return false
    val live = AccessibilitySnapshot.capture(liveRoot)
    if (!isOcrScopeCurrent(frame, live, scope.path) || live.windowId != frame.windowId ||
      live.rootBounds != frame.rootBounds || screenshotDisplayBounds() != frame.displayBounds ||
      SystemClock.uptimeMillis() - frame.capturedUptime > 1_600L ||
      isProtectedTimeSensitiveSnapshot(liveRoot, live) || !visualWindowUnobstructed(liveRoot, button.bounds) ||
      !isAppProcessingEnabled(packageName) ||
      findUnifiedAdScope(live, frame, button.bounds, label)?.stamp != scope.stamp
    ) return rejectScan("visual-target-changed")
    val now = SystemClock.uptimeMillis()
    if (now - lastActionAt < ACTION_COOLDOWN_MS || !actionAttempts.tryBegin(key, now)) return rejectScan("repeat-guard")
    lastActionAt = now
    visualAttempts.clear()
    pendingVisualAction = PendingVisualAction(packageName, live.windowId, Rect(live.rootBounds),
      Rect(frame.displayBounds), Rect(button.bounds), Rect(scope.bounds), live.signature, now)
    val path = Path().apply { moveTo(button.bounds.exactCenterX(), button.bounds.exactCenterY()) }
    val gesture = GestureDescription.Builder()
      .addStroke(GestureDescription.StrokeDescription(path, 0L, OCR_SKIP_GESTURE_DURATION_MS)).build()
    val dispatched = runCatching { dispatchGesture(gesture, object : GestureResultCallback() {
      override fun onCompleted(gestureDescription: GestureDescription?) {
        if (!ensureConsentAndStore() || !isAppProcessingEnabled(packageName)) return
        store.appendEvent(type = "skip", packageName = packageName, ruleId = VISUAL_CONTROL_RULE,
          result = "pending", text = "已触摸广告关闭按钮，结果待确认")
        recordWifiDiagnostic(packageName, "result", "mode=visual result=pending")
        handler.postDelayed({
          if (ensureConsentAndStore() && isAppProcessingEnabled(packageName) && isCurrentForegroundPackage(packageName)) {
            requestOcrScan(packageName, retryOnSignatureMismatch = false, confirmVisualTarget = true)
          }
        }, 850L)
      }
      override fun onCancelled(gestureDescription: GestureDescription?) {
        pendingVisualAction = null
        if (!ensureConsentAndStore() || !isAppProcessingEnabled(packageName)) return
        recordWifiDiagnostic(packageName, "result", "mode=visual result=cancelled")
        store.appendEvent(type = "skip", packageName = packageName, ruleId = VISUAL_CONTROL_RULE,
          result = "failed", text = "广告控件触摸未完成")
      }
    }, null) }.getOrDefault(false)
    if (!dispatched) pendingVisualAction = null
    recordWifiDiagnostic(packageName, "action", "mode=visual accepted=$dispatched window=" + frame.windowId)
    lastScanReason = if (dispatched) "visual-touch-dispatched" else "visual-touch-rejected"
    return dispatched
  }

  private fun verifyPendingVisualAction(frame: OcrFrame, snapshot: AccessibilitySnapshot): Boolean {
    val pending = pendingVisualAction ?: return false
    if (pending.packageName != frame.packageName) { pendingVisualAction = null; return false }
    if (frame.capturedUptime - pending.at < 500L) return true
    val sameWindow = pending.windowId == frame.windowId && pending.rootBounds == frame.rootBounds &&
      pending.displayBounds == frame.displayBounds && isOcrScopeCurrent(frame, snapshot, "0")
    val stillPresent = frame.controlText.any { Rect.intersects(pending.button, it.screenBounds) }
    val readableContent = snapshot.signature != pending.signature && snapshot.nodes.count { it.isVisible &&
      it.controlLabels.any { value -> value.length >= 2 && AdSemanticRules.controlLabel(listOf(value)) == null } } >= 4 &&
      snapshot.nodes.any { it.isVisible && it.isScrollable }
    val adEvidenceRemains = (frame.elements + frame.lines).any { pending.scope.contains(it.screenBounds) &&
      (AdControlEvidenceRules.isAdBadge(it.text) || AdControlEvidenceRules.hasRedirectDisclosure(it.text)) }
    val result = when {
      !sameWindow -> "pending"
      stillPresent -> "failed"
      readableContent && !adEvidenceRemains -> "success"
      else -> "pending"
    }
    recordWifiDiagnostic(frame.packageName, "result", "mode=visual result=$result targetStillPresent=$stillPresent")
    store.appendEvent(type = "skip", packageName = frame.packageName, ruleId = VISUAL_CONTROL_RULE, result = result,
      text = when (result) { "success" -> "已关闭广告"; "failed" -> "动作未生效"; else -> "关闭结果未确认" })
    pendingVisualAction = null
    return true
  }

  /**
   * China Mobile renders this confirmed splash's skip/countdown as screenshot
   * text without exposing a matching accessibility click target. Use a gesture
   * only when the exact campaign copy, its ad badge, the top-right skip label,
   * and the same fresh accessibility window all still match.
   */
  private fun tryHandleOcrOnlyMobileSkip(
    packageName: String,
    frame: OcrFrame,
    root: AccessibilityNodeInfo,
    snapshot: AccessibilitySnapshot,
    followUpDepth: Int,
  ): Boolean {
    if (packageName != MOBILE_TRAFFIC_CARD_PACKAGE) return false
    val rejection = when {
      !store.isSettingEnabled(ClearScreenStore.KEY_SKIP_SPLASH_VIDEO, true) -> "splash-disabled"
      !isAppProcessingEnabled(packageName) -> "app-disabled"
      !isEligibleForegroundPackage(packageName) -> "foreground-changed"
      !isMobileTrafficSplashOcrCurrent(frame, snapshot) -> "ocr-window-stale"
      screenshotDisplayBounds() != frame.displayBounds -> "display-changed"
      isProtectedTimeSensitiveSnapshot(root, snapshot) -> "protected-screen"
      snapshot.nodes.any { it.isVisible && (it.isPassword || (it.isEditable && it.isFocused)) } -> "editable-screen"
      else -> null
    }
    if (rejection != null) {
      Log.i(TAG, "mobile-ocr-skip rejected=$rejection window=${frame.windowId} " +
        "ageMs=${System.currentTimeMillis() - frame.capturedAt}")
      return false
    }

    val rootBounds = snapshot.rootBounds
    val screenBox = frame.displayBounds.toBox()
    val screenWidth = rootBounds.width().coerceAtLeast(1)
    val screenHeight = rootBounds.height().coerceAtLeast(1)
    val ocrTexts = frame.lines.map { AdCreativeRules.Text(it.text, it.screenBounds.toBox()) }
    val hasAdBadge = frame.lines.any { line ->
      hasStrongAdSignal(line.text) &&
        line.screenBounds.centerX() <= rootBounds.left + screenWidth * 0.42f &&
        line.screenBounds.centerY() <= rootBounds.top + screenHeight * 0.28f
    }

    val skipCandidates = (frame.elements + frame.lines)
      .distinctBy { it.compactText to it.screenBounds }
      .asSequence()
      .filter { line ->
        !AdSemanticRules.isBlockedSkipLabel(line.text) &&
          (isCountdownSkipLabel(line.compactText) || line.compactText == "跳过" || line.compactText == "skip")
      }
      .filter { line ->
        val bounds = line.screenBounds
        hasUsableBounds(bounds) && rootBounds.contains(bounds) && frame.displayBounds.contains(bounds) &&
          isVeryNearTopRight(bounds, rootBounds) && isCompactControl(bounds, rootBounds) &&
          hasCompactSkipTarget(bounds, bounds, rootBounds)
      }
      .toList()
    val campaignCandidates = skipCandidates.filter { line ->
      AdCreativeRules.hasMobileTrafficCardSplashComposition(
        packageName, ocrTexts, line.screenBounds.toBox(), screenBox,
      )
    }
    if (!hasAdBadge || campaignCandidates.isEmpty()) {
      val normalizedOffer = AdSemanticRules.normalize(frame.lines.joinToString("") { it.text })
      val offerMarkers = listOf("55gb", "大流量卡", "39元", "40gb", "云盘")
        .count { marker -> normalizedOffer.contains(marker) }
      val hasRedirect = frame.lines.any { line ->
        listOf("点击跳转详情页面", "点击跳转详情页", "点击跳转第三方应用", "点击跳转第三方页面")
          .any { marker -> AdSemanticRules.normalize(line.text).contains(marker) }
      }
      Log.i(TAG, "mobile-ocr-skip no-match badge=$hasAdBadge skip=${skipCandidates.size} " +
        "campaign=${campaignCandidates.size} offerMarkers=$offerMarkers redirect=$hasRedirect " +
        "lines=${frame.lines.size} ageMs=${System.currentTimeMillis() - frame.capturedAt}")
      return false
    }
    val skipLine = campaignCandidates.minByOrNull {
      it.screenBounds.width().toLong() * it.screenBounds.height().toLong()
    } ?: return false

    // If the tree already exposes an enabled clickable skip target at this OCR
    // location, let the regular node path handle it and its identity checks.
    if (snapshot.nodes.any { node ->
        val hasSkipIdentity = node.controlLabels.any { label -> isSkipOcrLabel(normalizeLabel(label)) } ||
          isSkipOcrLabel(normalizeLabel(node.resourceId)) || hasSkipResourceId(node.resourceId)
        val target = if (node.isVisible && hasSkipIdentity) clickableTarget(node.node) else null
        if (target == null) return@any false
        val targetBounds = Rect().also(target::getBoundsInScreen)
        targetBounds.contains(skipLine.screenBounds.centerX(), skipLine.screenBounds.centerY()) ||
          skipLine.screenBounds.contains(targetBounds.centerX(), targetBounds.centerY())
      }
    ) return false

    val now = System.currentTimeMillis()
    val actionNow = SystemClock.uptimeMillis()
    if (actionNow - lastActionAt < ACTION_COOLDOWN_MS) return false
    // The same campaign's countdown text/box can shrink; it is still one skip per window.
    val fingerprint = "ocr-mobile-skip|$packageName|${frame.windowId}"
    if (!actionAttempts.tryBegin(fingerprint, actionNow)) return false
    lastActionAt = actionNow

    Log.i(TAG, "mobile-ocr-skip candidate window=${frame.windowId} label=${skipLine.compactText} " +
      "bounds=${skipLine.screenBounds} ageMs=${now - frame.capturedAt}")

    val centerX = skipLine.screenBounds.centerX().toFloat()
    val centerY = skipLine.screenBounds.centerY().toFloat()
    val path = Path().apply {
      moveTo(centerX, centerY)
      lineTo(centerX + 1f, centerY)
    }
    val gesture = GestureDescription.Builder()
      .addStroke(GestureDescription.StrokeDescription(path, 0L, OCR_SKIP_GESTURE_DURATION_MS))
      .build()
    val dispatched = runCatching {
      dispatchGesture(gesture, object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
          if (!ensureConsentAndStore() || !isAppProcessingEnabled(packageName) ||
            !isEligibleForegroundPackage(packageName)
          ) return
          Log.i(TAG, "ad-action package=$packageName rule=$MOBILE_OCR_SKIP_RULE window=${frame.windowId} " +
            "bounds=${skipLine.screenBounds} gesture=completed result=unconfirmed")
          store.appendEvent(
            type = "skip",
            packageName = packageName,
            ruleId = MOBILE_OCR_SKIP_RULE,
            result = "pending",
            text = "已识别并尝试点击跳过，关闭结果待确认",
          )
          handler.postDelayed({
            if (ensureConsentAndStore() && isAppProcessingEnabled(packageName) &&
              isCurrentForegroundPackage(packageName)
            ) requestOcrScan(packageName, retryOnSignatureMismatch = false, followUpDepth = followUpDepth + 1)
          }, POST_ACTION_RESCAN_DELAY_MS)
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
          if (!ensureConsentAndStore() || !::store.isInitialized) return
          Log.i(TAG, "ad-action package=$packageName rule=$MOBILE_OCR_SKIP_RULE " +
            "window=${frame.windowId} bounds=${skipLine.screenBounds} gesture=cancelled")
          store.appendEvent(
            type = "skip",
            packageName = packageName,
            ruleId = MOBILE_OCR_SKIP_RULE,
            result = "failed",
            text = "跳过手势未完成",
          )
        }
      }, null)
    }.getOrDefault(false)
    if (!dispatched) {
      lastScanReason = "ocr-gesture-rejected"
      Log.i(TAG, "ad-action package=$packageName rule=$MOBILE_OCR_SKIP_RULE " +
        "window=${frame.windowId} bounds=${skipLine.screenBounds} gesture=not-dispatched")
      store.appendEvent(
        type = "skip",
        packageName = packageName,
        ruleId = MOBILE_OCR_SKIP_RULE,
        result = "failed",
        text = "跳过手势未能发送",
      )
      return false
    }

    lastScanReason = "ocr-gesture-dispatched"
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "action=$MOBILE_OCR_SKIP_RULE package=$packageName label=${skipLine.text.take(MAX_LOG_LABEL_LENGTH)}")
    }
    return true
  }

  /**
   * The system may bind this service before consent, or keep it bound after
   * consent is withdrawn. Read only the isolated consent record until the
   * user agrees; do not open the rules/log store or inspect another app's UI.
   */
  private fun ensureConsentAndStore(): Boolean {
    if (!privacyConsentStore.hasCurrentConsent()) {
      pauseForMissingConsent()
      return false
    }
    privacyPaused = false
    if (!::store.isInitialized) {
      store = ClearScreenStore(this).apply { prepareAfterConsent() }
    }
    return true
  }

  private fun pauseForMissingConsent() {
    if (privacyPaused) return
    privacyPaused = true
    ClearScreenSettingsObserver.clear()
    handler.removeCallbacksAndMessages(null)
    eventScans.clear()
    eventScanPosted = false
    lastSnapshot = null
    wifiDiagnostics.clear()
    clearVisualState()
    calendarWindowClass = ""
    calendarWindowId = -1
    adScans.cancel()
    actionAttempts.clear()
    stopMediaWatch()
    observedPlayerPackage = null
    observedPlayerVisible = false
    ocrScreenshotPending = false
    ocrSignatureRetryPending = false
  }

  @Suppress("DEPRECATION")
  private fun screenshotDisplayBounds(): Rect? {
    val display = getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY) ?: return null
    val size = Point().also(display::getRealSize)
    return Rect(0, 0, size.x, size.y).takeIf(::hasUsableBounds)
  }

  private fun mapOcrBounds(bounds: Rect, imageWidth: Int, imageHeight: Int, displayBounds: Rect): Rect? =
    AdGeometryRules.mapScreenshotBox(
      bounds.toBox(), imageWidth, imageHeight, displayBounds.width(), displayBounds.height(),
    )?.let { Rect(it.left, it.top, it.right, it.bottom) }

  private fun Rect.toBox(): AdGeometryRules.Box = AdGeometryRules.Box(left, top, right, bottom)

  /** Uses local ownership and independent evidence, with no screen-corner gate. */
  private fun findUnifiedAdScope(
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame,
    targetBounds: Rect,
    label: AdSemanticRules.ControlLabel,
    owner: ScreenNode? = null,
  ): UnifiedAdScope? {
    val density = resources.displayMetrics.density.toDouble()
    val rootArea = snapshot.rootBounds.width().toLong() * snapshot.rootBounds.height()
    if (rootArea <= 0 || !isOcrWindowCurrent(frame, snapshot)) return null
    val scopes = snapshot.nodes.asSequence().filter { scope ->
      scope.isVisible && !scope.isScrollable && scope.bounds.contains(targetBounds) &&
        scope.bounds.width().toLong() * scope.bounds.height() > targetBounds.width().toLong() * targetBounds.height() * 2 &&
        (owner == null || owner.path == scope.path || AdDecisionPolicy.isDescendantOf(owner.path, scope.path))
    }.sortedBy { it.bounds.width().toLong() * it.bounds.height() }
    for (scope in scopes) {
      if (!isOcrScopeCurrent(frame, snapshot, scope.path)) {
        rejectControl("stale-scope")
        continue
      }
      val local = snapshot.nodes.filter { it.isVisible &&
        (it.path == scope.path || AdDecisionPolicy.isDescendantOf(it.path, scope.path)) }
      if (local.any { it.isScrollable || SCROLL_CONTAINER_CLASSES.any(it.className::contains) ||
          it.isEditable || it.isPassword || it.controlLabels.any(AdSemanticRules::isSensitiveUiContext) }
      ) continue
      val areaRatio = scope.bounds.width().toLong() * scope.bounds.height() / rootArea.toDouble()
      val fullScreen = areaRatio >= 0.82
      if (fullScreen && VideoAdRules.isSupported(snapshot.packageName) && hasPlayerSurface(snapshot) &&
        label.kind != AdSemanticRules.ControlKind.SKIP && !label.explicitAd &&
        (owner == null || owner.ancestorRegions.none { it.path != "0" && AdControlEvidenceRules.isAdContainer(it.resourceId) })
      ) continue
      if (fullScreen && snapshot.nodes.any { it.isVisible &&
          (it.isScrollable || SCROLL_CONTAINER_CLASSES.any(it.className::contains) ||
            it.controlLabels.any { value -> normalizeLabel(value) in setOf(
              "倍速", "选集", "弹幕", "退出全屏", "退出播放", "登录", "注册", "支付", "同意", "不同意") }) }
      ) continue
      val visuals = local.filter { it.node != owner?.node && isVisualCreativeNode(it) &&
        it.bounds.width().toLong() * it.bounds.height() >= rootArea * (if (fullScreen) 0.36 else 0.008) }
      if (visuals.isEmpty() && (!fullScreen || !frame.rasterCreative ||
          frame.visualButtons.none { it.bounds == targetBounds })) continue
      val texts = (frame.lines + frame.elements).filter { scope.bounds.contains(it.screenBounds) }
      if (texts.any { AdSemanticRules.isBlockedControlLabel(it.text) || ProtectedUiPolicy.isProtectedText(it.text) ||
          normalizeLabel(it.text) in setOf("新手引导", "操作教程", "支付确认", "身份验证", "权限请求") }
        || AdDecisionPolicy.isProtectedConfirmation(texts.map { it.compactText })
      ) continue
      val badge = texts.any { it.screenBounds.height() <= 64 * density &&
        AdControlEvidenceRules.isAdBadge(it.text) } || local.any { peer ->
        peer.node != owner?.node && peer.bounds.height() <= 64 * density &&
          peer.controlLabels.any(AdControlEvidenceRules::isAdBadge)
      }
      val redirectTexts = texts.filter { AdControlEvidenceRules.hasRedirectDisclosure(it.text) }
      val redirect = redirectTexts.isNotEmpty() || local.any { peer -> peer.node != owner?.node &&
        peer.controlLabels.any(AdControlEvidenceRules::hasRedirectDisclosure) }
      val sdk = local.any { it.node != owner?.node && AdControlEvidenceRules.isAdContainer(it.resourceId) }
      val readyCountdown = label.kind == AdSemanticRules.ControlKind.SKIP &&
        AdSemanticRules.isCountdownSkip(label.value) && SystemClock.uptimeMillis() - foregroundEntryAt in 0L..8_000L
      val evidence = AdControlEvidenceRules.Evidence(
        visual = true, badge = badge, redirect = redirect,
        callToAction = texts.any { AdControlEvidenceRules.hasCallToAction(it.text) } ||
          local.any { it.controlLabels.any(AdControlEvidenceRules::hasCallToAction) },
        adContainer = sdk, fullScreen = fullScreen, readyCountdown = readyCountdown,
      )
      if (!evidence.permits(label)) { rejectControl("insufficient-ad-evidence"); continue }
      // Even a fully evidenced ad does not authorize a broad parent click area.
      if (!AdControlEvidenceRules.isIndependentControl(targetBounds.toBox(), targetBounds.toBox(),
          scope.bounds.toBox(), snapshot.rootBounds.toBox(), density,
          false)
      ) { rejectControl("oversize-target"); continue }
      val overlay = !fullScreen && isOverlayLayer(scope) && areaRatio >= 0.12
      return UnifiedAdScope(scope.path, Rect(scope.bounds), evidence, overlay,
        (scope.path + "|" + scope.resourceId + "|" + scope.bounds + "|" + evidence +
          "|" + visuals.map { it.bounds }.distinct() +
          "|" + redirectTexts.map { it.compactText }.distinct().sorted()).hashCode())
    }
    rejectControl("unowned-ad-control")
    return null
  }

  private fun findUnifiedControlCandidate(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): DetectionAction? {
    if (!node.isVisible || !node.isEnabled || frame == null) return null
    val nativeLabel = AdSemanticRules.controlLabel(node.controlLabels)
    val ocrLabels = frame.controlText.filter { node.bounds.contains(it.screenBounds) }
    if (nativeLabel == null && ocrLabels.isEmpty()) return null
    val target = clickableTarget(node.node) ?: run { rejectControl("no-accessible-target"); return null }
    val bounds = Rect().also(target::getBoundsInScreen)
    val targetNode = snapshot.nodes.firstOrNull { it.node == target } ?: return null
    if (!target.isVisibleToUser || !target.isEnabled || targetNode.isScrollable ||
      isUnsafeCaiyunRedirectClickTarget(target, snapshot.packageName)
    ) { rejectControl("target-unavailable"); return null }
    val texts = frame.controlText.filter { bounds.contains(it.screenBounds) }
    val label = nativeLabel ?: texts.mapNotNull { AdSemanticRules.controlLabel(listOf(it.text)) }
      .distinctBy { it.kind }.singleOrNull() ?: return null
    val controlNodes = snapshot.nodes.filter { it.isVisible &&
      (it.path == targetNode.path || AdDecisionPolicy.isDescendantOf(it.path, targetNode.path)) }
    if (controlNodes.any { it.isScrollable || it.isEditable || it.isPassword ||
        it.controlLabels.any { value ->
          !AdSemanticRules.isNeutralControlLabel(value) && !AdSemanticRules.isControlCounter(value) &&
            AdSemanticRules.controlLabel(listOf(value))?.kind != label.kind
        } } || frame.lines.any { bounds.contains(it.screenBounds.centerX(), it.screenBounds.centerY()) &&
          (AdSemanticRules.isBlockedControlLabel(it.text) || AdSemanticRules.hasPendingSkipHint(listOf(it.text))) }
    ) { rejectControl("business-or-waiting-control"); return null }
    val labelBounds = texts.firstOrNull {
      AdSemanticRules.controlLabel(listOf(it.text))?.kind == label.kind
    }?.screenBounds ?: node.bounds
    if (!AdControlEvidenceRules.isIndependentControl(bounds.toBox(), labelBounds.toBox(),
        snapshot.rootBounds.toBox(), snapshot.rootBounds.toBox(), resources.displayMetrics.density.toDouble(),
        label.kind == AdSemanticRules.ControlKind.GLYPH)
    ) { rejectControl("oversize-target"); return null }
    val scope = findUnifiedAdScope(snapshot, frame, bounds, label, targetNode) ?: return null
    val category = when {
      scope.evidence.fullScreen || label.kind == AdSemanticRules.ControlKind.SKIP -> DetectionCategory.SPLASH_VIDEO
      scope.overlay -> DetectionCategory.POPUP
      else -> DetectionCategory.FEED
    }
    val setting = when (category) {
      DetectionCategory.SPLASH_VIDEO -> ClearScreenStore.KEY_SKIP_SPLASH_VIDEO
      DetectionCategory.POPUP -> ClearScreenStore.KEY_CLOSE_POPUP_ADS
      DetectionCategory.FEED -> ClearScreenStore.KEY_CLOSE_FEED_ADS
    }
    if (!store.isSettingEnabled(setting, true)) return null
    return DetectionAction(
      target, bounds, score = if (scope.overlay) 188 else 160, minimumScore = 150,
      actionPriority = if (scope.overlay) 0 else 2, ruleId = UNIFIED_CONTROL_RULE,
      label = "广告关闭控件", successText = "已关闭广告", category = category,
      allowVerifiedTouchFallback = texts.any { AdSemanticRules.controlLabel(listOf(it.text))?.kind == label.kind },
      touchEvidenceScope = scope.path, foregroundOverlay = scope.overlay,
    )
  }

  private fun runDynamicAdRetry(ticket: AdScanCoordinator.Ticket) {
    if (!adScans.isCurrent(ticket)) return
    val packageName = ticket.packageName
    if (!ensureConsentAndStore() || !isAppProcessingEnabled(packageName) ||
      !isEligibleForegroundPackage(packageName)
    ) {
      adScans.cancel(ticket)
      return
    }
    val root = findRootForPackage(packageName)
    if (root == null || root.windowId != ticket.windowId || isProtectedTimeSensitiveWindow(root, packageName)) {
      adScans.cancel(ticket)
      return
    }
    if (tryHandlePackage(packageName)) {
      adScans.cancel(ticket)
      return
    }
    // Later controls need fresh OCR as well as node rescans. Ordinary pages
    // keep the short node-only schedule; screenshots remain globally throttled.
    if (hasPendingAdUi(AccessibilitySnapshot.capture(root))) requestOcrScan(packageName)
    postNextAdScan(ticket)
  }

  private fun findAdAction(
    root: AccessibilityNodeInfo,
    ocrFrame: OcrFrame? = null,
    capturedSnapshot: AccessibilitySnapshot? = null,
  ): DetectionAction? {
    controlRejections.clear()
    val snapshot = capturedSnapshot ?: AccessibilitySnapshot.capture(root)
    if (snapshot.nodes.isEmpty() || isProtectedTimeSensitiveSnapshot(root, snapshot)) return null
    if (AdDecisionPolicy.isProtectedConfirmation(
        snapshot.nodes.asSequence().filter { it.isVisible }.map { it.compactLabel }.toList()
      )
    ) return null
    val currentOcrFrame = ocrFrame?.takeIf { frame ->
      isOcrWindowCurrent(frame, snapshot)
    }
    // Caiyun's cll_close_click is an ad click-through despite looking like an X.
    // Exclude it, its descendants, and candidates whose clickable target is that
    // view before any node, OCR, feed, splash, or video path can score them.
    val candidateNodes = snapshot.nodes.filterNot {
      isUnsafeCaiyunRedirectClickNode(it, snapshot) || AdSemanticRules.isBusinessControlResource(it.resourceId) ||
        clickableTarget(it.node)?.viewIdResourceName?.let(AdSemanticRules::isBusinessControlResource) == true
    }
    val foregroundAdPaths = foregroundAdScopes(snapshot, currentOcrFrame)

    // Prefer the stricter video-player classifier. If it finds no safe action,
    // still allow the package-independent rules to recognize ad UI such as a
    // top-right "关闭" control, while retaining their local evidence gates.
    val videoPlayerContext = VideoAdRules.isSupported(snapshot.packageName) &&
      hasPlayerSurface(snapshot)
    val videoCandidates = if (videoPlayerContext) {
      candidateNodes.mapNotNull { node -> findVideoAdControl(node, snapshot) }
    } else emptyList()
    val candidates = if (AdDecisionPolicy.shouldEvaluateGenericAdCandidates(
        videoPlayerContext, videoCandidates.isNotEmpty())
    ) {
      videoCandidates + candidateNodes.mapNotNull { node ->
        findSkipCandidate(node, snapshot, currentOcrFrame)
      } + candidateNodes.mapNotNull { node ->
        findPopupCloseCandidate(node, snapshot, currentOcrFrame)
      } + candidateNodes.mapNotNull { node ->
        findEmbeddedAdCloseCandidate(node, snapshot, currentOcrFrame)
      } + candidateNodes.mapNotNull { node ->
        findFeedCloseCandidate(node, snapshot, currentOcrFrame)
      }
    } else videoCandidates

    val unifiedCandidates = candidateNodes.mapNotNull { node ->
      findUnifiedControlCandidate(node, snapshot, currentOcrFrame)
    }
    return (candidates + unifiedCandidates)
      .filter { action ->
        val blocked = currentOcrFrame?.lines?.any { line ->
          action.targetBounds.contains(line.screenBounds.centerX(), line.screenBounds.centerY()) &&
            (AdSemanticRules.isBlockedControlLabel(line.text) ||
              AdSemanticRules.hasPendingSkipHint(listOf(line.text)))
        } == true
        if (blocked) rejectControl("business-or-waiting-control")
        !blocked
      }
      .filter { action ->
        if (foregroundAdPaths.isEmpty()) true else {
          val targetPath = snapshot.nodes.firstOrNull { it.node == action.target }?.path
          val inForeground = targetPath != null && foregroundAdPaths.any {
            targetPath == it || AdDecisionPolicy.isDescendantOf(targetPath, it) }
          if (!inForeground) rejectControl("behind-foreground-ad")
          inForeground
        }
      }
      .filter { it.score >= it.minimumScore }
      .map { action ->
        val touchScope = if (!action.allowVerifiedTouchFallback && currentOcrFrame != null) {
          findVerifiedAdControlTouchScope(action, snapshot, currentOcrFrame)
        } else null
        val evidenceScope = touchScope ?: action.touchEvidenceScope
        val modalBounds = snapshot.nodes.firstOrNull { it.path == evidenceScope }?.bounds
        val verifiedModal = action.category == DetectionCategory.POPUP && evidenceScope != "0" &&
          (action.allowVerifiedTouchFallback || touchScope != null) && modalBounds != null &&
          AdGeometryRules.isModalCorner(action.targetBounds.toBox(), modalBounds.toBox(), snapshot.rootBounds.toBox())
        action.copy(
          score = if (verifiedModal) maxOf(action.score, 180) else action.score,
          allowVerifiedTouchFallback = action.allowVerifiedTouchFallback || touchScope != null,
          touchEvidenceScope = evidenceScope,
          foregroundOverlay = action.foregroundOverlay || verifiedModal,
        )
      }
      .maxWithOrNull(
        compareBy<DetectionAction> { it.foregroundOverlay }
          .thenBy { it.score }
          .thenByDescending { it.actionPriority }
          .thenBy { it.targetBounds.top },
      )?.copy(screenSignature = snapshot.signature)
  }

  /** Identify the front ad layer even when it exposes no accessible close button. */
  private fun foregroundAdScopes(snapshot: AccessibilitySnapshot, frame: OcrFrame?): Set<String> {
    val rootArea = snapshot.rootBounds.width().toLong() * snapshot.rootBounds.height()
    if (rootArea <= 0) return emptySet()
    return snapshot.nodes.asSequence().filter { scope ->
      scope.isVisible && scope.depth > 0 && scope.node.childCount >= 2 &&
        isOverlayLayer(scope) && !isVisualCreativeNode(scope) &&
        scope.ancestorRegions.none(::isScrollingRegion) &&
        scope.bounds.width().toLong() * scope.bounds.height() >= rootArea * 0.12
    }.filter { scope ->
      val local = snapshot.nodes.filter { it.isVisible &&
        (it.path == scope.path || AdDecisionPolicy.isDescendantOf(it.path, scope.path)) }
      val ocr = frame?.takeIf { isOcrScopeCurrent(it, snapshot, scope.path) }?.compositionText
        ?.filter { AdControlEvidenceRules.contains(scope.bounds.toBox(), it.bounds) }?.map { it.value }.orEmpty()
      val labels = local.flatMap { it.controlLabels } + ocr
      local.none { it.isScrollable || it.isEditable || it.isPassword } &&
        local.any(::isVisualCreativeNode) && labels.any(AdControlEvidenceRules::isAdBadge) &&
        (labels.any(AdControlEvidenceRules::hasCallToAction) ||
          labels.any(AdControlEvidenceRules::hasRedirectDisclosure) ||
          local.any { AdControlEvidenceRules.isAdContainer(it.resourceId) })
    }.map { it.path }.toSet()
  }

  /** Same-control retry only for the two observed hosts, never a page-level X or an OCR-only point. */
  private fun findVerifiedAdControlTouchScope(
    action: DetectionAction,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame,
  ): String? {
    if (snapshot.packageName !in setOf(WifiPromotionRules.PACKAGE_NAME, KNOWN_CAIYUN_INTERSTITIAL_PACKAGE) ||
      action.category == DetectionCategory.SPLASH_VIDEO ||
      !isIndependentCloseTarget(action.targetBounds, snapshot.rootBounds) ||
      frame.closeGlyphText.none { isCompactOcrCloseGlyph(it, action.targetBounds, snapshot.rootBounds) }
    ) return null
    val node = snapshot.nodes.firstOrNull { it.node == action.target } ?: return null
    if (!node.node.isClickable || node.isScrollable || !node.controlLabels.all {
        AdSemanticRules.isNeutralControlLabel(it) || AdSemanticRules.selectCloseLabel(listOf(it)) != null
      }
    ) return null
    for (region in node.ancestorRegions.sortedBy { it.bounds.width().toLong() * it.bounds.height() }) {
      if ((!isPotentialOcrAdContainer(region, snapshot.rootBounds) &&
          !isPotentialOcrAdBanner(region, snapshot.rootBounds, action.targetBounds)) ||
        !region.bounds.contains(action.targetBounds) ||
        !isOcrScopeCurrent(frame, snapshot, region.path)
      ) continue
      if (!isUninterruptedAdCardScope(region, snapshot)) continue
      val evidence = findFeedAdEvidence(region, snapshot, frame) ?: continue
      if (evidence.explicitAdMarker && evidence.hasCallToAction) return region.path
    }
    return null
  }

  /**
   * Runtime guard shared by all node and OCR recognition paths.
   * A real alarm or countdown must always win over a putative ad action.
   */
  private fun isProtectedTimeSensitiveWindow(
    root: AccessibilityNodeInfo,
    packageName: String,
  ): Boolean =
    isProtectedTimeSensitiveSnapshot(root, AccessibilitySnapshot.capture(root)) ||
      ProtectedUiPolicy.isProtectedPackage(packageName)

  private fun isProtectedTimeSensitiveSnapshot(
    root: AccessibilityNodeInfo,
    snapshot: AccessibilitySnapshot,
  ): Boolean =
    ProtectedUiPolicy.isProtectedScreen(
      packageName = snapshot.packageName,
      windowClassName = if (snapshot.packageName == WifiPromotionRules.CALENDAR_PACKAGE_NAME &&
        calendarWindowClass.isNotEmpty() && snapshot.windowId == calendarWindowId
      ) calendarWindowClass else root.className?.toString().orEmpty(),
      visibleLabels = snapshot.nodes.asSequence().filter { it.isVisible }
        .map { it.label }.asIterable(),
      visibleResourceIds = snapshot.nodes.asSequence().filter { it.isVisible }
        .map { it.resourceId }.asIterable(),
    )

  private fun findVideoAdControl(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
  ): DetectionAction? {
    if (!store.isSettingEnabled(ClearScreenStore.KEY_SKIP_SPLASH_VIDEO, true) ||
      !node.isVisible || !node.isEnabled ||
      node.ancestorLabels.any(AdSemanticRules::isSensitiveUiContext)
    ) return null
    val target = clickableTarget(node.node) ?: return null
    val bounds = Rect().also(target::getBoundsInScreen)
    if (!hasUsableBounds(bounds) ||
      !AdGeometryRules.isCompactControl(
        AdGeometryRules.Box(bounds.left, bounds.top, bounds.right, bounds.bottom),
        AdGeometryRules.Box(snapshot.rootBounds.left, snapshot.rootBounds.top,
          snapshot.rootBounds.right, snapshot.rootBounds.bottom),
      )
    ) return null
    val w = snapshot.rootBounds.width().coerceAtLeast(1)
    val h = snapshot.rootBounds.height().coerceAtLeast(1)
    val localAdContext = snapshot.nodes.any { peer ->
      peer.isVisible && peer.path != node.path &&
        VideoAdRules.hasAdEvidence(peer.label, peer.resourceId) &&
        kotlin.math.abs(peer.bounds.centerX() - bounds.centerX()) <= w * 0.42f &&
        kotlin.math.abs(peer.bounds.centerY() - bounds.centerY()) <= h * 0.18f
    }
    val classifier = VideoAdRules.classify(
      labels = listOfNotNull(
        node.node.text?.toString(),
        node.node.contentDescription?.toString(),
      ).ifEmpty { listOf(node.label) },
      resourceId = node.resourceId,
      nearbyAdEvidence = localAdContext,
      atRightEdge = bounds.centerX() >= snapshot.rootBounds.left + w * 0.65f,
    )
    if (classifier == VideoAdRules.Action.NONE) return null
    val explicit = classifier == VideoAdRules.Action.EXPLICIT_SKIP ||
      classifier == VideoAdRules.Action.EXPLICIT_CLOSE
    return DetectionAction(
      target = target,
      targetBounds = bounds,
      score = if (explicit) 175 else 128,
      minimumScore = if (explicit) 140 else 116,
      actionPriority = 2,
      ruleId = when (classifier) {
        VideoAdRules.Action.EXPLICIT_SKIP -> "builtin.video.ad-skip"
        VideoAdRules.Action.EXPLICIT_CLOSE -> "builtin.video.ad-close"
        VideoAdRules.Action.CONTEXTUAL_SKIP -> "builtin.video.contextual-skip"
        else -> return null
      },
      label = node.label.take(MAX_LOG_LABEL_LENGTH),
      successText = "已执行视频广告操作",
      category = DetectionCategory.SPLASH_VIDEO,
    )
  }

  private fun findSkipCandidate(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): DetectionAction? {
    if (!store.isSettingEnabled(ClearScreenStore.KEY_SKIP_SPLASH_VIDEO, true)) return null
    if (!node.isVisible) return null
    val ocrFrame = frame?.takeIf { isOcrScopeCurrent(it, snapshot, "0") }
    if (node.controlLabels.any(AdSemanticRules::isBlockedSkipLabel)) return null
    val compact = AdSemanticRules.selectSkipLabel(node.controlLabels) ?: node.compactLabel
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
    if (ocrFrame?.lines?.any { line ->
        targetBounds.contains(line.screenBounds.centerX(), line.screenBounds.centerY()) &&
          AdSemanticRules.isBlockedSkipLabel(line.text)
      } == true
    ) return null
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
    val targetNode = snapshot.nodes.firstOrNull { it.node == target }
    if (targetNode != null && AdSemanticRules.hasPendingSkipHint(snapshot.nodes.asSequence()
        .filter { it.path == targetNode.path || AdDecisionPolicy.isDescendantOf(it.path, targetNode.path) }
        .flatMap { it.controlLabels.asSequence() }.toList())
    ) return null
    val labelNearTopRight = isNearTopRight(labelBounds, snapshot.rootBounds)
    val targetNearTopRight = isNearTopRight(targetBounds, snapshot.rootBounds)
    val nearTopRight = labelNearTopRight || targetNearTopRight
    val veryNearTopRight = isVeryNearTopRight(labelBounds, snapshot.rootBounds) ||
      isVeryNearTopRight(targetBounds, snapshot.rootBounds)
    val labelCompactControl = isCompactControl(labelBounds, snapshot.rootBounds)
    val targetCompactControl = isCompactControl(targetBounds, snapshot.rootBounds)
    val compactControl = labelCompactControl || targetCompactControl
    val nodeSplashTexts = snapshot.nodes.filter { it.isVisible }.flatMap { visibleNode ->
      visibleNode.controlLabels.map { AdCreativeRules.Text(it, visibleNode.bounds.toBox()) }
    }
    val nodeMobileTrafficSplash = AdCreativeRules.hasMobileTrafficCardSplashComposition(
      snapshot.packageName, nodeSplashTexts, targetBounds.toBox(), snapshot.rootBounds.toBox())
    val nodeSplashAdContext = nodeMobileTrafficSplash || (
      snapshot.nodes.none { it.isVisible && it.isScrollable } &&
        AdCreativeRules.hasRedirectSplashComposition(
          nodeSplashTexts, targetBounds.toBox(), snapshot.rootBounds.toBox())
      )
    val caiyunRedirectSplashContext = node.ancestorRegions.any { region ->
      region.resourceId == "com.nowcasting.activity:id/splash_container" &&
        !region.isScrollable &&
        snapshot.nodes.none { it.isVisible && it.isScrollable && AdDecisionPolicy.isDescendantOf(it.path, region.path) } &&
        AdCreativeRules.hasCaiyunRedirectSplash(
          snapshot.packageName, region.resourceId, region.bounds.toBox(), snapshot.rootBounds.toBox(),
          candidateText, targetBounds.toBox(),
          snapshot.nodes.filter { it.isVisible && AdDecisionPolicy.isDescendantOf(it.path, region.path) }
            .flatMap { visibleNode -> visibleNode.controlLabels.map { AdCreativeRules.Text(it, visibleNode.bounds.toBox()) } },
          snapshot.nodes.filter { it.isVisible && it.className.endsWith("imageview") &&
            AdDecisionPolicy.isDescendantOf(it.path, region.path) }.map { it.bounds.toBox() },
        )
    }
    val ocrSplashAdContext = hasOcrBackedSplashAdContext(
      candidateText = candidateText,
      targetBounds = targetBounds,
      labelBounds = labelBounds,
      snapshot = snapshot,
      frame = ocrFrame,
    )
    val ocrMotionSplash = ocrSplashAdContext && ocrFrame != null &&
      snapshot.nodes.none { it.isVisible && it.isScrollable } &&
      AdCreativeRules.hasMotionRedirectSplashComposition(
        ocrFrame.compositionText,
        targetBounds.toBox(), snapshot.rootBounds.toBox(),
      )
    val verifiedSplashAdContext = nodeSplashAdContext || caiyunRedirectSplashContext || ocrSplashAdContext
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
    if (verifiedSplashAdContext) score += 30

    if (plainLabel && !edgeSkip && !videoAdContext &&
      !ancestorHasAdSignal && !localStrongAdContext && !topCenterInterstitialContext &&
      !verifiedSplashAdContext
    ) return null
    if (strongLabel && !candidateText.contains("广告") && !candidateText.contains("ad") &&
      !videoAdContext && !localStrongAdContext && !ancestorHasAdSignal && !verifiedSplashAdContext
    ) return null
    if (countdownLabel && !verifiedSplashAdContext && !AdDecisionPolicy.allowCountdownSkip(
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
      !verifiedSplashAdContext
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
    if (verifiedSplashAdContext && !topCenterInterstitialContext) {
      ruleId = when {
        caiyunRedirectSplashContext -> "builtin.skip.splash.caiyun-redirect"
        nodeSplashAdContext -> "builtin.skip.splash.labelled-redirect"
        else -> "builtin.skip.splash.ocr-creative"
      }
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
      allowVerifiedTouchFallback = ocrSplashAdContext &&
        (AdGeometryRules.isBottomLeftSplashSkip(targetBounds.toBox(), snapshot.rootBounds.toBox()) ||
          ocrMotionSplash) &&
        ocrSkipLine != null && targetBounds.contains(ocrSkipLine.screenBounds) &&
        targetNode != null && !targetNode.isScrollable &&
        targetNode.controlLabels.all { label ->
          AdSemanticRules.isNeutralControlLabel(label) || isSkipOcrLabel(normalizeLabel(label))
        },
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
    val root = snapshot.rootBounds
    val topRight = isVeryNearTopRight(targetBounds, root)
    val bottomLeft = AdGeometryRules.isBottomLeftSplashSkip(targetBounds.toBox(), root.toBox())
    if (!isOcrScopeCurrent(currentFrame, snapshot, "0") ||
      !isSkipOcrLabel(normalizeLabel(candidateText)) ||
      (!topRight && !bottomLeft) ||
      !isCompactControl(targetBounds, root) ||
      !hasCompactSkipTarget(targetBounds, labelBounds, root)
    ) return false

    if (bottomLeft) {
      if (snapshot.nodes.any { it.isVisible && it.isScrollable }) return false
      val creative = snapshot.nodes.asSequence()
        .filter { isLargeSplashCreative(it, root) && it.bounds.contains(targetBounds.centerX(), targetBounds.centerY()) }
        .maxByOrNull { it.bounds.width().toLong() * it.bounds.height().toLong() }
        ?: return false
      val creativeLines = currentFrame.lines.filter {
        creative.bounds.contains(it.screenBounds.centerX(), it.screenBounds.centerY())
      }
      val sameCreativeAdMarker = creativeLines.any { hasStrongAdSignal(it.text) } ||
        hasStrongAdSignal(joinOcrLineText(creativeLines))
      return AdDecisionPolicy.allowOcrBackedBottomLeftSplashSkip(
        hasSkipLabel = true,
        compactBottomLeft = true,
        hasLargeCreative = true,
        currentWindowEvidence = true,
        controlInsideCreative = creative.bounds.contains(targetBounds.centerX(), targetBounds.centerY()),
        hasExplicitAdMarker = sameCreativeAdMarker,
        hasPromotionalAction = hasOcrPromotionalAction(creativeLines, includeSplashActions = true),
      )
    }

    val splashTexts = currentFrame.compositionText
    val mobileTrafficCardSplash = AdCreativeRules.hasMobileTrafficCardSplashComposition(
      snapshot.packageName, splashTexts, targetBounds.toBox(), snapshot.rootBounds.toBox())
    val redirectSplash = mobileTrafficCardSplash || (
      snapshot.nodes.none { it.isVisible && it.isScrollable } &&
        AdCreativeRules.hasRedirectSplashComposition(
          splashTexts, targetBounds.toBox(), snapshot.rootBounds.toBox())
      )
    if (!hasLargeSplashCreative(snapshot) && !redirectSplash) return false
    val explicitAdMarker = mobileTrafficCardSplash || currentFrame.lines.any { hasStrongAdSignal(it.text) } ||
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
    return snapshot.nodes.any { isLargeSplashCreative(it, snapshot.rootBounds) }
  }

  private fun isLargeSplashCreative(node: ScreenNode, root: Rect): Boolean {
    if (!node.isVisible || node.depth == 0) return false
    if (!isVisualCreativeNode(node)) return false
    val rootArea = root.width().toLong().coerceAtLeast(1L) * root.height().toLong().coerceAtLeast(1L)
    val area = node.bounds.width().toLong().coerceAtLeast(0L) * node.bounds.height().toLong().coerceAtLeast(0L)
    return node.bounds.width() >= root.width() * MIN_SPLASH_CREATIVE_WIDTH_RATIO &&
      node.bounds.height() >= root.height() * MIN_SPLASH_CREATIVE_HEIGHT_RATIO &&
      area.toDouble() / rootArea >= MIN_SPLASH_CREATIVE_AREA_RATIO
  }

  private fun isVisualCreativeNode(node: ScreenNode): Boolean =
    node.className.contains("imageview") || node.className.contains("surfaceview") ||
      node.className.contains("textureview") || node.className.contains("webview")

  private fun hasOcrBackedFloatingCreativeCloseContext(
    node: ScreenNode,
    targetBounds: Rect,
    hasCloseSemantic: Boolean,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): Boolean {
    val currentFrame = frame ?: return false
    val root = snapshot.rootBounds
    if (!hasCloseSemantic || !isOcrScopeCurrent(currentFrame, snapshot, "0") ||
      !isNearOcrPopupScreenEdge(targetBounds, root)
    ) return false

    val rootBox = root.toBox()
    val visualCreatives = snapshot.nodes.asSequence()
      .filter { it.isVisible && isVisualCreativeNode(it) }
      .map { it.bounds }
    val overlayCreatives = node.ancestorRegions.asSequence()
      .filter { region ->
        !isScrollingRegion(region) &&
          (isOverlayLayer(region) || isPotentialOcrAdContainer(region, root))
      }
      .map { it.bounds }
    val promoMarkers = (COMMERCIAL_AD_MARKERS + OCR_PROMOTIONAL_OFFER_MARKERS + POPUP_MARKERS).distinct()

    return (visualCreatives + overlayCreatives).distinct().any { creativeBounds ->
      if (!AdGeometryRules.isOcrFloatingCreative(creativeBounds.toBox(), rootBox) ||
        !AdGeometryRules.isCompactFloatingCreativeClose(
          targetBounds.toBox(), creativeBounds.toBox(), rootBox,
        )
      ) return@any false

      val creativeLines = currentFrame.lines.filter { line ->
        creativeBounds.contains(line.screenBounds.centerX(), line.screenBounds.centerY())
      }
      val explicitAdMarker = creativeLines.any { hasStrongAdSignal(it.text) } ||
        hasStrongAdSignal(joinOcrLineText(creativeLines))
      val promotionalCluster = countDistinctOcrMarkers(creativeLines, promoMarkers) >= 2 &&
        hasOcrPromotionalAction(creativeLines, includeSplashActions = true)
      AdDecisionPolicy.allowOcrBackedFloatingCreativeClose(
        hasCloseSemantic = true,
        compactCreativeEdgeTarget = true,
        currentWindowEvidence = true,
        sameCreativeEvidence = creativeLines.isNotEmpty(),
        hasAdMarkerOrPromoCluster = explicitAdMarker || promotionalCluster,
      )
    }
  }

  private fun hasOcrBackedFullScreenAdCloseContext(
    targetBounds: Rect,
    closeSemantic: Boolean,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): Boolean {
    val currentFrame = frame ?: return false
    if (!isOcrScopeCurrent(currentFrame, snapshot, "0")) return false
    val root = snapshot.rootBounds
    if (!isVeryNearTopRight(targetBounds, root) ||
      !AdGeometryRules.isCompactFullScreenAdClose(targetBounds.toBox(), root.toBox())
    ) return false

    val creative = snapshot.nodes.asSequence()
      .filter { isLargeSplashCreative(it, root) && it.bounds.contains(targetBounds.centerX(), targetBounds.centerY()) }
      .maxByOrNull { it.bounds.width().toLong() * it.bounds.height().toLong() }
      ?: return false
    val creativeLines = currentFrame.lines.filter {
      creative.bounds.contains(it.screenBounds.centerX(), it.screenBounds.centerY())
    }
    val hasAdMarker = creativeLines.any { hasStrongAdSignal(it.text) }
    return AdDecisionPolicy.allowOcrBackedFullScreenAdClose(
      hasCloseSemantic = closeSemantic,
      compactTopRightControl = true,
      currentWindowEvidence = true,
      controlBelongsToLargeCreative = true,
      hasExplicitAdMarker = hasAdMarker,
    )
  }

  private fun hasTopCenterThirdPartyInterstitialContext(
    targetBounds: Rect,
    labelBounds: Rect,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): Boolean {
    if (frame == null || !isOcrScopeCurrent(frame, snapshot, "0") ||
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
          !isScrollingRegion(region)
      }
      .any { region ->
        snapshot.nodes.any { candidate ->
          candidate.isVisible && candidate.path != node.path &&
            AdDecisionPolicy.isDescendantOf(candidate.path, region.path) &&
            (hasVideoAdSignal(candidate.label) ||
              VIDEO_AD_RESOURCE_MARKERS.any(candidate.resourceId::contains))
        } || frame?.takeIf { isOcrScopeCurrent(it, snapshot, region.path) }?.lines?.any { line ->
          region.bounds.contains(line.screenBounds.centerX(), line.screenBounds.centerY()) &&
            hasVideoAdSignal(line.text)
        } == true
      }
  }

  private fun findPopupCloseCandidate(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): DetectionAction? {
    if (!store.isSettingEnabled(ClearScreenStore.KEY_CLOSE_POPUP_ADS, true)) return null
    if (!node.isVisible || !node.isEnabled) return null
    val ocrFrame = frame?.takeIf { isOcrScopeCurrent(it, snapshot, node.path) }
    if (AdSemanticRules.selectCloseLabel(node.controlLabels) == null &&
      !hasGenericCloseResourceId(node.resourceId) &&
      ocrFrame?.closeGlyphText?.none { node.bounds.contains(it.screenBounds.centerX(), it.screenBounds.centerY()) } != false
    ) return null
    val target = clickableTarget(node.node) ?: return null
    if (!target.isVisibleToUser || !target.isEnabled) return null

    val compact = AdSemanticRules.selectCloseLabel(node.controlLabels) ?: node.compactLabel
    val resourceId = node.resourceId
    val closeLabel = isPopupCloseLabel(compact)
    val targetResourceId = target.viewIdResourceName.orEmpty().lowercase(Locale.ROOT)
    val explicitAdResource = hasAdCloseResourceId(resourceId) ||
      hasAdCloseResourceId(targetResourceId)

    val targetBounds = Rect().also(target::getBoundsInScreen)
    if (!hasUsableBounds(targetBounds)) return null
    val independentCloseControl = isIndependentCloseTarget(targetBounds, snapshot.rootBounds)
    val possibleFullScreenCloseControl = isVeryNearTopRight(targetBounds, snapshot.rootBounds) &&
      AdGeometryRules.isCompactFullScreenAdClose(targetBounds.toBox(), snapshot.rootBounds.toBox())
    if (!independentCloseControl && !possibleFullScreenCloseControl) return null
    findKnownWifiPromotionCloseCandidate(node, target, targetBounds, snapshot, ocrFrame)?.let { return it }
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
    val mobilePosterEvidence = findKnownMobilePosterAdEvidence(node, target, snapshot, ocrFrame)
    val nearTopRight = isNearTopRight(targetBounds, snapshot.rootBounds)
    val nearPopupEdge = isNearPopupEdge(targetBounds, snapshot.rootBounds)
    val compactControl = isIndependentCloseTarget(targetBounds, snapshot.rootBounds)
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
    val localStrongAdContext = explicitAdResource ||
      hasStrongAdSignal(node.label, resourceId) ||
      node.ancestorRegions.any { region ->
        isLocalAdRegion(region, snapshot.rootBounds) &&
          hasStrongAdSignal(region.label, region.resourceId)
      }
    val fullScreenAdContext = hasFullScreenAdEvidenceWithinOverlay(node, snapshot)
    val ocrOverlayContext = hasOcrOverlayEvidence(node, targetBounds, snapshot, ocrFrame)
    val ocrFullScreenAdContext = hasOcrBackedFullScreenAdCloseContext(
      targetBounds, closeLabel || explicitAdResource || unlabeledEdgeClose, snapshot, ocrFrame,
    )
    val ocrGlyphCloseSemantic = ocrFrame?.closeGlyphText?.any {
      isCompactOcrCloseGlyph(it, targetBounds, snapshot.rootBounds)
    } == true
    val ocrFloatingCreativeContext = hasOcrBackedFloatingCreativeCloseContext(
      node = node,
      targetBounds = targetBounds,
      hasCloseSemantic = closeLabel || explicitAdResource || unlabeledEdgeClose || ocrGlyphCloseSemantic,
      snapshot = snapshot,
      frame = ocrFrame,
    )
    val belongsToAdOverlay = node.ancestorRegions.any { region ->
      region.path != "0" && isLocalAdRegion(region, snapshot.rootBounds) && isOverlayLayer(region)
    }
    if (!closeLabel && !explicitAdResource && !unlabeledEdgeClose &&
      centeredPosterEvidence == null && caiyunInterstitialEvidence == null && mobilePosterEvidence == null
    ) return null
    val genericCloseLabel = isGenericPopupCloseLabel(compact)

    // Do not use the whole screen's commercial/benefit text as evidence. A
    // normal dialog can contain words such as "会员" or "福利" elsewhere.
    // Require an ad marker on this control, its ancestors, or a separately
    // verified full-screen ad context. The latter is deliberately stricter:
    // it needs a large overlay/panel plus a cluster of ad/commercial labels.
    if (!localStrongAdContext && !fullScreenAdContext && !ocrOverlayContext && !ocrFullScreenAdContext &&
      !ocrFloatingCreativeContext &&
      centeredPosterEvidence == null && caiyunInterstitialEvidence == null && mobilePosterEvidence == null
    ) return null
    if (!belongsToAdOverlay && !fullScreenAdContext && !ocrOverlayContext && !ocrFullScreenAdContext &&
      !ocrFloatingCreativeContext &&
      !explicitAdResource && centeredPosterEvidence == null && caiyunInterstitialEvidence == null && mobilePosterEvidence == null
    ) return null
    if ((!nearPopupEdge && !ocrFloatingCreativeContext) ||
      (!compactControl && !ocrFullScreenAdContext && !ocrFloatingCreativeContext)
    ) return null

    // Generic "关闭"/X controls are only safe in the conventional ad position.
    // Ad-specific labels such as "关闭广告" may also appear on the popup edge.
    if (genericCloseLabel && !nearTopRight && !explicitAdResource && !ocrOverlayContext &&
      !ocrFloatingCreativeContext
    ) return null
    if (unlabeledEdgeClose && !nearPopupEdge && centeredPosterEvidence == null &&
      caiyunInterstitialEvidence == null
    ) return null

    var score = when {
      mobilePosterEvidence != null -> 142
      explicitAdResource -> 124
      caiyunInterstitialEvidence != null -> 122
      centeredPosterEvidence != null -> 118
      isAdSpecificCloseLabel(compact) -> 114
      ocrFloatingCreativeContext -> 112
      ocrFullScreenAdContext -> 112
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
      minimumScore = if (explicitAdResource || centeredPosterEvidence != null || ocrFullScreenAdContext ||
        ocrFloatingCreativeContext ||
        caiyunInterstitialEvidence != null || mobilePosterEvidence != null
      ) 120 else 112,
      actionPriority = 1,
      ruleId = when {
        mobilePosterEvidence != null -> "builtin.popup.close.mobile-campaign-carousel"
        explicitAdResource -> "builtin.popup.close.resource-id"
        caiyunInterstitialEvidence != null -> "builtin.popup.close.caiyun-interstitial.bottom-center"
        centeredPosterEvidence != null -> "builtin.popup.close.centered-poster"
        ocrFloatingCreativeContext -> "builtin.popup.close.ocr-floating-creative"
        ocrFullScreenAdContext -> "builtin.popup.close.ocr-fullscreen-creative"
        ocrOverlayContext -> "builtin.popup.close.ocr-overlay"
        fullScreenAdContext -> "builtin.popup.close.fullscreen-ad"
        else -> "builtin.popup.close.context"
      },
      label = node.label.ifBlank { node.className }.take(MAX_LOG_LABEL_LENGTH),
      successText = "已关闭弹窗广告",
      category = DetectionCategory.POPUP,
      evidenceFingerprint = mobilePosterEvidence ?: caiyunInterstitialEvidence ?: centeredPosterEvidence ?: ocrFrame?.lines
        ?.filter { isAdRelatedOcrText(it.text) }
        ?.joinToString("|") { it.compactText }?.take(240).orEmpty(),
    )
  }

  /** Match the observed SDK creatives only in their own stable, exclusive subtree. */
  private fun findKnownWifiPromotionCloseCandidate(
    node: ScreenNode,
    target: AccessibilityNodeInfo,
    targetBounds: Rect,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): DetectionAction? {
    if (!WifiPromotionRules.isSupportedHost(snapshot.packageName) || target != node.node ||
      !node.node.isClickable || !node.isVisible || !node.isEnabled || node.isScrollable ||
      !node.controlLabels.all {
        AdSemanticRules.isNeutralControlLabel(it) || AdSemanticRules.selectCloseLabel(listOf(it)) != null
      }
    ) return null
    val currentFrame = frame ?: return null
    if (!isOcrWindowCurrent(currentFrame, snapshot)) return null
    // An ID, the screen corner or a circle does not prove that this is an X.
    if (currentFrame.closeGlyphText.none {
        isCompactOcrCloseGlyph(it, targetBounds, snapshot.rootBounds)
      }
    ) return null

    for (region in node.ancestorRegions.sortedBy { it.bounds.width().toLong() * it.bounds.height() }) {
      if (isScrollingRegion(region) || !region.bounds.contains(targetBounds) ||
        !isOcrScopeCurrent(currentFrame, snapshot, region.path)
      ) continue
      val localNodes = snapshot.nodes.filter {
        it.isVisible && (it.path == region.path || AdDecisionPolicy.isDescendantOf(it.path, region.path))
      }
      if (localNodes.any { it.isScrollable || it.isEditable || it.isPassword } ||
        localNodes.any { it.controlLabels.any(AdSemanticRules::isSensitiveUiContext) }
      ) continue
      val texts = (currentFrame.lines + currentFrame.elements.filter { isExplicitAdBadge(it.compactText) })
        .distinctBy { it.compactText to it.screenBounds }
        .map { AdCreativeRules.Text(it.text, it.screenBounds.toBox()) }
      val creatives = localNodes.asSequence()
        .filter { candidate ->
          candidate.node != target && hasUsableBounds(candidate.bounds) &&
            (isVisualCreativeNode(candidate) || (
              candidate.path != region.path &&
                localNodes.any { child -> isVisualCreativeNode(child) &&
                  AdDecisionPolicy.isDescendantOf(child.path, candidate.path) }
              ))
        }
        .distinctBy { it.bounds }
      for (creative in creatives) {
        val kind = WifiPromotionRules.classify(
          snapshot.packageName, snapshot.rootBounds.toBox(), region.bounds.toBox(),
          creative.bounds.toBox(), targetBounds.toBox(), texts,
        ) ?: continue
        // The SDK's caption may be a sibling just below the banner. In that
        // case validate the whole window as well, not only the button subtree.
        val evidenceScope = if (kind == WifiPromotionRules.Kind.EXTERNAL_FLOATING_BANNER) "0" else region.path
        if (!isOcrScopeCurrent(currentFrame, snapshot, evidenceScope)) continue
        return DetectionAction(
          target = target,
          targetBounds = targetBounds,
          score = if (kind == WifiPromotionRules.Kind.VIDEO_POSTER) 184 else 164,
          minimumScore = 150,
          actionPriority = 1,
          ruleId = kind.ruleId,
          label = "关闭促销浮层",
          successText = "已关闭促销浮层",
          category = if (kind == WifiPromotionRules.Kind.CALENDAR_DOWNLOAD_CARD) DetectionCategory.FEED else DetectionCategory.POPUP,
          evidenceFingerprint = "${region.path}|${kind.name}",
          allowVerifiedTouchFallback = true,
          touchEvidenceScope = evidenceScope,
        )
      }
    }
    return null
  }

  private fun findFeedCloseCandidate(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): DetectionAction? {
    if (!store.isSettingEnabled(ClearScreenStore.KEY_CLOSE_FEED_ADS, true)) return null
    val ocrFrame = frame?.takeIf { isOcrScopeCurrent(it, snapshot, node.path) }
    if (ocrFrame == null || !node.isEnabled || !node.isVisible) return null
    val target = clickableTarget(node.node) ?: return null
    if (!target.isVisibleToUser || !target.isEnabled) return null

    val targetBounds = Rect().also(target::getBoundsInScreen)
    if (!hasUsableBounds(targetBounds)) return null
    val compact = AdSemanticRules.selectCloseLabel(node.controlLabels) ?: node.compactLabel
    val ocrCloseLine = ocrFrame?.closeGlyphText?.firstOrNull { line ->
      isCompactOcrCloseGlyph(line, targetBounds, snapshot.rootBounds)
    }
    val adSpecificClose = isAdSpecificCloseLabel(compact) ||
      hasAdCloseResourceId(node.resourceId) ||
      hasAdCloseResourceId(target.viewIdResourceName.orEmpty().lowercase(Locale.ROOT))
    val genericClose = isGenericPopupCloseLabel(compact) ||
      isLikelyUnlabeledCloseIcon(node, target) ||
      (node.controlLabels.all(AdSemanticRules::isNeutralControlLabel) && ocrCloseLine != null)
    if (!adSpecificClose && !genericClose) return null

    val card = node.ancestorRegions.asSequence()
      .filter { region ->
        isPotentialFeedCard(region, node.bounds, targetBounds, snapshot.rootBounds) ||
          isPotentialOcrAdBanner(region, snapshot.rootBounds, targetBounds)
      }
      .mapNotNull { region ->
        val evidence = findFeedAdEvidence(region, snapshot, ocrFrame) ?: return@mapNotNull null
        val horizontalBanner = isPotentialOcrAdBanner(region, snapshot.rootBounds, targetBounds)
        if (horizontalBanner && (!evidence.explicitAdMarker || !evidence.hasCallToAction ||
            target != node.node || !isUninterruptedAdCardScope(region, snapshot) || ocrCloseLine == null ||
            !AdGeometryRules.isBannerCloseTarget(
              targetBounds.toBox(), ocrCloseLine.screenBounds.toBox(), region.bounds.toBox(),
              snapshot.rootBounds.toBox(),
            )
          )
        ) return@mapNotNull null
        if (ocrCloseLine != null && (!(evidence.explicitAdMarker || evidence.downloadDisclosure) ||
            !region.bounds.contains(ocrCloseLine.screenBounds.centerX(), ocrCloseLine.screenBounds.centerY())
          )
        ) return@mapNotNull null
        val verifiedSideClose = evidence.explicitAdMarker && evidence.hasCallToAction &&
          target == node.node && !node.isScrollable && ocrCloseLine != null &&
          isUninterruptedAdCardScope(region, snapshot) &&
          AdGeometryRules.isRightEdgeAdCardClose(
            targetBounds.toBox(), ocrCloseLine.screenBounds.toBox(), region.bounds.toBox(),
            snapshot.rootBounds.toBox(),
          )
        if (!horizontalBanner && !verifiedSideClose && !isCloseAtCardEdge(
            node = node,
            targetBounds = targetBounds,
            cardBounds = region.bounds,
            adSpecificClose = adSpecificClose,
            closeVisualBounds = ocrCloseLine?.screenBounds,
          )
        ) return@mapNotNull null
        Pair(region, evidence)
      }
      .minByOrNull { it.first.bounds.width().toLong() * it.first.bounds.height().toLong() }
      ?: return null

    val region = card.first
    val targetArea = targetBounds.width().toLong() * targetBounds.height().toLong()
    val regionArea = region.bounds.width().toLong().coerceAtLeast(1L) *
      region.bounds.height().toLong().coerceAtLeast(1L)
    val horizontalBanner = isPotentialOcrAdBanner(region, snapshot.rootBounds, targetBounds)
    // A row's 48dp touch area can be nearly as tall as the row. Its glyph and
    // screen-relative limits were checked above; a percentage of card height
    // would reject valid controls even on an explicitly marked ad row.
    if (!horizontalBanner) {
      if (targetArea > regionArea * MAX_FEED_CLOSE_TARGET_AREA_RATIO) return null
      if (targetBounds.width() > region.bounds.width() * MAX_FEED_CLOSE_TARGET_WIDTH_RATIO ||
        targetBounds.height() > region.bounds.height() * MAX_FEED_CLOSE_TARGET_HEIGHT_RATIO
      ) return null
    }
    val rightEdgeClose = !isCloseAtCardEdge(node, targetBounds, region.bounds, adSpecificClose,
      ocrCloseLine?.screenBounds)

    return DetectionAction(
      target = target,
      targetBounds = targetBounds,
      score = if (card.second.explicitAdMarker || card.second.downloadDisclosure) 152 else 132,
      minimumScore = 120,
      actionPriority = 1,
      ruleId = when {
        card.second.downloadDisclosure -> "builtin.feed.close.download-disclosure"
        rightEdgeClose -> "builtin.feed.close.ocr-right-edge"
        else -> "builtin.feed.close.ocr-card"
      },
      label = node.label.ifBlank { node.className }.take(MAX_LOG_LABEL_LENGTH),
      successText = "已关闭信息流广告",
      category = DetectionCategory.FEED,
      evidenceFingerprint = "${region.resourceId}|${card.second.fingerprint}".take(300),
    )
  }

  /** A small, independently clickable X on an explicitly marked ad card.
   * Covers player pause overlays and inline banners when the app exposes both
   * the ad badge and close control in the same accessibility subtree.
   */
  private fun findEmbeddedAdCloseCandidate(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): DetectionAction? {
    if (!store.isSettingEnabled(ClearScreenStore.KEY_CLOSE_POPUP_ADS, true) ||
      !node.isVisible || !node.isEnabled || !node.node.isClickable
    ) return null
    val ocrFrame = frame?.takeIf { isOcrScopeCurrent(it, snapshot, node.path) }
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
          !isScrollingRegion(region)
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
        val glyph = ocrFrame?.closeGlyphText?.firstOrNull {
          isCompactOcrCloseGlyph(it, targetBounds, snapshot.rootBounds)
        }
        val sideClose = if (ocrFrame != null && glyph != null && isUninterruptedAdCardScope(region, snapshot) &&
          AdGeometryRules.isRightEdgeAdCardClose(targetBounds.toBox(), glyph.screenBounds.toBox(),
            region.bounds.toBox(), snapshot.rootBounds.toBox())
        ) {
          findFeedAdEvidence(region, snapshot, ocrFrame)?.let { it.explicitAdMarker && it.hasCallToAction } == true
        } else false
        val allowed = AdDecisionPolicy.allowEmbeddedAdClose(
          directClickable = true,
          sameSubtree = AdDecisionPolicy.isDescendantOf(badge.path, region.path),
          explicitAdBadge = true,
          closeSemantic = closeSemantic,
          atVerifiedCardEdge = AdDecisionPolicy.isVerifiedCardEdge(x, y, explicitAdClose) || sideClose,
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

  private fun findKnownMobilePosterAdEvidence(
    node: ScreenNode,
    target: AccessibilityNodeInfo,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): String? {
    if (snapshot.packageName != "com.greenpoint.android.mc10086.activity" || frame == null ||
      target != node.node || !node.node.isClickable || !isOcrWindowCurrent(frame, snapshot)
    ) return null
    val regions = node.ancestorRegions.sortedBy { it.bounds.width().toLong() * it.bounds.height() }
    for (region in regions) {
      if (isScrollingRegion(region) || !region.bounds.contains(node.bounds) ||
        !isOcrScopeCurrent(frame, snapshot, region.path)
      ) continue
      val localNodes = snapshot.nodes.filter {
        it.isVisible && (it.path == region.path || AdDecisionPolicy.isDescendantOf(it.path, region.path))
      }
      // Do not borrow the underlying store page's carousel or marketing copy.
      if (localNodes.any { it.isScrollable || it.isEditable }) continue
      val texts = frame.lines.filter { region.bounds.contains(it.screenBounds) }
        .map { AdCreativeRules.Text(it.text, it.screenBounds.toBox()) } +
        localNodes.flatMap { local -> local.controlLabels.map { AdCreativeRules.Text(it, local.bounds.toBox()) } }
      val posters = localNodes.filter {
        it.path != node.path && (it.className.contains("imageview") || it.className.contains("webview"))
      }
      for (poster in posters) {
        val posterTexts = texts.filter { poster.bounds.contains(it.bounds.centerX, it.bounds.centerY) }.map { it.value }
        if (AdCreativeRules.hasMobileCampaignPosterComposition(
            snapshot.packageName, poster.bounds.toBox(), node.bounds.toBox(), snapshot.rootBounds.toBox(),
            posterTexts, texts,
          )
        ) return "${region.path}|${poster.resourceId}|mobile-video-benefit-carousel"
      }
    }
    return null
  }

  private fun hasOcrOverlayEvidence(
    node: ScreenNode,
    targetBounds: Rect,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame?,
  ): Boolean {
    if (frame == null || !isOcrWindowCurrent(frame, snapshot)) return false
    if (!isNearOcrPopupScreenEdge(targetBounds, snapshot.rootBounds)) return false

    // OCR may authorize a generic X only from the same overlay subtree. A
    // separate feed banner elsewhere on the page must never authorize this X.
    val overlayRegion = node.ancestorRegions.asSequence()
      .filter { region ->
        (isOverlayLayer(region) || isPotentialOcrAdContainer(region, snapshot.rootBounds)) &&
          region.bounds.contains(targetBounds.centerX(), targetBounds.centerY()) &&
          isOcrScopeCurrent(frame, snapshot, region.path)
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
    if (region.path == "0" || isScrollingRegion(region) ||
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
    if (!isOcrWindowCurrent(currentFrame, snapshot)) return null

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
           isOcrScopeCurrent(currentFrame, snapshot, poster.ancestorRegions.last().path) &&
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
      (!AdCreativeRules.isCaiyunInterstitialCloseId(packageName, node.resourceId) &&
        !AdCreativeRules.isCaiyunInterstitialCloseId(packageName, targetResourceId))
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
      targetResourceId,
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
      isScrollingRegion(region)
    ) return false
    val rootArea = rootBounds.width().toLong().coerceAtLeast(1L) *
      rootBounds.height().toLong().coerceAtLeast(1L)
    val regionArea = region.bounds.width().toLong() * region.bounds.height().toLong()
    val ratio = regionArea.toDouble() / rootArea
    return ratio in MIN_FEED_CARD_AREA_RATIO..MAX_FEED_CARD_AREA_RATIO &&
      region.bounds.width() >= rootBounds.width() * MIN_FEED_CARD_WIDTH_RATIO &&
      region.bounds.height() >= rootBounds.height() * MIN_FEED_CARD_HEIGHT_RATIO
  }

  private fun isPotentialOcrAdBanner(
    region: AncestorRegion,
    rootBounds: Rect,
    targetBounds: Rect,
  ): Boolean =
    region.path != "0" && !isScrollingRegion(region) && hasUsableBounds(region.bounds) &&
      region.bounds.contains(targetBounds.centerX(), targetBounds.centerY()) &&
      AdGeometryRules.isHorizontalAdBanner(region.bounds.toBox(), rootBounds.toBox())

  /** Do not borrow an inline ad's evidence for controls in an enclosing list or form. */
  private fun isUninterruptedAdCardScope(region: AncestorRegion, snapshot: AccessibilitySnapshot): Boolean =
    region.path != "0" && snapshot.nodes.none { local ->
      local.isVisible && (local.path == region.path || AdDecisionPolicy.isDescendantOf(local.path, region.path)) &&
        (local.isScrollable || SCROLL_CONTAINER_CLASSES.any(local.className::contains) ||
          local.isEditable || local.isPassword || local.controlLabels.any(AdSemanticRules::isSensitiveUiContext))
    }

  private fun findFeedAdEvidence(
    region: AncestorRegion,
    snapshot: AccessibilitySnapshot,
    frame: OcrFrame,
  ): FeedAdEvidence? {
    if (!isOcrScopeCurrent(frame, snapshot, region.path)) return null
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
    val hasCallToAction = hasOcrPromotionalAction(ocrLines) ||
      treeLabels.any { hasPromotionalCallToActionText(it) }
    // The supplied Caiyun cards have an app-name/version/developer disclosure
    // and an explicit download CTA, but no standalone ad badge. Keep this
    // exception confined to the observed host and this same bounded card.
    val downloadDisclosure = AdCreativeRules.hasDownloadDisclosure(
      snapshot.packageName, ocrLines.map { it.text } + treeLabels,
    )
    // Prefer an explicit in-card marker (for example the "推荐广告" badge in
    // the weather-page sample). Without it, require three distinct commercial
    // signals plus a call to action, all still inside this same card.
    if (!explicitAdMarker && !downloadDisclosure &&
      (commercialSignals < MIN_FEED_COMMERCIAL_SIGNALS || !hasCallToAction)
    ) return null

    val fingerprint = ocrLines.asSequence().map { it.compactText }
      .filter { it.isNotBlank() }
      .distinct()
      .take(MAX_FEED_FINGERPRINT_LINES)
      .joinToString(";")
    return FeedAdEvidence(explicitAdMarker, downloadDisclosure, hasCallToAction,
      fingerprint.ifBlank { "accessible-ad-marker" })
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
      !isScrollingRegion(region)

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

  private fun appendActionEvent(
    packageName: String,
    action: DetectionAction,
    result: String,
    text: String,
  ) {
    Log.i(TAG, "ad-result package=$packageName rule=${action.ruleId} result=$result")
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
    val windowId: Int,
    val ocrNodeStates: Map<String, OcrNodeIdentity>,
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
          // Offscreen forecast charts can expose hundreds of descendants. A
          // hidden subtree cannot provide a currently clickable ad control.
          if (current.depth > 0 && !node.isVisibleToUser) continue
          val controlLabels = readNodeLabels(node)
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
            controlLabels = controlLabels,
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
            isScrollable = node.isScrollable,
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
            isScrollable = node.isScrollable,
          )).takeLast(MAX_ANCESTOR_REGIONS)
          if (current.depth >= MAX_TREE_DEPTH) continue
          for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
              pending.add(PendingNode(child, "${current.path}.$index", current.depth + 1,
                nextAncestors, nextAncestorRegions))
            }
          }
        }

        val countdownSkipPaths = nodes.asSequence().filter { owner ->
          owner.isVisible && owner.node.isClickable && !owner.isScrollable &&
            owner.bounds.width() <= rootBounds.width() * 0.52 &&
            owner.bounds.height() <= rootBounds.height() * 0.20 &&
            owner.bounds.width().toLong() * owner.bounds.height() <=
              rootBounds.width().toLong() * rootBounds.height() * 0.065
        }.filter { owner ->
          val labels = nodes.asSequence()
            .filter { it.path == owner.path || AdDecisionPolicy.isDescendantOf(it.path, owner.path) }
            .flatMap { it.controlLabels.asSequence() }.toList()
          AdSemanticRules.selectSkipLabel(labels) != null && !AdSemanticRules.hasPendingSkipHint(labels)
        }.map { it.path }.toSet()
        return AccessibilitySnapshot(
          nodes = nodes,
          rootBounds = rootBounds,
          packageName = root.packageName?.toString().orEmpty(),
          windowId = root.windowId,
          ocrNodeStates = nodes.asSequence().filter { it.isVisible }.associate { node ->
            node.path to OcrNodeIdentity(
              node.node.hashCode(), node.resourceId, node.className,
              AdGeometryRules.Box(node.bounds.left, node.bounds.top, node.bounds.right, node.bounds.bottom),
              node.controlLabels.map { label -> AdSemanticRules.ocrEvidenceLabel(label,
                countdownSkipPaths.any { node.path == it || AdDecisionPolicy.isDescendantOf(node.path, it) }) },
              node.isEnabled || countdownSkipPaths.any {
                node.path == it || AdDecisionPolicy.isDescendantOf(node.path, it)
              }, node.node.isClickable, node.isEditable, node.isPassword,
              node.isFocused, node.isScrollable,
            )
          },
          signature = ("${root.packageName}:${root.windowId}:$rootBounds|" + nodes.asSequence()
            .filter { it.isVisible }
            .joinToString("|") { node ->
              "${node.path}:${node.compactLabel}:${node.resourceId}:${node.className}:" +
                "${node.isEnabled},${node.node.isClickable},${node.isEditable},${node.isPassword},${node.isFocused}:" +
                "${node.bounds.left},${node.bounds.top},${node.bounds.right},${node.bounds.bottom}"
            })
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
    val controlLabels: List<String>,
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
    val isScrollable: Boolean,
    val depth: Int,
  )

  private data class AncestorRegion(
    val path: String,
    val label: String,
    val compactLabel: String,
    val resourceId: String,
    val className: String,
    val bounds: Rect,
    val isScrollable: Boolean,
  )

  private data class FeedAdEvidence(
    val explicitAdMarker: Boolean,
    val downloadDisclosure: Boolean,
    val hasCallToAction: Boolean,
    val fingerprint: String,
  )

  private data class OcrLine(
    val text: String,
    val compactText: String,
    val screenBounds: Rect,
  )

  private data class OcrVisualButton(val label: OcrLine, val bounds: Rect)

  private data class UnifiedAdScope(
    val path: String,
    val bounds: Rect,
    val evidence: AdControlEvidenceRules.Evidence,
    val overlay: Boolean,
    val stamp: Int,
  )

  private data class PendingVisualAction(
    val packageName: String,
    val windowId: Int,
    val rootBounds: Rect,
    val displayBounds: Rect,
    val button: Rect,
    val scope: Rect,
    val signature: String,
    val at: Long,
  )

  private data class OcrNodeIdentity(
    val sourceIdentity: Int,
    val resourceId: String,
    val className: String,
    val bounds: AdGeometryRules.Box,
    val labels: List<String>,
    val enabled: Boolean,
    val clickable: Boolean,
    val editable: Boolean,
    val password: Boolean,
    val focused: Boolean,
    val scrollable: Boolean,
  )

  private data class OcrFrame(
    val packageName: String,
    val capturedAt: Long,
    val windowId: Int,
    val rootBounds: Rect,
    val displayBounds: Rect,
    val nodeStates: Map<String, OcrNodeIdentity>,
    val lines: List<OcrLine>,
    val elements: List<OcrLine>,
    val visualCloseGlyphs: List<OcrLine>,
    val capturedUptime: Long,
    val visualButtons: List<OcrVisualButton> = emptyList(),
    val rasterCreative: Boolean = false,
  ) {
    // A top-right ad badge and countdown can share one OCR line. Preserve the
    // separate word boxes so the badge is not lost as "广告跳过3".
    val compositionText: List<AdCreativeRules.Text> by lazy {
      (elements + lines).distinctBy { it.compactText to it.screenBounds }
        .map { AdCreativeRules.Text(it.text, AdGeometryRules.Box(
          it.screenBounds.left, it.screenBounds.top, it.screenBounds.right, it.screenBounds.bottom,
        )) }
    }
    val skipText: List<OcrLine> by lazy {
      (elements + lines).filter { isSkipOcrLabel(it.compactText) }
    }
    val closeGlyphText: List<OcrLine> by lazy {
      (elements + lines + visualCloseGlyphs).filter { it.compactText in OCR_CLOSE_GLYPH_LABELS }
        .distinctBy { it.compactText to it.screenBounds }
    }
    val controlText: List<OcrLine> by lazy { ocrControlText(lines, elements) +
      visualCloseGlyphs.filter { glyph -> lines.none {
        Rect.intersects(it.screenBounds, glyph.screenBounds) && AdSemanticRules.isBlockedControlLabel(it.text)
      } }
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
    val screenSignature: String = "",
    val targetWindowId: Int = target.windowId,
    val targetResourceId: String = target.viewIdResourceName.orEmpty(),
    val targetClassName: String = target.className?.toString().orEmpty(),
    val targetLabels: List<String> = readNodeLabels(target).map {
      AdSemanticRules.ocrEvidenceLabel(it, category == DetectionCategory.SPLASH_VIDEO)
    },
    val allowVerifiedTouchFallback: Boolean = false,
    val touchEvidenceScope: String = "0",
    val foregroundOverlay: Boolean = false,
  ) {
    // Changing countdowns, creative text or classifier rules must not reset a control's budget.
    fun targetFingerprint(packageName: String): String = buildString {
      append(packageName)
      append('|')
      append(targetWindowId)
      append('|')
      append(targetResourceId)
      append('|')
      append(targetClassName)
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
    private const val UNIFIED_CONTROL_RULE = "builtin.ad.close.unified-evidence"
    private const val VISUAL_CONTROL_RULE = "builtin.ad.close.visual-confirmed"

    /** Preserve word boxes and join only adjacent pieces of the same OCR line. */
    private fun ocrControlText(lines: List<OcrLine>, elements: List<OcrLine>): List<OcrLine> {
      val result = mutableListOf<OcrLine>()
      for (line in lines) {
        if (AdSemanticRules.isBlockedControlLabel(line.text)) continue
        if (AdSemanticRules.controlLabel(listOf(line.text)) != null) result += line
        val words = elements.filter { line.screenBounds.contains(it.screenBounds) }
          .sortedBy { it.screenBounds.left }.take(40)
        for (start in words.indices) {
          val bounds = Rect(words[start].screenBounds)
          var text = ""
          for (end in start..minOf(start + 2, words.lastIndex)) {
            val word = words[end]
            if (end > start && (word.screenBounds.left - words[end - 1].screenBounds.right >
                maxOf(word.screenBounds.height(), words[end - 1].screenBounds.height()) * 0.8 ||
                kotlin.math.abs(word.screenBounds.centerY() - words[start].screenBounds.centerY()) >
                maxOf(word.screenBounds.height(), words[start].screenBounds.height()) * 0.5)
            ) break
            text += word.text
            bounds.union(word.screenBounds)
            if (AdSemanticRules.controlLabel(listOf(text)) != null) {
              result += OcrLine(text, normalizeLabel(text), Rect(bounds))
            }
          }
        }
      }
      return result.distinctBy { it.compactText to it.screenBounds }.take(120)
    }
    private const val MOBILE_TRAFFIC_CARD_PACKAGE = "com.greenpoint.android.mc10086.activity"
    private const val MOBILE_OCR_SKIP_RULE = "builtin.skip.splash.mobile-traffic-ocr-gesture"
    private const val OCR_SKIP_GESTURE_DURATION_MS = 90L
    private const val ACTION_COOLDOWN_MS = 480L
    private const val ACTION_VERIFY_DELAY_MS = 650L
    private const val INITIAL_FOREGROUND_SCAN_DELAY_MS = 600L
    private const val OCR_EXECUTOR_KEEP_ALIVE_SECONDS = 30L
    private const val MAX_CLICKABLE_ANCESTORS = 6
    private const val MAX_NODES_PER_SNAPSHOT = 1_200
    private const val MAX_TREE_DEPTH = 32
    private const val MAX_ANCESTOR_LABELS = 8
    private const val MAX_ANCESTOR_REGIONS = 12
    private const val MAX_LOG_LABEL_LENGTH = 80
    private const val OCR_MIN_INTERVAL_MS = 1_200L
    private const val OCR_SIGNATURE_RETRY_DELAY_MS = 1_350L
    private const val OCR_FRAME_MAX_AGE_MS = 2_500L
    private const val MOBILE_OCR_MAX_LONG_EDGE = 1_800
    private const val MOBILE_OCR_MAX_ACTION_AGE_MS = 1_800L
    private const val MAX_OCR_TEXT_ELEMENTS = 900
    private const val POST_ACTION_RESCAN_DELAY_MS = 850L
    private const val MAX_ACTION_FOLLOW_UPS = 2
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
      "点此领取",
      "点击到落地页",
      "点击卡片跳转",
      "戳我下载",
      "点击立即下载",
      "抢先下载",
      "安全下载",
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
      "立即前往",
      "开通会员",
      "详情请见",
      "了解详情",
      "了解更多",
    )
    private val OCR_PROMOTIONAL_OFFER_MARKERS = listOf(
      "免单",
      "超1分钟",
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
      "扭动手机开启详情",
      "互动跳转详情页或第三方应用",
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

    private fun readNodeLabels(node: AccessibilityNodeInfo): List<String> =
      listOfNotNull(
        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() },
      ).distinct()

    private fun readNodeLabel(node: AccessibilityNodeInfo): String = readNodeLabels(node).joinToString(" ")

    private fun normalizeLabel(value: String): String = AdSemanticRules.normalize(value)

    private fun isStrongSkipLabel(compact: String): Boolean =
      AdSemanticRules.isExplicitSkip(compact)

    private fun isCountdownSkipLabel(compact: String): Boolean =
      AdSemanticRules.isCountdownSkip(compact) && !AdSemanticRules.isBlockedSkipLabel(compact)

    private fun hasSkipResourceId(resourceId: String): Boolean =
      SKIP_RESOURCE_MARKERS.any(resourceId::contains)

    private fun hasAdCloseResourceId(resourceId: String): Boolean =
      AD_CLOSE_RESOURCE_MARKERS.any(resourceId::contains)

    private fun isPopupCloseLabel(compact: String): Boolean =
      AdSemanticRules.isClose(compact)

    private fun isAdSpecificCloseLabel(compact: String): Boolean =
      AdSemanticRules.isExplicitAdClose(compact)

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
      !node.isScrollable && isOverlayLayer(node.compactLabel, node.resourceId, node.className)

    private fun isOverlayLayer(region: AncestorRegion): Boolean =
      !isScrollingRegion(region) && isOverlayLayer(region.compactLabel, region.resourceId, region.className)

    private fun isScrollingRegion(region: AncestorRegion): Boolean =
      region.isScrollable || SCROLL_CONTAINER_CLASSES.any(region.className::contains)

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
      if (!node.controlLabels.all(AdSemanticRules::isNeutralControlLabel) ||
        !nearPopupEdge || !compactControl
      ) return false
      val className = (node.className + " " + target.className?.toString().orEmpty())
        .lowercase(Locale.ROOT)
      val closeResource = hasGenericCloseResourceId(node.resourceId) ||
        hasGenericCloseResourceId(targetResourceId)
      val iconClass = className.contains("imageview") ||
        className.contains("imagebutton") ||
        className.endsWith("button")
      val independentlyExposedGlyph = target == node.node && node.node.isClickable && !node.isScrollable
      return AdDecisionPolicy.allowUnlabeledClose(closeResource, (iconClass || independentlyExposedGlyph) && ocrCloseGlyph)
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
