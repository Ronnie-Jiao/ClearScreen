package com.clearscreen.prototype.backend

import java.util.Locale

/** Structural floating-card evidence; no displayed copy or screen-corner tests. */
internal object FloatingControlRules {
  private val layerMarkers = listOf("float", "pendant", "overlay", "popup")
  private val promotionMarkers = listOf("promo", "advert", "marketing", "commercial", "sponsor", "campaign", "operation")
  private val businessMarkers = listOf(
    "player", "playback", "pictureinpicture", "picture_in_picture", "miniplayer", "mini_player",
    "chat", "message", "call", "voice", "music", "avatar", "bubble", "download", "upload",
    "progress", "toolbar", "toolbox", "navigation", "keyboard", "input", "accessibility",
    "permission", "privacy", "consent", "tutorial", "guide", "help", "feedback",
  )

  fun isFloatingLayer(resourceId: String, className: String): Boolean {
    val id = resourceId.substringAfter(":id/").lowercase(Locale.ROOT)
    val type = className.substringAfterLast('.').lowercase(Locale.ROOT)
    if (businessMarkers.any { id.contains(it) || type.contains(it) }) return false
    return AdControlEvidenceRules.isAdStructureNode(resourceId, className) ||
      layerMarkers.any { id.contains(it) || type.contains(it) } &&
        promotionMarkers.any { id.contains(it) || type.contains(it) }
  }

  fun isCompactLayer(layer: AdGeometryRules.Box, screen: AdGeometryRules.Box): Boolean {
    if (!AdControlEvidenceRules.contains(screen, layer)) return false
    val ratio = layer.width.toLong() * layer.height / (screen.width.toLong() * screen.height).toDouble()
    return layer.width <= screen.width * 0.55 && layer.height <= screen.height * 0.40 && ratio in 0.0008..0.18
  }

  fun ownsCreative(layer: AdGeometryRules.Box, creative: AdGeometryRules.Box): Boolean =
    AdControlEvidenceRules.contains(layer, creative) &&
      creative.width.toLong() * creative.height >= layer.width.toLong() * layer.height * 0.25

  fun supportsCreative(resourceId: String, layerClass: String, creativeClass: String): Boolean =
    AdControlEvidenceRules.isAdStructureNode(resourceId, layerClass) || creativeClass.contains("imageview")
}
