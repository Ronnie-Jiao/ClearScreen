package com.clearscreen.prototype.backend

/** Startup actions depend on the control itself, never on surrounding ad copy. */
internal object StartupControlRules {
  fun label(labels: List<String>): AdSemanticRules.ControlLabel? =
    AdSemanticRules.controlLabel(labels)?.takeUnless {
      AdSemanticRules.isContextualAdDismissal(it.value)
    }

  fun supportsLabel(value: String): Boolean =
    AdSemanticRules.isNeutralControlLabel(value) || AdSemanticRules.isControlCounter(value) ||
      label(listOf(value)) != null
}
