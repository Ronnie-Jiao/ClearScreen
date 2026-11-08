package com.clearscreen.prototype.backend

import android.content.Context
import androidx.annotation.Keep
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import java.security.PrivateKey
import java.security.cert.Certificate

/**
 * Local ADB connection manager. The key pair lives in the app's private files
 * directory and is only presented to the device this app is running on.
 */
@Keep
internal class ClearScreenAdbClient(context: Context) : AbsAdbConnectionManager() {
  private val appContext = context.applicationContext

  override fun getPrivateKey(): PrivateKey = ClearScreenAdbKeys.privateKey(appContext)

  override fun getCertificate(): Certificate = ClearScreenAdbKeys.certificate(appContext)

  override fun getDeviceName(): String = "ClearScreen"
}
