package com.example.crypto

import com.example.model.OtpAccount
import com.example.model.OtpAlgorithm
import com.example.model.OtpType
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

object OtpUriParser {

  fun parse(rawUri: String): Result<OtpAccount> {
    return try {
      val trimmed = rawUri.trim()
      if (!trimmed.startsWith("otpauth://", ignoreCase = true)) {
        return Result.failure(IllegalArgumentException("Invalid URI scheme. Must start with otpauth://"))
      }

      val javaUri = URI(trimmed)
      val host = javaUri.host?.lowercase() ?: ""
      val type = when (host) {
        "totp" -> OtpType.TOTP
        "hotp" -> OtpType.HOTP
        else -> return Result.failure(IllegalArgumentException("Unsupported OTP type: $host"))
      }

      val path = javaUri.path?.removePrefix("/") ?: ""
      val decodedLabel = try {
        URLDecoder.decode(path, StandardCharsets.UTF_8.name())
      } catch (_: Exception) {
        path
      }

      var labelIssuer = ""
      var accountName = decodedLabel.trim()

      if (decodedLabel.contains(":")) {
        val parts = decodedLabel.split(":", limit = 2)
        labelIssuer = parts[0].trim()
        accountName = parts[1].trim()
      }

      val queryParams = parseQueryParams(javaUri.rawQuery)
      val secretParam = queryParams["secret"]
      if (secretParam.isNullOrBlank()) {
        return Result.failure(IllegalArgumentException("Missing secret key parameter"))
      }

      val sanitizedSecret = Base32.sanitize(secretParam)
      if (!Base32.isValid(sanitizedSecret)) {
        return Result.failure(IllegalArgumentException("Secret key is not valid Base32"))
      }

      val paramIssuer = queryParams["issuer"]?.trim().orEmpty()
      val finalIssuer = if (paramIssuer.isNotEmpty()) paramIssuer else labelIssuer

      val algorithmParam = queryParams["algorithm"]?.uppercase()
      val algorithm = when (algorithmParam) {
        "SHA256" -> OtpAlgorithm.SHA256
        "SHA512" -> OtpAlgorithm.SHA512
        else -> OtpAlgorithm.SHA1
      }

      val digitsParam = queryParams["digits"]?.toIntOrNull() ?: 6
      val digits = if (digitsParam in 6..8) digitsParam else 6

      val periodParam = queryParams["period"]?.toIntOrNull() ?: 30
      val period = if (periodParam > 0) periodParam else 30

      val counterParam = queryParams["counter"]?.toLongOrNull() ?: 0L

      val account = OtpAccount(
        id = UUID.randomUUID().toString(),
        accountName = if (accountName.isEmpty()) finalIssuer.ifEmpty { "Account" } else accountName,
        issuer = finalIssuer,
        secretKey = sanitizedSecret,
        type = type,
        algorithm = algorithm,
        digits = digits,
        period = period,
        counter = counterParam
      )

      Result.success(account)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun parseQueryParams(query: String?): Map<String, String> {
    if (query.isNullOrEmpty()) return emptyMap()
    val params = mutableMapOf<String, String>()
    val pairs = query.split("&")
    for (pair in pairs) {
      val idx = pair.indexOf("=")
      if (idx > 0) {
        val key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name())
        val value = if (idx < pair.length - 1) {
          URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name())
        } else {
          ""
        }
        params[key] = value
      }
    }
    return params
  }
}
