package com.example.crypto

import com.example.model.OtpAlgorithm
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpGenerator {

  private val DIGITS_POWER = intArrayOf(
    1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000
  )

  fun generateTotp(
    secretBase32: String,
    timeSeconds: Long = System.currentTimeMillis() / 1000L,
    period: Int = 30,
    digits: Int = 6,
    algorithm: OtpAlgorithm = OtpAlgorithm.SHA1
  ): String {
    val timeStep = timeSeconds / period
    return generateHotp(secretBase32, timeStep, digits, algorithm)
  }

  fun generateHotp(
    secretBase32: String,
    counter: Long,
    digits: Int = 6,
    algorithm: OtpAlgorithm = OtpAlgorithm.SHA1
  ): String {
    val keyBytes = try {
      Base32.decode(secretBase32)
    } catch (_: Exception) {
      return "0".repeat(digits)
    }

    if (keyBytes.isEmpty()) {
      return "0".repeat(digits)
    }

    val macAlgorithm = when (algorithm) {
      OtpAlgorithm.SHA1 -> "HmacSHA1"
      OtpAlgorithm.SHA256 -> "HmacSHA256"
      OtpAlgorithm.SHA512 -> "HmacSHA512"
    }

    val hash: ByteArray
    try {
      val mac = Mac.getInstance(macAlgorithm)
      val macKey = SecretKeySpec(keyBytes, macAlgorithm)
      mac.init(macKey)

      val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()
      try {
        hash = mac.doFinal(counterBytes)
      } finally {
        // Scrub counter bytes from memory
        java.util.Arrays.fill(counterBytes, 0.toByte())
      }
    } finally {
      // Ephemeral Memory & RAM Scrubbing: overwrite secret key bytes with zeros immediately after hashing
      java.util.Arrays.fill(keyBytes, 0.toByte())
    }

    // Dynamic Truncation (RFC 4226 Section 5.4)
    val offset = hash[hash.size - 1].toInt() and 0x0F
    val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
      ((hash[offset + 1].toInt() and 0xFF) shl 16) or
      ((hash[offset + 2].toInt() and 0xFF) shl 8) or
      (hash[offset + 3].toInt() and 0xFF)

    val modulo = if (digits in 1..8) DIGITS_POWER[digits] else 10.0.pow(digits.toDouble()).toInt()
    val otp = binary % modulo

    return String.format("%0${digits}d", otp)
  }

  fun formatOtp(code: String): String {
    return if (code.length == 6) {
      "${code.substring(0, 3)} ${code.substring(3)}"
    } else if (code.length == 8) {
      "${code.substring(0, 4)} ${code.substring(4)}"
    } else {
      code
    }
  }

  fun getRemainingSeconds(period: Int = 30, currentTimeSeconds: Long = System.currentTimeMillis() / 1000L): Int {
    val elapsed = (currentTimeSeconds % period).toInt()
    val remaining = period - elapsed
    return if (remaining == 0) period else remaining
  }

  fun getProgress(period: Int = 30, currentTimeSeconds: Long = System.currentTimeMillis() / 1000L): Float {
    val remaining = getRemainingSeconds(period, currentTimeSeconds)
    return remaining.toFloat() / period.toFloat()
  }
}
