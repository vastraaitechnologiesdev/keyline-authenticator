package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.example.crypto.BiometricAuthHelper
import com.example.ui.navigation.KeylineNavHost
import com.example.ui.screens.VaultLockScreen
import com.example.ui.theme.KeylineAuthTheme
import com.example.viewmodel.AuthenticatorViewModel

class MainActivity : FragmentActivity() {

  private val viewModel: AuthenticatorViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      // Enforce FLAG_SECURE safely on window creation
      try {
        window.setFlags(
          WindowManager.LayoutParams.FLAG_SECURE,
          WindowManager.LayoutParams.FLAG_SECURE
        )
      } catch (_: Throwable) {}

      try {
        enableEdgeToEdge()
      } catch (_: Throwable) {}

      setContent {
        val isBlockScreenshots by viewModel.isBlockScreenshotsEnabled.collectAsState()
        val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
        val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
        val themeMode by viewModel.themeMode.collectAsState()

        // Handle FLAG_SECURE dynamically
        LaunchedEffect(isBlockScreenshots) {
          try {
            if (isBlockScreenshots) {
              window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
              )
            } else {
              window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
          } catch (_: Throwable) {}
        }

        fun promptBiometricUnlock() {
          try {
            if (!BiometricAuthHelper.canAuthenticate(this@MainActivity)) {
              // If device cannot authenticate (no hardware or not enrolled), unlock vault directly
              viewModel.unlockApp()
              return
            }

            BiometricAuthHelper.authenticate(
              activity = this@MainActivity,
              title = "Keyline Vault Lock",
              subtitle = "Authenticate to unlock your 2FA accounts",
              onSuccess = {
                viewModel.unlockApp()
              },
              onError = { _ ->
                // Allow user to re-try via the unlock button
              }
            )
          } catch (_: Throwable) {
            viewModel.unlockApp()
          }
        }

        // Automatically prompt biometric if app lock is enabled and currently locked
        LaunchedEffect(isAppLockEnabled, isAppUnlocked) {
          if (isAppLockEnabled && !isAppUnlocked) {
            promptBiometricUnlock()
          }
        }

        KeylineAuthTheme(themeMode = themeMode) {
          if (isAppLockEnabled && !isAppUnlocked) {
            VaultLockScreen(
              onUnlockClick = { promptBiometricUnlock() }
            )
          } else {
            KeylineNavHost(viewModel = viewModel)
          }
        }
      }
    } catch (e: Exception) {
      // Intercept any runtime initialization exceptions
    } catch (_: Throwable) {
      // Intercept any errors
    }
  }

  override fun onStop() {
    super.onStop()
    // If App Lock is enabled, lock the vault when user leaves the app
    try {
      if (viewModel.isAppLockEnabled.value) {
        viewModel.lockApp()
      }
    } catch (_: Throwable) {}
  }
}
