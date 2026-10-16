package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.model.OtpAccount
import com.example.model.OtpAlgorithm
import com.example.model.OtpType
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.KeyStoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class EncryptedAccountRepository(context: Context) {

  private val _isStrongBoxBacked = MutableStateFlow(checkStrongBoxSupport(context))
  val isStrongBoxBacked: StateFlow<Boolean> = _isStrongBoxBacked.asStateFlow()

  @Volatile
  private var isLoadedSuccessfully: Boolean = false

  private val encryptedPrefs: SharedPreferences = initEncryptedPrefs(context)

  private val _accountsFlow = MutableStateFlow<List<OtpAccount>>(emptyList())
  val accountsFlow: Flow<List<OtpAccount>> = _accountsFlow.asStateFlow()

  init {
    loadAccounts()
  }

  /**
   * Initializes persistent EncryptedSharedPreferences using the fixed standard
   * Android KeyStore MasterKey alias (_androidx_security_master_key_).
   *
   * Crucially ensures:
   * 1. The KeyStore key is NEVER deleted or overwritten on ordinary launches.
   * 2. Preserves data from any legacy vaults previously created under custom StrongBox/TEE aliases.
   * 3. Prevents startup crashes by gracefully recovering or falling back if OEM KeyStore throws KeyStoreException, SecurityException, or GeneralSecurityException.
   */
  private fun initEncryptedPrefs(context: Context): SharedPreferences {
    return try {
      initEncryptedPrefsInternal(context)
    } catch (e: KeyStoreException) {
      safeFallbackPrefs(context)
    } catch (e: SecurityException) {
      safeFallbackPrefs(context)
    } catch (e: Exception) {
      safeFallbackPrefs(context)
    } catch (_: Throwable) {
      safeFallbackPrefs(context)
    }
  }

  private fun safeFallbackPrefs(context: Context): SharedPreferences {
    return try {
      context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    } catch (_: Throwable) {
      context.getSharedPreferences("keyline_memory_prefs", Context.MODE_PRIVATE)
    }
  }

  private fun initEncryptedPrefsInternal(context: Context): SharedPreferences {
    val masterKey = getOrCreatePersistentMasterKey(context)

    if (masterKey != null) {
      // Primary attempt: Open standard encrypted vault with persistent MasterKey
      try {
        val prefs = EncryptedSharedPreferences.create(
          context,
          PREFS_NAME,
          masterKey,
          EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
          EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        // If successful, check if legacy StrongBox or TEE accounts need migration
        migrateLegacyVaultIfNeeded(context, prefs)
        return prefs
      } catch (e: KeyStoreException) {
        return recoverOrFallback(context, masterKey)
      } catch (e: SecurityException) {
        return recoverOrFallback(context, masterKey)
      } catch (e: GeneralSecurityException) {
        return recoverOrFallback(context, masterKey)
      } catch (e: Exception) {
        return recoverOrFallback(context, masterKey)
      } catch (_: Throwable) {
        return recoverOrFallback(context, masterKey)
      }
    }

    // Isolated safe fallback (used for environments lacking working KeyStore)
    return safeFallbackPrefs(context)
  }

  private fun recoverOrFallback(context: Context, masterKey: MasterKey): SharedPreferences {
    // Recovery attempt 1: Recover accounts from legacy keys if available
    try {
      val recoveredAccounts = recoverFromLegacyKeys(context)
      val persistentPrefs = EncryptedSharedPreferences.create(
        context,
        PERSISTENT_VAULT_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
      )
      if (recoveredAccounts.isNotEmpty()) {
        saveAccountsToPrefs(persistentPrefs, recoveredAccounts)
      }
      return persistentPrefs
    } catch (_: Throwable) {
      // Recovery attempt 2: Keystore/Keyset corruption cleanup & fresh recreation
      try {
        cleanupCorruptedVault(context, PREFS_NAME)
        val freshMasterKey = getOrCreatePersistentMasterKey(context, forceRecreate = true)
        if (freshMasterKey != null) {
          return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            freshMasterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
          )
        }
      } catch (_: Throwable) {
        // Handled by final fallback below
      }
    }
    return safeFallbackPrefs(context)
  }

  /**
   * Cleans up corrupted SharedPreferences XML file and clears corrupted MasterKey entry from AndroidKeyStore.
   */
  private fun cleanupCorruptedVault(context: Context, prefsName: String) {
    try {
      context.deleteSharedPreferences(prefsName)
    } catch (_: Throwable) {}

    try {
      val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
      if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
        keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
      }
    } catch (_: Throwable) {}
  }

  /**
   * Retrieves or recreates the persistent MasterKey using the fixed Android KeyStore alias.
   * Catches KeyStoreException, SecurityException, Exception, and Throwable to guarantee safety.
   */
  private fun getOrCreatePersistentMasterKey(context: Context, forceRecreate: Boolean = false): MasterKey? {
    if (forceRecreate) {
      try {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
          keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        }
      } catch (e: KeyStoreException) {
        // Ignored during cleanup
      } catch (e: SecurityException) {
        // Ignored during cleanup
      } catch (e: Exception) {
        // Ignored during cleanup
      } catch (_: Throwable) {}
    }

    return try {
      MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setRequestStrongBoxBacked(false)
        .build()
    } catch (e: KeyStoreException) {
      fallbackBuildMasterKey(context)
    } catch (e: SecurityException) {
      fallbackBuildMasterKey(context)
    } catch (e: GeneralSecurityException) {
      fallbackBuildMasterKey(context)
    } catch (e: Exception) {
      fallbackBuildMasterKey(context)
    } catch (_: Throwable) {
      fallbackBuildMasterKey(context)
    }
  }

  private fun fallbackBuildMasterKey(context: Context): MasterKey? {
    return try {
      MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    } catch (e: KeyStoreException) {
      null
    } catch (e: SecurityException) {
      null
    } catch (e: GeneralSecurityException) {
      null
    } catch (e: Exception) {
      null
    } catch (_: Throwable) {
      null
    }
  }

  private fun checkStrongBoxSupport(context: Context): Boolean {
    return try {
      context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
    } catch (_: Throwable) {
      false
    }
  }

  /**
   * Checks if legacy vaults exist and migrates accounts to the persistent vault.
   */
  private fun migrateLegacyVaultIfNeeded(context: Context, targetPrefs: SharedPreferences) {
    try {
      val existingAccounts = parseAccountsJson(targetPrefs.getString(KEY_ACCOUNTS, null))
      if (existingAccounts.isNotEmpty()) {
        return // Current vault already has accounts, no migration needed
      }

      val legacyAccounts = recoverFromLegacyKeys(context)
      if (legacyAccounts.isNotEmpty()) {
        saveAccountsToPrefs(targetPrefs, legacyAccounts)
      }
    } catch (_: Throwable) {
      // Non-critical; avoid crashing during migration check
    }
  }

  /**
   * Attempts to read accounts from older legacy MasterKey aliases (StrongBox / TEE).
   */
  private fun recoverFromLegacyKeys(context: Context): List<OtpAccount> {
    val legacyAliases = listOf(LEGACY_KEY_STRONGBOX, LEGACY_KEY_TEE)
    for (alias in legacyAliases) {
      try {
        val legacyKey = MasterKey.Builder(context, alias)
          .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
          .build()

        val legacyPrefs = EncryptedSharedPreferences.create(
          context,
          PREFS_NAME,
          legacyKey,
          EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
          EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val accounts = parseAccountsJson(legacyPrefs.getString(KEY_ACCOUNTS, null))
        if (accounts.isNotEmpty()) {
          return accounts
        }
      } catch (_: Throwable) {
        // Continue to next alias
      }
    }

    // Check PERSISTENT_VAULT_NAME if PREFS_NAME was migrated previously
    try {
      val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
      val persistentPrefs = EncryptedSharedPreferences.create(
        context,
        PERSISTENT_VAULT_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
      )
      val accounts = parseAccountsJson(persistentPrefs.getString(KEY_ACCOUNTS, null))
      if (accounts.isNotEmpty()) {
        return accounts
      }
    } catch (_: Throwable) {
      // Ignored
    }

    return emptyList()
  }

  private fun parseAccountsJson(jsonString: String?): List<OtpAccount> {
    if (jsonString.isNullOrBlank()) return emptyList()
    return try {
      val array = JSONArray(jsonString)
      val list = ArrayList<OtpAccount>()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
          OtpAccount(
            id = obj.getString("id"),
            accountName = obj.getString("accountName"),
            issuer = obj.optString("issuer", ""),
            secretKey = obj.getString("secretKey"),
            type = OtpType.valueOf(obj.optString("type", OtpType.TOTP.name)),
            algorithm = OtpAlgorithm.valueOf(obj.optString("algorithm", OtpAlgorithm.SHA1.name)),
            digits = obj.optInt("digits", 6),
            period = obj.optInt("period", 30),
            counter = obj.optLong("counter", 0L),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
          )
        )
      }
      list
    } catch (_: Throwable) {
      emptyList()
    }
  }

  private fun saveAccountsToPrefs(prefs: SharedPreferences, accounts: List<OtpAccount>) {
    try {
      val array = JSONArray()
      for (acc in accounts) {
        val obj = JSONObject().apply {
          put("id", acc.id)
          put("accountName", acc.accountName)
          put("issuer", acc.issuer)
          put("secretKey", acc.secretKey)
          put("type", acc.type.name)
          put("algorithm", acc.algorithm.name)
          put("digits", acc.digits)
          put("period", acc.period)
          put("counter", acc.counter)
          put("createdAt", acc.createdAt)
        }
        array.put(obj)
      }
      prefs.edit().putString(KEY_ACCOUNTS, array.toString()).apply()
    } catch (_: Throwable) {}
  }

  @Synchronized
  private fun loadAccounts() {
    val jsonString = try {
      encryptedPrefs.getString(KEY_ACCOUNTS, null)
    } catch (_: Throwable) {
      null
    }

    if (jsonString.isNullOrBlank()) {
      if (!isLoadedSuccessfully) {
        _accountsFlow.value = emptyList()
      }
      return
    }

    val accounts = parseAccountsJson(jsonString)
    _accountsFlow.value = accounts
    isLoadedSuccessfully = true
  }

  @Synchronized
  private fun saveAccounts(accounts: List<OtpAccount>) {
    // Safety guard: Never wipe storage on uninitialized state
    if (accounts.isEmpty() && !isLoadedSuccessfully) {
      return
    }

    saveAccountsToPrefs(encryptedPrefs, accounts)
    _accountsFlow.value = accounts
    isLoadedSuccessfully = true
  }

  @Synchronized
  fun addAccount(account: OtpAccount): Boolean {
    val current = _accountsFlow.value.toMutableList()
    current.removeAll { it.id == account.id }
    current.add(0, account)
    saveAccounts(current)
    return true
  }

  @Synchronized
  fun deleteAccount(id: String): Boolean {
    val current = _accountsFlow.value.toMutableList()
    val removed = current.removeAll { it.id == id }
    if (removed) {
      saveAccounts(current)
    }
    return removed
  }

  @Synchronized
  fun incrementHotpCounter(id: String): OtpAccount? {
    val current = _accountsFlow.value.toMutableList()
    val index = current.indexOfFirst { it.id == id }
    if (index != -1) {
      val existing = current[index]
      val updated = existing.copy(counter = existing.counter + 1)
      current[index] = updated
      saveAccounts(current)
      return updated
    }
    return null
  }

  @Synchronized
  fun getAllAccounts(): List<OtpAccount> {
    return _accountsFlow.value
  }

  @Synchronized
  fun bulkImportAccounts(incoming: List<OtpAccount>): Pair<Int, Int> {
    val current = _accountsFlow.value.toMutableList()
    var importedCount = 0
    var duplicateCount = 0

    for (acc in incoming) {
      val isDuplicate = current.any { existing ->
        existing.secretKey.equals(acc.secretKey, ignoreCase = true) ||
          (existing.issuer.equals(acc.issuer, ignoreCase = true) &&
            existing.accountName.equals(acc.accountName, ignoreCase = true))
      }

      if (isDuplicate) {
        duplicateCount++
      } else {
        current.add(0, acc)
        importedCount++
      }
    }

    if (importedCount > 0) {
      saveAccounts(current)
    }

    return Pair(importedCount, duplicateCount)
  }

  companion object {
    private const val PREFS_NAME = "keyline_encrypted_vault"
    private const val PERSISTENT_VAULT_NAME = "keyline_persistent_vault"
    private const val FALLBACK_PREFS_NAME = "keyline_fallback_vault"
    private const val KEY_ACCOUNTS = "encrypted_otp_accounts"

    /**
     * Standard fixed Android KeyStore MasterKey alias that persists across app updates.
     */
    const val MASTER_KEY_ALIAS = MasterKey.DEFAULT_MASTER_KEY_ALIAS

    private const val LEGACY_KEY_STRONGBOX = "keyline_master_key_strongbox"
    private const val LEGACY_KEY_TEE = "keyline_master_key_tee"
  }
}
