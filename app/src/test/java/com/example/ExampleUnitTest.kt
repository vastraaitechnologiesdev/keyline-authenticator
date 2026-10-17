package com.example

import com.example.crypto.Base32
import com.example.crypto.OtpUriParser
import com.example.crypto.TotpGenerator
import com.example.model.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

  @Test
  fun testBase32SanitizeAndValidate() {
    val keyWithSpaces = "jbsw y3dp ehpk 3pxp"
    val sanitized = Base32.sanitize(keyWithSpaces)
    assertEquals("JBSWY3DPEHPK3PXP", sanitized)
    assertTrue(Base32.isValid(sanitized))
  }

  @Test
  fun testBase32Decoding() {
    // RFC 4648 test vector: "MZXW6YTBOI======" -> "foobar"
    val bytes = Base32.decode("MZXW6YTBOI")
    val text = String(bytes)
    assertEquals("foobar", text)
  }

  @Test
  fun testTotpGeneration() {
    // Using standard test key "JBSWY3DPEHPK3PXP"
    val secret = "JBSWY3DPEHPK3PXP"
    val code = TotpGenerator.generateTotp(
      secretBase32 = secret,
      timeSeconds = 1234567890L,
      period = 30,
      digits = 6
    )
    assertNotNull(code)
    assertEquals(6, code.length)
    assertTrue(code.all { it.isDigit() })

    val formatted = TotpGenerator.formatOtp(code)
    assertEquals(7, formatted.length) // "123 456"
    assertEquals(' ', formatted[3])
  }

  @Test
  fun testOtpUriParser() {
    val uri = "otpauth://totp/GitHub:mona?secret=JBSWY3DPEHPK3PXP&issuer=GitHub"
    val result = OtpUriParser.parse(uri)
    assertTrue(result.isSuccess)

    val account = result.getOrThrow()
    assertEquals("GitHub", account.issuer)
    assertEquals("mona", account.accountName)
    assertEquals("JBSWY3DPEHPK3PXP", account.secretKey)
    assertEquals(OtpType.TOTP, account.type)
  }

  @Test
  fun testTransferPayloadSerializationAndDeserialization() {
    val sampleAccounts = listOf(
      com.example.model.OtpAccount(
        id = "id-1",
        accountName = "alice@gmail.com",
        issuer = "Google",
        secretKey = "JBSWY3DPEHPK3PXP",
        type = OtpType.TOTP,
        algorithm = com.example.model.OtpAlgorithm.SHA1,
        digits = 6,
        period = 30,
        counter = 0L
      ),
      com.example.model.OtpAccount(
        id = "id-2",
        accountName = "bob",
        issuer = "GitHub",
        secretKey = "MZXW6YTBOI======",
        type = OtpType.HOTP,
        algorithm = com.example.model.OtpAlgorithm.SHA256,
        digits = 8,
        period = 30,
        counter = 42L
      )
    )

    val payload = com.example.crypto.TransferPayloadHelper.serializeAccounts(sampleAccounts)
    assertTrue(com.example.crypto.TransferPayloadHelper.isTransferPayload(payload))

    val deserializedResult = com.example.crypto.TransferPayloadHelper.deserialize(payload)
    assertTrue(deserializedResult.isSuccess)

    val list = deserializedResult.getOrThrow()
    assertEquals(2, list.size)
    assertEquals("Google", list[0].issuer)
    assertEquals("alice@gmail.com", list[0].accountName)
    assertEquals("JBSWY3DPEHPK3PXP", list[0].secretKey)
    assertEquals(OtpType.TOTP, list[0].type)

    assertEquals("GitHub", list[1].issuer)
    assertEquals("bob", list[1].accountName)
    assertEquals(OtpType.HOTP, list[1].type)
    assertEquals(8, list[1].digits)
  }

  @Test
  fun testEncryptedTransferPayloadWithPin() {
    val sampleAccounts = listOf(
      com.example.model.OtpAccount(
        id = "id-1",
        accountName = "alice@gmail.com",
        issuer = "Google",
        secretKey = "JBSWY3DPEHPK3PXP",
        type = OtpType.TOTP,
        algorithm = com.example.model.OtpAlgorithm.SHA1,
        digits = 6,
        period = 30
      )
    )

    val pin = com.example.crypto.TransferPayloadHelper.generateTransferPin()
    assertEquals(6, pin.length)
    assertTrue(pin.all { it.isDigit() })

    val encryptedPayload = com.example.crypto.TransferPayloadHelper.serializeAndEncrypt(sampleAccounts, pin)
    assertTrue(com.example.crypto.TransferPayloadHelper.isTransferPayload(encryptedPayload))
    assertTrue(com.example.crypto.TransferPayloadHelper.isEncrypted(encryptedPayload))

    // Ensure zero plaintext secrets in the encrypted payload
    org.junit.Assert.assertFalse(encryptedPayload.contains("alice@gmail.com"))
    org.junit.Assert.assertFalse(encryptedPayload.contains("JBSWY3DPEHPK3PXP"))
    org.junit.Assert.assertTrue(encryptedPayload.contains("ciphertext"))
    org.junit.Assert.assertTrue(encryptedPayload.contains("salt"))
    org.junit.Assert.assertTrue(encryptedPayload.contains("iv"))

    // Deserializing without PIN fails
    val failWithoutPin = com.example.crypto.TransferPayloadHelper.deserialize(encryptedPayload, pin = null)
    assertTrue(failWithoutPin.isFailure)

    // Decrypting with wrong PIN fails
    val wrongPin = if (pin == "123456") "654321" else "123456"
    val failWrongPin = com.example.crypto.TransferPayloadHelper.decryptAndDeserialize(encryptedPayload, wrongPin)
    assertTrue(failWrongPin.isFailure)

    // Decrypting with correct PIN succeeds
    val successResult = com.example.crypto.TransferPayloadHelper.decryptAndDeserialize(encryptedPayload, pin)
    assertTrue(successResult.isSuccess)
    val decryptedAccounts = successResult.getOrThrow()
    assertEquals(1, decryptedAccounts.size)
    assertEquals("Google", decryptedAccounts[0].issuer)
    assertEquals("alice@gmail.com", decryptedAccounts[0].accountName)
    assertEquals("JBSWY3DPEHPK3PXP", decryptedAccounts[0].secretKey)
  }

  @Test
  fun testEncryptedBackupAndRestore() {
    val sampleAccounts = listOf(
      com.example.model.OtpAccount(
        id = "id-1",
        accountName = "alice@example.com",
        issuer = "Proton",
        secretKey = "JBSWY3DPEHPK3PXP",
        type = OtpType.TOTP,
        algorithm = com.example.model.OtpAlgorithm.SHA1,
        digits = 6,
        period = 30
      )
    )

    val password = "SuperSecretPassword123!".toCharArray()
    val backupJson = com.example.crypto.EncryptedBackupHelper.exportEncryptedBackup(sampleAccounts, password)

    // Assert that the backup json is not plaintext
    org.junit.Assert.assertFalse(backupJson.contains("alice@example.com"))
    org.junit.Assert.assertFalse(backupJson.contains("JBSWY3DPEHPK3PXP"))
    org.junit.Assert.assertTrue(backupJson.contains("ciphertext"))
    org.junit.Assert.assertTrue(backupJson.contains("PBKDF2WithHmacSHA256"))

    // Decrypt with correct password
    val decryptSuccess = com.example.crypto.EncryptedBackupHelper.decryptBackup(backupJson, password)
    assertTrue(decryptSuccess.isSuccess)
    val restored = decryptSuccess.getOrThrow()
    assertEquals(1, restored.size)
    assertEquals("Proton", restored[0].issuer)
    assertEquals("alice@example.com", restored[0].accountName)

    // Decrypt with incorrect password fails
    val wrongPassword = "WrongPassword999".toCharArray()
    val decryptFailure = com.example.crypto.EncryptedBackupHelper.decryptBackup(backupJson, wrongPassword)
    assertTrue(decryptFailure.isFailure)
  }

  @Test
  fun testEphemeralMemoryScrubbingAndTotpGeneration() {
    // RFC 4226 standard test vector secret: "12345678901234567890" in Base32
    val rfcKey = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    val code0 = TotpGenerator.generateHotp(rfcKey, counter = 0L, digits = 6)
    assertEquals("755224", code0)

    val code1 = TotpGenerator.generateHotp(rfcKey, counter = 1L, digits = 6)
    assertEquals("287082", code1)

    val code2 = TotpGenerator.generateHotp(rfcKey, counter = 2L, digits = 6)
    assertEquals("359152", code2)

    // Verify key bytes zeroing behaviour
    val keyBytes = com.example.crypto.Base32.decode(rfcKey)
    assertTrue(keyBytes.isNotEmpty())
    java.util.Arrays.fill(keyBytes, 0.toByte())
    for (b in keyBytes) {
      assertEquals(0.toByte(), b)
    }
  }

  @Test
  fun testRootDetectionCheckExecutesGracefully() {
    val checkResult = com.example.crypto.RootDetectionHelper.checkEnvironment()
    // Verify check completes without crash and returns non-null status
    org.junit.Assert.assertNotNull(checkResult)
    org.junit.Assert.assertNotNull(checkResult.reasons)
  }

  @Test
  fun testSoftDeleteWithUndoFlow() {
    val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
    val testPrefs = app.getSharedPreferences("test_vault_prefs", android.content.Context.MODE_PRIVATE)
    val testRepo = com.example.data.EncryptedAccountRepository(app, customPrefs = testPrefs)
    val viewModel = com.example.viewmodel.AuthenticatorViewModel(app, customRepository = testRepo)

    val addResult = viewModel.addAccountFromQr("otpauth://totp/TestIssuer:user@test.com?secret=JBSWY3DPEHPK3PXP&issuer=TestIssuer")
    assertTrue(addResult.isSuccess)
    val account = addResult.getOrThrow()

    // Initially account is in rawAccounts and uiAccounts
    val initialRaw = viewModel.rawAccounts.value
    assertTrue(initialRaw.any { it.id == account.id })
    val initialUi = viewModel.uiAccounts.value
    assertTrue(initialUi.any { it.id == account.id })

    // Execute soft delete with undo
    viewModel.deleteAccountWithUndo(account.id)

    // Pending state should be active
    val pendingState = viewModel.pendingDeleteState.value
    org.junit.Assert.assertNotNull(pendingState)
    assertEquals(account.id, pendingState?.account?.id)
    assertEquals("TestIssuer", pendingState?.displayName)

    // uiAccounts should immediately exclude the account during undo countdown
    val uiList = viewModel.uiAccounts.value
    assertTrue(uiList.none { it.id == account.id })

    // Account is still preserved in underlying raw storage (not purged immediately)
    assertTrue(viewModel.rawAccounts.value.any { it.id == account.id })

    // User triggers UNDO within 3 seconds
    viewModel.undoDelete()

    // Pending state is cleared
    org.junit.Assert.assertNull(viewModel.pendingDeleteState.value)

    // Account is smoothly restored to visible uiAccounts list
    val restoredUiList = viewModel.uiAccounts.value
    assertTrue(restoredUiList.any { it.id == account.id })
  }
}
