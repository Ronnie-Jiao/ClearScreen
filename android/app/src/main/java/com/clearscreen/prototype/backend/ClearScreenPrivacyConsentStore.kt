package com.clearscreen.prototype.backend

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * The first-run consent screen has been removed. Local capabilities are
 * available immediately after installation; the policy stays as a reference
 * document in the repository. The record helpers are kept so existing call
 * sites (and older installs) keep working without a migration.
 */
class ClearScreenPrivacyConsentStore(context: Context) {
  private val baseFile = File(context.filesDir, FILE_NAME)
  private val atomicFile = AtomicFile(baseFile)

  @Synchronized
  fun hasCurrentConsent(): Boolean = true

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
    internal val operationLock = Any()
    const val POLICY_VERSION = "2026-11-14-v1"
    private const val FILE_NAME = "clearscreen_privacy_consent.json"
    private const val KEY_POLICY_VERSION = "policyVersion"
    private const val KEY_ACCEPTED_AT = "acceptedAt"
  }
}
