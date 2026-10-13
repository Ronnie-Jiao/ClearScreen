package com.clearscreen.prototype

import android.app.Application
import android.content.res.Configuration
import android.os.Build
import android.os.Process

import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.ReactHost
import com.facebook.react.common.ReleaseLevel
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint
import com.facebook.react.defaults.DefaultReactNativeHost

import expo.modules.ApplicationLifecycleDispatcher
import expo.modules.ReactNativeHostWrapper
import com.clearscreen.prototype.backend.ClearScreenPackage

class MainApplication : Application(), ReactApplication {

  override val reactNativeHost: ReactNativeHost = ReactNativeHostWrapper(
      this,
      object : DefaultReactNativeHost(this) {
        override fun getPackages(): List<ReactPackage> =
            PackageList(this).packages.apply {
              // Packages that cannot be autolinked yet can be added manually here, for example:
              add(ClearScreenPackage())
            }

          override fun getJSMainModuleName(): String = ".expo/.virtual-metro-entry"

          override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

          override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
      }
  )

  override val reactHost: ReactHost
    get() = ReactNativeHostWrapper.createReactHost(applicationContext, reactNativeHost)

  override fun onCreate() {
    super.onCreate()
    // The accessibility service runs in a small, independent process. Do not
    // initialize React Native or Expo there: loading the JS runtime makes the
    // service needlessly large and allows vivo's task cleaner to remove it
    // together with the UI process.
    if (isAccessibilityProcess()) return

    DefaultNewArchitectureEntryPoint.releaseLevel = try {
      ReleaseLevel.valueOf(BuildConfig.REACT_NATIVE_RELEASE_LEVEL.uppercase())
    } catch (e: IllegalArgumentException) {
      ReleaseLevel.STABLE
    }
    loadReactNative(this)
    ApplicationLifecycleDispatcher.onApplicationCreate(this)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    if (isAccessibilityProcess()) return
    ApplicationLifecycleDispatcher.onConfigurationChanged(this, newConfig)
  }

  private fun isAccessibilityProcess(): Boolean {
    val currentProcessName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      Application.getProcessName()
    } else {
      val activityManager = getSystemService(ACTIVITY_SERVICE) as? android.app.ActivityManager
      activityManager?.runningAppProcesses
        ?.firstOrNull { it.pid == Process.myPid() }
        ?.processName
    }
    return currentProcessName?.endsWith(":accessibility") == true
  }
}
