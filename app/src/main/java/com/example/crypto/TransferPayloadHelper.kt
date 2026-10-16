package com.example.crypto

import com.example.model.OtpAccount
import com.example.model.OtpAlgorithm
import com.example.model.OtpType
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

object TransferPayloadHelper {

  private const val HEADER_APP = "KeylineAuthenticator"
  private const val FORMAT_VERSION = 1

  fun isTransferPayload(raw: String): Boolean {
    val trimmed = raw.trim()
    return (trimmed.startsWith("{") && trimmed.contains("\"accounts\"")) ||
      trimmed.startsWith("keyline-transfer://")
  }

  fun serializeAccounts(accounts: List<OtpAccount>): String {
    val root = JSONObject()
    root.put("app", HEADER_APP)
    root.put("version", FORMAT_VERSION)
    root.put("timestamp", System.currentTimeMillis() / 1000L)

    val accountsArray = JSONArray()
    for (account in accounts) {
      val item = JSONObject().apply {
        put("name", account.accountName)
        put("issuer", account.issuer)
        put("secret", account.secretKey)
        put("type", account.type.name)
        put("algo", account.algorithm.name)
        put("digits", account.digits)
        put("period", account.period)
        put("counter", account.counter)
      }
      accountsArray.put(item)
    }

    root.put("accounts", accountsArray)
    return root.toString()
  }

  fun deserialize(payload: String): Result<List<OtpAccount>> {
    return try {
      val trimmed = payload.trim()

      // Also support single otpauth:// QR codes
      if (trimmed.startsWith("otpauth://", ignoreCase = true)) {
        val singleResult = OtpUriParser.parse(trimmed)
        return singleResult.map { listOf(it) }
      }

      val jsonString = if (trimmed.startsWith("keyline-transfer://")) {
        trimmed.removePrefix("keyline-transfer://")
      } else {
        trimmed
      }

      val root = JSONObject(jsonString)
      val accountsArray = root.getJSONArray("accounts")
      val list = ArrayList<OtpAccount>()

      for (i in 0 until accountsArray.length()) {
        val item = accountsArray.getJSONObject(i)
        val name = item.getString("name")
        val issuer = item.optString("issuer", "")
        val secret = item.getString("secret")
        val typeStr = item.optString("type", OtpType.TOTP.name)
        val algoStr = item.optString("algo", OtpAlgorithm.SHA1.name)
        val digits = item.optInt("digits", 6)
        val period = item.optInt("period", 30)
        val counter = item.optLong("counter", 0L)

        val sanitizedSecret = Base32.sanitize(secret)
        if (!Base32.isValid(sanitizedSecret)) {
          continue // skip invalid entries
        }

        list.add(
          OtpAccount(
            id = UUID.randomUUID().toString(),
            accountName = name,
            issuer = issuer,
            secretKey = sanitizedSecret,
            type = try { OtpType.valueOf(typeStr) } catch (_: Exception) { OtpType.TOTP },
            algorithm = try { OtpAlgorithm.valueOf(algoStr) } catch (_: Exception) { OtpAlgorithm.SHA1 },
            digits = if (digits in 6..8) digits else 6,
            period = if (period > 0) period else 30,
            counter = counter
          )
        )
      }

      if (list.isEmpty()) {
        Result.failure(IllegalArgumentException("No valid accounts found in transfer payload"))
      } else {
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException("Failed to decode transfer QR payload: ${e.message}", e))
    }
  }
}
