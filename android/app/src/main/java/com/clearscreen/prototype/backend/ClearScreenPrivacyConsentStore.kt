package com.clearscreen.prototype.backend

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * A tiny, uncached consent record checked by both the UI and accessibility
 * service. It stays separate from rules/log preferences so those are not
 * opened before consent, and remains safe if the service is later isolated.
 */
class ClearScreenPrivacyConsentStore(context: Context) {
  private val baseFile = File(context.filesDir, FILE_NAME)
  private val atomicFile = AtomicFile(baseFile)

  @Synchronized
  fun hasCurrentConsent(): Boolean {
    if (!baseFile.exists()) return false
    return runCatching {
      atomicFile.openRead().bufferedReader(StandardCharsets.UTF_8).use { reader ->
        val record = JSONObject(reader.readText())
        record.optString(KEY_POLICY_VERSION) == POLICY_VERSION &&
          record.optLong(KEY_ACCEPTED_AT, 0L) > 0L
      }
    }.getOrDefault(false)
  }

  @Synchronized
  fun acceptCurrentPolicy() {
    val output = atomicFile.startWrite()
    try {
      val bytes = JSONObject()
        .put(KEY_POLICY_VERSION, POLICY_VERSION)
        .put(KEY_ACCEPTED_AT, System.currentTimeMillis())
        .toString()
        .toByteArray(StandardCharsets.UTF_8)
      output.write(bytes)
      output.flush()
      output.fd.sync()
      atomicFile.finishWrite(output)
    } catch (error: Exception) {
      atomicFile.failWrite(output)
      throw error
    }
  }

  @Synchronized
  fun withdrawConsent() {
    atomicFile.delete()
  }

  companion object {
    const val POLICY_VERSION = "2026-10-13-v1"
    private const val FILE_NAME = "clearscreen_privacy_consent.json"
    private const val KEY_POLICY_VERSION = "policyVersion"
    private const val KEY_ACCEPTED_AT = "acceptedAt"
  }
}
