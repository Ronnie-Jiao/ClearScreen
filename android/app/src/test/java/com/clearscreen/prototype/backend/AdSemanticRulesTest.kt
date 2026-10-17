package com.clearscreen.prototype.backend

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit-level regression corpus. This deliberately includes negative samples:
 * adding a new ad word must not turn normal onboarding / confirmation UI into
 * an ad action. A positive text match alone NEVER authorizes a click.
 */
class AdSemanticRulesTest {
  @Test fun textAndDescriptionAreMatchedIndependently() {
    assertEquals("2跳过", AdSemanticRules.selectSkipLabel(listOf("2 跳过", "按钮")))
    assertEquals("1跳过", AdSemanticRules.selectSkipLabel(listOf("1 跳过", "跳过")))
    assertEquals("跳过广告", AdSemanticRules.selectSkipLabel(listOf("跳过", "跳过广告")))
    assertEquals("skipad", AdSemanticRules.selectSkipLabel(listOf("Ｓｋｉｐ　Ａｄ", "button")))
    assertEquals("跳过", AdSemanticRules.selectSkipLabel(listOf("跳\u200b过")))
  }

  @Test fun aFutureOrBusinessSkipCannotBeOverriddenByItsShortDescription() {
    for (label in listOf("5秒后可跳过广告", "3秒后跳过广告", "Skip in 3", "跳过倒计时 5 秒",
      "跳过教程", "跳过片头", "跳过验证", "跳过登录")) {
      assertNull(label, AdSemanticRules.selectSkipLabel(listOf(label, "跳过")))
    }
  }

  @Test fun closeLabelsSurviveRoleDescriptionsWithoutAcceptingBusinessControls() {
    assertEquals("关闭此广告", AdSemanticRules.selectCloseLabel(listOf("关闭此广告", "按钮")))
    assertEquals("×", AdSemanticRules.selectCloseLabel(listOf("×", "关闭按钮", "X")))
    assertNull(AdSemanticRules.selectCloseLabel(listOf("关闭权限", "关闭银行账户")))
    assertTrue(AdSemanticRules.isNeutralControlLabel("Image Button"))
    assertFalse(AdSemanticRules.isNeutralControlLabel("下载"))
    assertFalse(AdSemanticRules.isNeutralControlLabel("下一步"))
  }
  @Test fun recognizesCommonCountdownFormats() {
    for (raw in listOf("3 跳过", "03秒跳过广告", "跳过 3s", "Skip in 3", "跳过倒计时 5 秒")) {
      assertTrue(raw, AdSemanticRules.isCountdownSkip(AdSemanticRules.normalize(raw)))
    }
  }

  @Test fun recognizesExplicitSkipWithoutAppSpecificHardcoding() {
    for (raw in listOf("跳过广告", "跳过此广告", "跳过推广", "跳过开屏广告", "Skip ad", "Skip advertisement")) {
      assertTrue(raw, AdSemanticRules.isExplicitSkip(AdSemanticRules.normalize(raw)))
    }
  }

  @Test fun doesNotTreatOtherSkipWordsAsAdvertising() {
    for (raw in listOf("跳过验证", "跳过教程", "跳过绑定", "跳过支付", "跳过这个步骤", "下一步", "点击跳转")) {
      val label = AdSemanticRules.normalize(raw)
      assertFalse(raw, AdSemanticRules.isExplicitSkip(label))
      assertFalse(raw, AdSemanticRules.isCountdownSkip(label))
    }
  }

  @Test fun classifiesPlainSkipSeparatelyFromExplicitAdSkip() {
    assertTrue(AdSemanticRules.isPlainSkip(AdSemanticRules.normalize("跳过")))
    assertTrue(AdSemanticRules.isPlainSkip(AdSemanticRules.normalize("SKIP")))
    assertFalse(AdSemanticRules.isExplicitSkip(AdSemanticRules.normalize("跳过")))
  }

  @Test fun recognizesAdCloseLabelsButNotRegularWindowCloses() {
    for (raw in listOf("关闭广告", "关闭此广告", "关闭推广", "Close ad")) {
      val label = AdSemanticRules.normalize(raw)
      assertTrue(raw, AdSemanticRules.isClose(label))
      assertTrue(raw, AdSemanticRules.isExplicitAdClose(label))
    }
    for (raw in listOf("关闭", "关闭弹窗", "X", "×")) {
      val label = AdSemanticRules.normalize(raw)
      assertTrue(raw, AdSemanticRules.isClose(label))
      assertFalse(raw, AdSemanticRules.isExplicitAdClose(label))
    }
    for (raw in listOf("关闭位置", "关闭银行账户", "关闭权限", "关闭后台音乐")) {
      assertFalse(raw, AdSemanticRules.isClose(AdSemanticRules.normalize(raw)))
    }
  }

  @Test fun usesIdTokenBoundariesInsteadOfArbitrarySubstrings() {
    for (id in listOf("com.example:id/skip_button", "org.foo:id/ad_skip", "com.foo:id/countdown_skip", "com.foo:id/skipbtn")) {
      assertTrue(id, AdSemanticRules.isSkipResource(id))
    }
    for (id in listOf("com.foo:id/ad_close", "com.foo:id/close_ad", "com.foo:id/ad_close_button", "com.foo:id/splash_close")) {
      assertTrue(id, AdSemanticRules.isAdCloseResource(id))
    }
    for (id in listOf("com.foo:id/jump_to_payment", "com.foo:id/skip_intro_page", "com.foo:id/close_settings", "com.foo:id/skipjack")) {
      if (id.contains("close")) assertFalse(id, AdSemanticRules.isAdCloseResource(id))
      else assertFalse(id, AdSemanticRules.isSkipResource(id))
    }
  }

  @Test fun distinguishesAdFromOrdinaryMarketingWords() {
    assertTrue(AdSemanticRules.hasStrongAdSignal("广告"))
    assertTrue(AdSemanticRules.hasStrongAdSignal("Sponsored"))
    assertTrue(AdSemanticRules.hasMediumAdSignal(AdSemanticRules.normalize("领取红包")))
    assertFalse(AdSemanticRules.hasStrongAdSignal("日常天气"))
  }

  @Test fun marksSensitiveContextsThatMustNotAuthorizeAutomaticTap() {
    for (raw in listOf("隐私政策", "用户协议", "验证码", "实名认证", "支付确认", "新手引导")) {
      assertTrue(raw, AdSemanticRules.isSensitiveUiContext(raw))
    }
    assertFalse(AdSemanticRules.isSensitiveUiContext("立即办理宽带套餐"))
  }
}
