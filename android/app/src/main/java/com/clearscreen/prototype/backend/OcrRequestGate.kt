package com.clearscreen.prototype.backend

/** One owned OCR operation. Cancelling evidence must not queue more hung ML tasks. */
internal class OcrRequestGate(private val timeoutMs: Long = 8_000L) {
  class Request internal constructor(val startedAt: Long, internal val generation: Long) {
    internal var cancelled = false
    internal var recognizing = false
  }
  private var active: Request? = null
  private var generation = 0L

  @Synchronized fun begin(now: Long): Request? {
    expire(now)
    if (active != null) return null
    return Request(now, ++generation).also { active = it }
  }

  @Synchronized fun accepts(request: Request, now: Long): Boolean =
    request.generation == generation && !request.cancelled && now - request.startedAt in 0..timeoutMs

  @Synchronized fun recognizing(request: Request, now: Long): Boolean {
    if (active !== request || !accepts(request, now)) return false
    request.recognizing = true
    return true
  }

  @Synchronized fun finish(request: Request) {
    if (active === request) active = null
  }

  @Synchronized fun cancel() {
    generation++
    active?.let {
      it.cancelled = true
      // ML Kit owns its input until task completion. Do not recycle or overlap it.
      if (!it.recognizing) active = null
    }
  }

  @Synchronized fun expire(now: Long): Boolean {
    val request = active ?: return false
    if (now - request.startedAt in 0..timeoutMs) return false
    cancel()
    return true
  }

  @Synchronized fun busy(): Boolean = active != null
  @Synchronized fun stalled(now: Long): Boolean = active?.let {
    it.recognizing && now - it.startedAt > timeoutMs
  } ?: false
}
