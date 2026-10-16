package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.Base32
import com.example.crypto.OtpUriParser
import com.example.crypto.RootDetectionHelper
import com.example.crypto.SecurityCheckResult
import com.example.crypto.TotpGenerator
import com.example.crypto.TransferPayloadHelper
import com.example.data.EncryptedAccountRepository
import com.example.data.PreferencesManager
import com.example.model.OtpAccount
import com.example.model.OtpAlgorithm
import com.example.model.OtpType
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AccountUiItem(
  val id: String,
  val issuer: String,
  val accountName: String,
  val formattedCode: String,
  val rawCode: String,
  val type: OtpType,
  val remainingSeconds: Int,
  val progress: Float,
  val isExpiring: Boolean,
  val counter: Long,
  val isCodeHidden: Boolean = false
)

class AuthenticatorViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = EncryptedAccountRepository(application.applicationContext)
  val preferencesManager = PreferencesManager(application.applicationContext)

  val isStrongBoxBacked: StateFlow<Boolean> = repository.isStrongBoxBacked

  private val _securityCheckResult = MutableStateFlow(RootDetectionHelper.checkEnvironment())
  val securityCheckResult: StateFlow<SecurityCheckResult> = _securityCheckResult.asStateFlow()

  private val _isRootWarningDismissed = MutableStateFlow(false)
  val isRootWarningDismissed: StateFlow<Boolean> = _isRootWarningDismissed.asStateFlow()

  data class UndoDeleteState(
    val account: OtpAccount,
    val displayName: String,
    val durationMillis: Long = 3000L,
    val timestamp: Long = System.currentTimeMillis()
  )

  private val _pendingDeleteState = MutableStateFlow<UndoDeleteState?>(null)
  val pendingDeleteState: StateFlow<UndoDeleteState?> = _pendingDeleteState.asStateFlow()

  private var pendingDeleteJob: Job? = null

  fun dismissRootWarning() {
    _isRootWarningDismissed.value = true
  }

  fun recheckSecurityEnvironment() {
    _securityCheckResult.value = RootDetectionHelper.checkEnvironment()
  }

  private val _rawAccounts = MutableStateFlow<List<OtpAccount>>(emptyList())
  val rawAccounts: StateFlow<List<OtpAccount>> = _rawAccounts.asStateFlow()

  private val _uiAccounts = MutableStateFlow<List<AccountUiItem>>(emptyList())
  val uiAccounts: StateFlow<List<AccountUiItem>> = _uiAccounts.asStateFlow()

  private val _revealedCodeIds = MutableStateFlow<Set<String>>(emptySet())
  val revealedCodeIds: StateFlow<Set<String>> = _revealedCodeIds.asStateFlow()

  private val _isAppUnlocked = MutableStateFlow(!preferencesManager.isAppLockEnabled.value)
  val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

  val isAppLockEnabled: StateFlow<Boolean> = preferencesManager.isAppLockEnabled
  val isHideCodesEnabled: StateFlow<Boolean> = preferencesManager.isHideCodesEnabled
  val isBlockScreenshotsEnabled: StateFlow<Boolean> = preferencesManager.isBlockScreenshotsEnabled
  val isTapToCopyEnabled: StateFlow<Boolean> = preferencesManager.isTapToCopyEnabled
  val sortOrder: StateFlow<String> = preferencesManager.sortOrder
  val themeMode: StateFlow<String> = preferencesManager.themeMode
  val timeOffsetSeconds: StateFlow<Long> = preferencesManager.timeOffsetSeconds

  private val _globalRemainingSeconds = MutableStateFlow(30)
  val globalRemainingSeconds: StateFlow<Int> = _globalRemainingSeconds.asStateFlow()

  private val _globalProgress = MutableStateFlow(1f)
  val globalProgress: StateFlow<Float> = _globalProgress.asStateFlow()

  init {
    viewModelScope.launch {
      repository.accountsFlow.collectLatest { accounts ->
        _rawAccounts.value = accounts
        updateUiState()
      }
    }

    viewModelScope.launch {
      preferencesManager.isHideCodesEnabled.collectLatest {
        updateUiState()
      }
    }

    viewModelScope.launch {
      preferencesManager.sortOrder.collectLatest {
        updateUiState()
      }
    }

    viewModelScope.launch {
      while (isActive) {
        val now = (System.currentTimeMillis() / 1000L) + preferencesManager.timeOffsetSeconds.value
        val remaining = TotpGenerator.getRemainingSeconds(30, now)
        val progress = TotpGenerator.getProgress(30, now)

        _globalRemainingSeconds.value = remaining
        _globalProgress.value = progress

        updateUiState()
        delay(500)
      }
    }
  }

  fun unlockApp() {
    _isAppUnlocked.value = true
  }

  fun lockApp() {
    if (isAppLockEnabled.value) {
      _isAppUnlocked.value = false
    }
  }

  fun setAppLock(enabled: Boolean) {
    preferencesManager.setAppLock(enabled)
    if (!enabled) {
      _isAppUnlocked.value = true
    }
  }

  fun setHideCodes(enabled: Boolean) {
    preferencesManager.setHideCodes(enabled)
    if (!enabled) {
      _revealedCodeIds.value = emptySet()
    }
  }

  fun setBlockScreenshots(enabled: Boolean) {
    preferencesManager.setBlockScreenshots(enabled)
  }

  fun setTapToCopy(enabled: Boolean) {
    preferencesManager.setTapToCopy(enabled)
  }

  fun setSortOrder(order: String) {
    preferencesManager.setSortOrder(order)
    updateUiState()
  }

  fun setThemeMode(mode: String) {
    preferencesManager.setThemeMode(mode)
  }

  fun syncTimeCorrection(): Long {
    val offset = preferencesManager.syncTimeCorrection()
    updateUiState()
    return offset
  }

  fun toggleCodeVisibility(id: String) {
    val current = _revealedCodeIds.value.toMutableSet()
    if (current.contains(id)) {
      current.remove(id)
    } else {
      current.add(id)
    }
    _revealedCodeIds.value = current
    updateUiState()
  }

  private fun updateUiState() {
    val now = (System.currentTimeMillis() / 1000L) + preferencesManager.timeOffsetSeconds.value
    val rawList = _rawAccounts.value
    val pendingDeleteId = _pendingDeleteState.value?.account?.id
    val activeList = if (pendingDeleteId != null) {
      rawList.filter { it.id != pendingDeleteId }
    } else {
      rawList
    }
    val sortOrder = preferencesManager.sortOrder.value

    val sortedAccounts = when (sortOrder) {
      PreferencesManager.SORT_RECENTLY_ADDED -> activeList.sortedByDescending { it.createdAt }
      else -> activeList.sortedWith(
        compareBy(String.CASE_INSENSITIVE_ORDER) {
          if (it.issuer.isNotBlank()) "${it.issuer} ${it.accountName}" else it.accountName
        }
      )
    }

    val hideCodes = isHideCodesEnabled.value
    val revealed = _revealedCodeIds.value

    _uiAccounts.value = sortedAccounts.map { account ->
      val remaining = TotpGenerator.getRemainingSeconds(account.period, now)
      val progress = TotpGenerator.getProgress(account.period, now)
      val isExpiring = remaining <= 5

      val rawCode = if (account.type == OtpType.TOTP) {
        TotpGenerator.generateTotp(
          secretBase32 = account.secretKey,
          timeSeconds = now,
          period = account.period,
          digits = account.digits,
          algorithm = account.algorithm
        )
      } else {
        TotpGenerator.generateHotp(
          secretBase32 = account.secretKey,
          counter = account.counter,
          digits = account.digits,
          algorithm = account.algorithm
        )
      }

      val isHidden = hideCodes && !revealed.contains(account.id)

      AccountUiItem(
        id = account.id,
        issuer = account.issuer,
        accountName = account.accountName,
        formattedCode = TotpGenerator.formatOtp(rawCode),
        rawCode = rawCode,
        type = account.type,
        remainingSeconds = remaining,
        progress = progress,
        isExpiring = isExpiring,
        counter = account.counter,
        isCodeHidden = isHidden
      )
    }
  }

  fun restoreAccounts(accounts: List<OtpAccount>): Pair<Int, Int> {
    return repository.bulkImportAccounts(accounts)
  }

  fun exportSelectedAccounts(selectedIds: Set<String>): Result<String> {
    val accountsToExport = _rawAccounts.value.filter { selectedIds.contains(it.id) }
    if (accountsToExport.isEmpty()) {
      return Result.failure(IllegalArgumentException("No accounts selected for export"))
    }
    val payload = TransferPayloadHelper.serializeAccounts(accountsToExport)
    return Result.success(payload)
  }

  fun importPayload(rawPayload: String): Result<Pair<Int, Int>> {
    val parseResult = TransferPayloadHelper.deserialize(rawPayload)
    if (parseResult.isFailure) {
      return Result.failure(parseResult.exceptionOrNull() ?: Exception("Failed to parse import QR"))
    }

    val accounts = parseResult.getOrThrow()
    val counts = repository.bulkImportAccounts(accounts)
    return Result.success(counts)
  }

  fun addManualAccount(
    name: String,
    secretKey: String,
    type: OtpType
  ): Result<Unit> {
    val trimmedName = name.trim()
    val sanitizedKey = Base32.sanitize(secretKey)

    if (trimmedName.isEmpty()) {
      return Result.failure(IllegalArgumentException("Account name cannot be empty"))
    }

    if (!Base32.isValid(sanitizedKey)) {
      return Result.failure(IllegalArgumentException("Invalid Base32 secret key"))
    }

    val account = OtpAccount(
      id = UUID.randomUUID().toString(),
      accountName = trimmedName,
      issuer = "",
      secretKey = sanitizedKey,
      type = type,
      algorithm = OtpAlgorithm.SHA1,
      digits = 6,
      period = 30,
      counter = 0L
    )

    val success = repository.addAccount(account)
    return if (success) Result.success(Unit) else Result.failure(Exception("Failed to save account"))
  }

  fun addAccountFromQr(qrContent: String): Result<OtpAccount> {
    val parseResult = OtpUriParser.parse(qrContent)
    if (parseResult.isFailure) {
      return parseResult
    }

    val account = parseResult.getOrThrow()
    val success = repository.addAccount(account)
    return if (success) {
      Result.success(account)
    } else {
      Result.failure(Exception("Failed to save scanned account"))
    }
  }

  fun deleteAccountWithUndo(id: String) {
    // If there is an existing pending deletion, permanently commit it first
    commitPendingDeletion()

    val account = _rawAccounts.value.find { it.id == id } ?: return
    val displayName = if (account.issuer.isNotBlank()) account.issuer else account.accountName

    _pendingDeleteState.value = UndoDeleteState(
      account = account,
      displayName = displayName,
      durationMillis = 3000L,
      timestamp = System.currentTimeMillis()
    )
    updateUiState()

    pendingDeleteJob = viewModelScope.launch {
      delay(3000L)
      permanentlyDeletePendingAccount()
    }
  }

  fun undoDelete() {
    pendingDeleteJob?.cancel()
    pendingDeleteJob = null
    _pendingDeleteState.value = null
    updateUiState()
  }

  fun commitPendingDeletion() {
    val pending = _pendingDeleteState.value ?: return
    pendingDeleteJob?.cancel()
    pendingDeleteJob = null
    _pendingDeleteState.value = null
    repository.deleteAccount(pending.account.id)
  }

  private fun permanentlyDeletePendingAccount() {
    val pending = _pendingDeleteState.value ?: return
    _pendingDeleteState.value = null
    repository.deleteAccount(pending.account.id)
  }

  fun deleteAccount(id: String) {
    deleteAccountWithUndo(id)
  }

  override fun onCleared() {
    super.onCleared()
    commitPendingDeletion()
    clipboardClearJob?.cancel()
  }

  fun incrementHotp(id: String) {
    repository.incrementHotpCounter(id)
  }

  private var clipboardClearJob: Job? = null

  fun copyToClipboard(context: Context, item: AccountUiItem) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("2FA Code", item.rawCode)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      clip.description.extras = android.os.PersistableBundle().apply {
        putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
      }
    }
    clipboard.setPrimaryClip(clip)

    val label = if (item.issuer.isNotEmpty()) item.issuer else item.accountName

    // Auto-clear timeout: 30s or when the current TOTP window expires
    val secondsUntilClear = if (item.type == OtpType.TOTP) {
      if (item.remainingSeconds in 1..30) item.remainingSeconds else 30
    } else {
      30
    }

    Toast.makeText(
      context,
      "$label code copied • Auto-clearing in ${secondsUntilClear}s",
      Toast.LENGTH_SHORT
    ).show()

    clipboardClearJob?.cancel()
    val codeToClear = item.rawCode
    clipboardClearJob = viewModelScope.launch {
      delay(secondsUntilClear * 1000L)
      try {
        val currentClip = clipboard.primaryClip
        if (currentClip != null && currentClip.itemCount > 0) {
          val currentText = currentClip.getItemAt(0).text?.toString()
          if (currentText == codeToClear) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
              clipboard.clearPrimaryClip()
            } else {
              clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
          }
        }
      } catch (_: Throwable) {
        // Silently catch in case background clipboard access is restricted by the OS
      }
    }
  }
}
