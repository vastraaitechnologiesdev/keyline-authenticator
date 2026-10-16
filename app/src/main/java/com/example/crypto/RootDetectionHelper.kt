package com.example.crypto

import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class SecurityCheckResult(
  val isCompromised: Boolean,
  val reasons: List<String>
)

object RootDetectionHelper {

  private val SU_BINARY_PATHS = arrayOf(
    "/system/app/Superuser.apk",
    "/sbin/su",
    "/system/bin/su",
    "/system/xbin/su",
    "/data/local/xbin/su",
    "/data/local/bin/su",
    "/system/sd/xbin/su",
    "/system/bin/failsafe/su",
    "/data/local/su",
    "/su/bin/su",
    "/system/bin/.ext/.su"
  )

  fun checkEnvironment(): SecurityCheckResult {
    val detectedReasons = mutableListOf<String>()

    // 1. Check for SU binaries
    for (path in SU_BINARY_PATHS) {
      try {
        val file = File(path)
        if (file.exists()) {
          detectedReasons.add("Superuser binary found ($path)")
          break
        }
      } catch (_: Throwable) {}
    }

    // 2. Check for which su
    if (detectedReasons.isEmpty()) {
      var process: Process? = null
      try {
        process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val line = reader.readLine()
        if (!line.isNullOrBlank()) {
          detectedReasons.add("Executable su binary detected in system PATH")
        }
      } catch (_: Throwable) {
      } finally {
        process?.destroy()
      }
    }

    // 3. Check for test-keys / custom unverified ROM build tags
    val tags = Build.TAGS
    if (tags != null && tags.contains("test-keys")) {
      detectedReasons.add("Unverified system build signatures (test-keys)")
    }

    return SecurityCheckResult(
      isCompromised = detectedReasons.isNotEmpty(),
      reasons = detectedReasons
    )
  }
}
