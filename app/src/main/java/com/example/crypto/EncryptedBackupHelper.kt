package com.example.crypto

import android.util.Base64
import com.example.model.OtpAccount
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

object EncryptedBackupHelper {

  private const val HEADER_APP = "KeylineAuthenticator"
  private const val BACKUP_TYPE = "encrypted_backup"
  private const val FORMAT_VERSION = 1
  private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
  private const val PBKDF2_ITERATIONS = 100_000
  private const val KEY_LENGTH_BITS = 256
  private const val SALT_LENGTH_BYTES = 16
  private const val GCM_IV_LENGTH_BYTES = 12
  private const val GCM_TAG_LENGTH_BITS = 128

  fun exportEncryptedBackup(
    accounts: List<OtpAccount>,
    password: CharArray
  ): String {
    val random = SecureRandom()
    val salt = ByteArray(SALT_LENGTH_BYTES).apply { random.nextBytes(this) }
    val iv = ByteArray(GCM_IV_LENGTH_BYTES).apply { random.nextBytes(this) }

    val keySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
    val keyFactory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
    val keyBytes = keyFactory.generateSecret(keySpec).encoded
    val secretKey = SecretKeySpec(keyBytes, "AES")

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

    val plainJson = TransferPayloadHelper.serializeAccounts(accounts)
    val ciphertext = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

    val root = JSONObject().apply {
      put("app", HEADER_APP)
      put("type", BACKUP_TYPE)
      put("version", FORMAT_VERSION)
      put("timestamp", System.currentTimeMillis() / 1000L)
      put("kdf", KDF_ALGORITHM)
      put("iterations", PBKDF2_ITERATIONS)
      put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
      put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
      put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
    }

    return root.toString(2)
  }

  fun decryptBackup(
    backupContent: String,
    password: CharArray
  ): Result<List<OtpAccount>> {
    return try {
      val trimmed = backupContent.trim()
      val root = JSONObject(trimmed)

      val app = root.optString("app", "")
      if (app != HEADER_APP && !root.has("ciphertext")) {
        return Result.failure(IllegalArgumentException("Invalid Keyline backup file format"))
      }

      val saltBase64 = root.getString("salt")
      val ivBase64 = root.getString("iv")
      val ciphertextBase64 = root.getString("ciphertext")
      val iterations = root.optInt("iterations", PBKDF2_ITERATIONS)

      val salt = Base64.decode(saltBase64, Base64.DEFAULT)
      val iv = Base64.decode(ivBase64, Base64.DEFAULT)
      val ciphertext = Base64.decode(ciphertextBase64, Base64.DEFAULT)

      val keySpec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
      val keyFactory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
      val keyBytes = keyFactory.generateSecret(keySpec).encoded
      val secretKey = SecretKeySpec(keyBytes, "AES")

      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
      cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

      val decryptedBytes = cipher.doFinal(ciphertext)
      val decryptedPlaintext = String(decryptedBytes, Charsets.UTF_8)

      TransferPayloadHelper.deserialize(decryptedPlaintext)
    } catch (e: javax.crypto.AEADBadTagException) {
      Result.failure(IllegalArgumentException("Incorrect password or corrupted backup file", e))
    } catch (e: javax.crypto.BadPaddingException) {
      Result.failure(IllegalArgumentException("Incorrect password or corrupted backup file", e))
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException("Failed to restore backup: ${e.message}", e))
    }
  }
}
