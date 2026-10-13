package com.clearscreen.prototype.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoAdRulesTest {
  @Test fun coversTheFourRequestedAppsWithoutConfusingTencentChat() {
    val names = mapOf(
      "com.qiyi.video" to "爱奇艺",
      "com.tencent.qqlive" to "腾讯视频",
      "com.youku.phone" to "优酷",
      "com.hunantv.imgo.activity" to "芒果TV",
    )
    names.forEach { (pkg, name) ->
      assertTrue(pkg, VideoAdRules.isSupported(pkg))
      assertEquals(name, VideoAdRules.platformName(pkg))
    }
    assertFalse(VideoAdRules.isSupported("com.tencent.mm"))
    assertFalse(VideoAdRules.isSupported("com.android.deskclock"))
  }

  @Test fun explicitAdSkipAndCloseCanBeAnywhereInVideoPlayer() {
    val skip = listOf("跳过广告", "跳过此广告", "跳过贴片广告", "跳过视频广告",
      "跳过中插广告", "立即跳过广告", "Skip ad", "Skip this ad")
    for (text in skip) {
      assertEquals(text, VideoAdRules.Action.EXPLICIT_SKIP,
        VideoAdRules.classify(listOf(text), "", false, false))
    }
    for (text in listOf("关闭广告", "关闭此广告", "关闭贴片广告", "关闭视频广告", "Close ad")) {
      assertEquals(text, VideoAdRules.Action.EXPLICIT_CLOSE,
        VideoAdRules.classify(listOf(text), "", false, false))
    }
  }

  @Test fun plainSkipAndCountdownNeedNearbyAdEvidenceAndEdgePlacement() {
    assertEquals(VideoAdRules.Action.NONE,
      VideoAdRules.classify(listOf("跳过"), "", false, true))
    assertEquals(VideoAdRules.Action.NONE,
      VideoAdRules.classify(listOf("3 跳过"), "", true, false))
    assertEquals(VideoAdRules.Action.CONTEXTUAL_SKIP,
      VideoAdRules.classify(listOf("3 跳过"), "", true, true))
    assertEquals(VideoAdRules.Action.CONTEXTUAL_SKIP,
      VideoAdRules.classify(listOf("skip"), "", true, true))
  }

  @Test fun commonPlayerActionsAreNeverClicked() {
    for (text in listOf(
      "跳过片头", "跳过片尾", "跳过前情", "下一集",
      "关闭播放器", "关闭视频", "退出全屏", "试看",
      "开通会员", "购买VIP", "订阅", "支付", "倍速", "弹幕",
      "Skip Intro", "Skip Recap", "Next Episode", "Close Player"
    )) {
      assertEquals(text, VideoAdRules.Action.NONE,
        VideoAdRules.classify(listOf(text), "com.qiyi.video:id/ad_skip", true, true))
    }
  }

  @Test fun countdownUntilAdIsSkippableMustNeverBePressedEarly() {
    for (text in listOf("5秒后可跳过广告", "3秒后跳过广告", "跳过倒计时",
      "Skip in 5", "广告剩余 5 秒", "10 秒后可跳过")) {
      assertEquals(text, VideoAdRules.Action.NONE,
        VideoAdRules.classify(listOf(text), "", true, true))
    }
  }

  @Test fun genericCloseAndXNeverCountAsVideoAdClose() {
    for (text in listOf("关闭", "X", "×", "关闭弹窗", "取消")) {
      assertEquals(text, VideoAdRules.Action.NONE,
        VideoAdRules.classify(listOf(text), "", true, true))
    }
  }

  @Test fun specificAdResourceIdentifiersRequireLocalEvidence() {
    for (id in listOf("com.qiyi.video:id/ad_skip", "com.youku.phone:id/video_ad_skip",
      "com.tencent.qqlive:id/preroll_skip_btn")) {
      assertEquals(id, VideoAdRules.Action.EXPLICIT_SKIP,
        VideoAdRules.classify(emptyList(), id, true, true))
      assertEquals(id, VideoAdRules.Action.NONE,
        VideoAdRules.classify(emptyList(), id, false, true))
    }
    assertEquals(VideoAdRules.Action.EXPLICIT_CLOSE,
      VideoAdRules.classify(emptyList(), "com.qiyi.video:id/ad_close", true, false))
    assertEquals(VideoAdRules.Action.NONE,
      VideoAdRules.classify(emptyList(), "com.qiyi.video:id/close_player", true, true))
  }

  @Test fun videoPlayerPromotionsAreNotProofOfSkippableAds() {
    for (text in listOf("会员权益", "领取红包", "下集预告", "精彩回顾", "重播")) {
      assertFalse(text, VideoAdRules.hasAdEvidence(text))
    }
    assertTrue(VideoAdRules.hasAdEvidence("广告"))
    assertTrue(VideoAdRules.hasAdEvidence("贴片广告"))
    assertTrue(VideoAdRules.hasAdEvidence("Advertisement"))
  }

  @Test fun protectedAlarmTimerScreensStillVetoActions() {
    assertTrue(ProtectedUiPolicy.isProtectedScreen(
      packageName = "com.google.android.deskclock",
      visibleLabels = listOf("广告", "跳过广告")
    ))
    assertTrue(ProtectedUiPolicy.isProtectedScreen(
      packageName = "com.qiyi.video",
      visibleLabels = listOf("闹钟", "3 跳过", "广告")
    ))
    assertFalse(ProtectedUiPolicy.isProtectedScreen(
      packageName = "com.qiyi.video",
      visibleLabels = listOf("贴片广告", "跳过广告")
    ))
  }
}
