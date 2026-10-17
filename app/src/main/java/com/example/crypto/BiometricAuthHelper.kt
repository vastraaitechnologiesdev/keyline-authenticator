package com.example.crypto

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthHelper {

  fun canAuthenticate(activity: FragmentActivity): Boolean {
    return try {
      val biometricManager = BiometricManager.from(activity)
      val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
      biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    } catch (_: Throwable) {
      false
    }
  }

  fun authenticate(
    activity: FragmentActivity,
    title: String = "Authentication Required",
    subtitle: String = "Verify your identity to proceed",
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    if (activity.isFinishing || activity.isDestroyed) {
      return
    }

    val executor = ContextCompat.getMainExecutor(activity)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        onSuccess()
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        onError(errString.toString())
      }

      override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
      }
    }

    try {
      val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(
          BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

      val prompt = BiometricPrompt(activity, executor, callback)
      prompt.authenticate(promptInfo)
    } catch (_: Throwable) {
      // If prompt fails to build/show (e.g. negative button vs device credential collision on old SDKs)
      try {
        val fallbackPromptInfo = BiometricPrompt.PromptInfo.Builder()
          .setTitle(title)
          .setSubtitle(subtitle)
          .setNegativeButtonText("Cancel")
          .build()

        val prompt = BiometricPrompt(activity, executor, callback)
        prompt.authenticate(fallbackPromptInfo)
      } catch (t2: Throwable) {
        onError(t2.message ?: "Authentication failed")
      }
    }
  }
}
