package com.example.crypto

import com.example.model.OtpAccount
import com.example.model.OtpAlgorithm
import com.example.model.OtpType
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject

object TransferPayloadHelper {

  private const val HEADER_APP = "KeylineAuthenticator"
  private const val FORMAT_VERSION_PLAIN = 1
  private const val FORMAT_VERSION_ENCRYPTED = 2

  private const val PBKDF2_ALGO = "PBKDF2WithHmacSHA256"
  private const val PBKDF2_ITERATIONS = 20_000
  private const val KEY_LEN_BITS = 256
  private const val GCM_TAG_LEN_BITS = 128
  private const val SALT_LEN_BYTES = 16
  private const val IV_LEN_BYTES = 12

  /**
   * Generates a secure, human-friendly 6-digit one-time transfer PIN (000000 - 999999).
   */
  fun generateTransferPin(): String {
    val random = SecureRandom()
    val number = random.nextInt(1_000_000)
    return String.format(java.util.Locale.US, "%06d", number)
  }

  fun isTransferPayload(raw: String): Boolean {
    val trimmed = raw.trim()
    return (trimmed.startsWith("{") && (trimmed.contains("\"accounts\"") || trimmed.contains("\"ciphertext\""))) ||
      trimmed.startsWith("keyline-transfer://") ||
      trimmed.startsWith("otpauth://", ignoreCase = true)
  }

  fun isEncrypted(raw: String): Boolean {
    val trimmed = if (raw.trim().startsWith("keyline-transfer://")) {
      raw.trim().removePrefix("keyline-transfer://")
    } else {
      raw.trim()
    }
    return trimmed.startsWith("{") && trimmed.contains("\"ciphertext\"")
  }

  fun serializeAccounts(accounts: List<OtpAccount>): String {
    val root = JSONObject()
    root.put("app", HEADER_APP)
    root.put("version", FORMAT_VERSION_PLAIN)
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

  /**
   * Serializes accounts and encrypts the JSON payload with AES-256-GCM using a key
   * derived from the 6-digit transfer PIN via PBKDF2-HMAC-SHA256.
   *
   * The returned string contains only ciphertext, IV, and salt in Base64 (zero plaintext secrets).
   */
  fun serializeAndEncrypt(accounts: List<OtpAccount>, pin: String): String {
    val plaintextJson = serializeAccounts(accounts)
    val random = SecureRandom()

    val salt = ByteArray(SALT_LEN_BYTES).also { random.nextBytes(it) }
    val iv = ByteArray(IV_LEN_BYTES).also { random.nextBytes(it) }

    val keySpec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LEN_BITS)
    val keyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGO)
    val keyBytes = keyFactory.generateSecret(keySpec).encoded
    val secretKey = SecretKeySpec(keyBytes, "AES")

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LEN_BITS, iv))
    val ciphertext = cipher.doFinal(plaintextJson.toByteArray(Charsets.UTF_8))

    val root = JSONObject().apply {
      put("app", HEADER_APP)
      put("version", FORMAT_VERSION_ENCRYPTED)
      put("encrypted", true)
      put("timestamp", System.currentTimeMillis() / 1000L)
      put("salt", base64Encode(salt))
      put("iv", base64Encode(iv))
      put("ciphertext", base64Encode(ciphertext))
      put("iterations", PBKDF2_ITERATIONS)
    }

    return root.toString()
  }

  /**
   * Decrypts an encrypted transfer payload using the 6-digit transfer PIN and parses the accounts.
   * Throws or returns failure if GCM authentication tag verification fails (incorrect PIN or tampered data).
   */
  fun decryptAndDeserialize(payload: String, pin: String): Result<List<OtpAccount>> {
    return try {
      val jsonString = if (payload.trim().startsWith("keyline-transfer://")) {
        payload.trim().removePrefix("keyline-transfer://")
      } else {
        payload.trim()
      }

      val root = JSONObject(jsonString)
      if (!root.optBoolean("encrypted", false) && !root.has("ciphertext")) {
        return deserialize(payload, null)
      }

      val salt = base64Decode(root.getString("salt"))
      val iv = base64Decode(root.getString("iv"))
      val ciphertext = base64Decode(root.getString("ciphertext"))
      val iterations = root.optInt("iterations", PBKDF2_ITERATIONS)

      val keySpec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LEN_BITS)
      val keyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGO)
      val keyBytes = keyFactory.generateSecret(keySpec).encoded
      val secretKey = SecretKeySpec(keyBytes, "AES")

      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LEN_BITS, iv))

      val plaintextBytes = try {
        cipher.doFinal(ciphertext)
      } catch (_: AEADBadTagException) {
        return Result.failure(IllegalArgumentException("Incorrect 6-digit transfer PIN. Please try again."))
      } catch (e: Exception) {
        return Result.failure(IllegalArgumentException("Decryption failed: Incorrect PIN or invalid data.", e))
      }

      val decryptedJson = String(plaintextBytes, Charsets.UTF_8)
      parseAccountsJson(decryptedJson)
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException(e.message ?: "Failed to decrypt transfer payload", e))
    }
  }

  fun deserialize(payload: String, pin: String? = null): Result<List<OtpAccount>> {
    if (isEncrypted(payload)) {
      if (pin.isNullOrBlank()) {
        return Result.failure(IllegalStateException("TRANSFER_PIN_REQUIRED"))
      }
      return decryptAndDeserialize(payload, pin)
    }

    return try {
      val trimmed = payload.trim()

      if (trimmed.startsWith("otpauth://", ignoreCase = true)) {
        val singleResult = OtpUriParser.parse(trimmed)
        return singleResult.map { listOf(it) }
      }

      val jsonString = if (trimmed.startsWith("keyline-transfer://")) {
        trimmed.removePrefix("keyline-transfer://")
      } else {
        trimmed
      }

      parseAccountsJson(jsonString)
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException("Failed to decode transfer QR payload: ${e.message}", e))
    }
  }

  private fun parseAccountsJson(jsonString: String): Result<List<OtpAccount>> {
    return try {
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
          continue
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
      Result.failure(IllegalArgumentException("Failed to parse accounts: ${e.message}", e))
    }
  }

  private fun base64Encode(data: ByteArray): String {
    return try {
      android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
    } catch (_: Throwable) {
      java.util.Base64.getEncoder().encodeToString(data)
    }
  }

  private fun base64Decode(str: String): ByteArray {
    return try {
      android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
    } catch (_: Throwable) {
      java.util.Base64.getDecoder().decode(str)
    }
  }
}
