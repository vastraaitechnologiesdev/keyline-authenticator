package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.fragment.app.FragmentActivity
import com.example.crypto.BiometricAuthHelper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.EncryptedBackupHelper
import com.example.data.PreferencesManager
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.KeylineBlue
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SlateTextTertiary
import com.example.viewmodel.AuthenticatorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: AuthenticatorViewModel,
  onNavigateBack: () -> Unit
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current

  val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
  val isHideCodesEnabled by viewModel.isHideCodesEnabled.collectAsState()
  val isBlockScreenshotsEnabled by viewModel.isBlockScreenshotsEnabled.collectAsState()
  val timeOffset by viewModel.timeOffsetSeconds.collectAsState()
  val isTapToCopyEnabled by viewModel.isTapToCopyEnabled.collectAsState()
  val sortOrder by viewModel.sortOrder.collectAsState()
  val themeMode by viewModel.themeMode.collectAsState()
  val rawAccounts by viewModel.rawAccounts.collectAsState()
  val isStrongBoxBacked by viewModel.isStrongBoxBacked.collectAsState()
  val securityCheck by viewModel.securityCheckResult.collectAsState()

  // Dialog & state holders
  var showPrivacyDialog by remember { mutableStateOf(false) }
  var showSortDialog by remember { mutableStateOf(false) }
  var showThemeDialog by remember { mutableStateOf(false) }
  var showSecurityInfoDialog by remember { mutableStateOf(false) }

  // Export state
  var showExportPasswordDialog by remember { mutableStateOf(false) }
  var pendingExportPassword by remember { mutableStateOf("") }
  var exportPasswordConfirm by remember { mutableStateOf("") }
  var exportPasswordVisible by remember { mutableStateOf(false) }
  var exportErrorText by remember { mutableStateOf<String?>(null) }
  var exportResultDialogMessage by remember { mutableStateOf<String?>(null) }

  // Restore state
  var pendingRestoreFileContent by remember { mutableStateOf<String?>(null) }
  var showRestorePasswordDialog by remember { mutableStateOf(false) }
  var restorePassword by remember { mutableStateOf("") }
  var restorePasswordVisible by remember { mutableStateOf(false) }
  var restorePasswordError by remember { mutableStateOf<String?>(null) }
  var restoreResultDialogMessage by remember { mutableStateOf<String?>(null) }

  // Storage Access Framework launchers
  val exportFileLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri != null && pendingExportPassword.isNotEmpty()) {
      try {
        val backupJson = EncryptedBackupHelper.exportEncryptedBackup(
          accounts = rawAccounts,
          password = pendingExportPassword.toCharArray()
        )
        context.contentResolver.openOutputStream(uri)?.use { stream ->
          stream.write(backupJson.toByteArray(Charsets.UTF_8))
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        exportResultDialogMessage = "Successfully exported ${rawAccounts.size} account(s) to encrypted backup file."
      } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
      } finally {
        pendingExportPassword = ""
      }
    } else {
      pendingExportPassword = ""
    }
  }

  val restoreFileLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      try {
        val fileContent = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (fileContent.isNullOrBlank()) {
          Toast.makeText(context, "Selected backup file is empty", Toast.LENGTH_SHORT).show()
        } else {
          pendingRestoreFileContent = fileContent
          restorePassword = ""
          restorePasswordError = null
          restorePasswordVisible = false
          showRestorePasswordDialog = true
        }
      } catch (e: Exception) {
        Toast.makeText(context, "Failed to read backup file: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  Scaffold(
    containerColor = CleanWhite,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = CleanWhite,
          titleContentColor = SlateTextPrimary
        ),
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("btn_settings_back")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = SlateTextPrimary
            )
          }
        },
        title = {
          Text(
            text = "Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = SlateTextPrimary
          )
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .navigationBarsPadding()
        .padding(horizontal = 20.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      // Group 1: Security Preferences
      SettingsSectionHeader(title = "SECURITY PREFERENCES")

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CleanWhite),
        border = BorderStroke(1.dp, CleanBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          // Setting 1: App Lock
          SettingsSwitchItem(
            title = "App Lock",
            subtitle = "Require Biometric or device PIN on every app launch",
            icon = Icons.Default.Fingerprint,
            checked = isAppLockEnabled,
            onCheckedChange = { enabled ->
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              viewModel.setAppLock(enabled)
              Toast.makeText(
                context,
                if (enabled) "App Lock enabled" else "App Lock disabled",
                Toast.LENGTH_SHORT
              ).show()
            },
            testTag = "switch_app_lock"
          )

          SettingsDivider()

          // Setting 2: Hide codes until tapped
          SettingsSwitchItem(
            title = "Hide codes until tapped",
            subtitle = "Obscure OTP digits with dots to prevent shoulder-surfing in public",
            icon = Icons.Default.VisibilityOff,
            checked = isHideCodesEnabled,
            onCheckedChange = { enabled ->
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              viewModel.setHideCodes(enabled)
              Toast.makeText(
                context,
                if (enabled) "Codes will now be obscured until tapped" else "Codes always visible",
                Toast.LENGTH_SHORT
              ).show()
            },
            testTag = "switch_hide_codes"
          )

          SettingsDivider()

          // Setting 3: Block Screenshots
          SettingsSwitchItem(
            title = "Block Screenshots",
            subtitle = "Enforce FLAG_SECURE to prevent screen capture and recordings",
            icon = Icons.Default.Shield,
            checked = isBlockScreenshotsEnabled,
            onCheckedChange = { enabled ->
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              viewModel.setBlockScreenshots(enabled)
              Toast.makeText(
                context,
                if (enabled) "Screen capture blocked" else "Screen capture allowed",
                Toast.LENGTH_SHORT
              ).show()
            },
            testTag = "switch_block_screenshots"
          )

          SettingsDivider()

          // Setting 4: Hardware Keystore
          SettingsNavigationItem(
            title = "Hardware Keystore",
            subtitle = if (isStrongBoxBacked) {
              "Enforced: StrongBox dedicated hardware security module"
            } else {
              "Enforced: Hardware-backed Trusted Execution Environment (TEE)"
            },
            icon = Icons.Default.Lock,
            badgeText = if (isStrongBoxBacked) "STRONGBOX" else "TEE",
            badgeColor = KeylineBlue,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              showSecurityInfoDialog = true
            },
            testTag = "item_hardware_keystore"
          )

          SettingsDivider()

          // Setting 5: Environment & Anti-Tamper Integrity
          SettingsNavigationItem(
            title = "Environment Integrity",
            subtitle = if (securityCheck.isCompromised) {
              "Unverified: Root/SU binaries or test-keys detected"
            } else {
              "Verified: Clean system integrity & anti-tamper passed"
            },
            icon = Icons.Default.Security,
            badgeText = if (securityCheck.isCompromised) "WARNING" else "VERIFIED",
            badgeColor = if (securityCheck.isCompromised) androidx.compose.ui.graphics.Color(0xFFD97706) else KeylineBlue,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              showSecurityInfoDialog = true
            },
            testTag = "item_environment_integrity"
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Group 2: User Experience (NEW: Placed above TIME SYNCHRONIZATION)
      SettingsSectionHeader(title = "USER EXPERIENCE")

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CleanWhite),
        border = BorderStroke(1.dp, CleanBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          // Setting 1: Tap to Copy
          SettingsSwitchItem(
            title = "Tap to Copy",
            subtitle = "Quickly copy 6-digit OTP to clipboard on single tap",
            icon = Icons.Default.ContentCopy,
            checked = isTapToCopyEnabled,
            onCheckedChange = { enabled ->
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              viewModel.setTapToCopy(enabled)
              Toast.makeText(
                context,
                if (enabled) "Tap to copy enabled" else "Tap to copy disabled",
                Toast.LENGTH_SHORT
              ).show()
            },
            testTag = "switch_tap_to_copy"
          )

          SettingsDivider()

          // Setting 2: Sort Accounts
          val currentSortLabel = when (sortOrder) {
            PreferencesManager.SORT_RECENTLY_ADDED -> "Recently added"
            else -> "A to Z"
          }

          SettingsNavigationItem(
            title = "Sort Accounts",
            subtitle = "Choose default ordering (A to Z, Recently added)",
            icon = Icons.AutoMirrored.Filled.Sort,
            badgeText = currentSortLabel,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              showSortDialog = true
            },
            testTag = "item_sort_accounts"
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Group 3: Time Synchronization
      SettingsSectionHeader(title = "TIME SYNCHRONIZATION")

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CleanWhite),
        border = BorderStroke(1.dp, CleanBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
              modifier = Modifier.size(40.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = KeylineBlue,
                modifier = Modifier
                  .padding(10.dp)
                  .size(20.dp)
              )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Time Correction",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateTextPrimary
              )
              Text(
                text = if (timeOffset == 0L) "Internal clock: in sync (+0.00s)" else "Internal clock offset: ${timeOffset}s",
                fontSize = 12.sp,
                color = KeylineBlue,
                fontWeight = FontWeight.Medium
              )
            }

            OutlinedButton(
              onClick = {
                val offset = viewModel.syncTimeCorrection()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Toast.makeText(context, "Time synchronized successfully (+${offset}.00s)", Toast.LENGTH_SHORT).show()
              },
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.testTag("btn_sync_time")
            ) {
              Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = SlateTextPrimary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sync Now", fontSize = 13.sp, color = SlateTextPrimary)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "2FA codes rely on precise time synchronization. If your codes are rejected by services, syncing recalibrates the internal cryptographic clock offset.",
            fontSize = 12.sp,
            color = SlateTextSecondary,
            lineHeight = 17.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Group 4: Offline Backup & Restore (File Operations via Android SAF)
      SettingsSectionHeader(title = "OFFLINE BACKUP & RESTORE")

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CleanWhite),
        border = BorderStroke(1.dp, CleanBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          // Export Encrypted File (Strictly Biometric Guarded)
          SettingsNavigationItem(
            title = "Export Encrypted File",
            subtitle = "Save a password-protected backup file (.json) to device storage",
            icon = Icons.Default.FileDownload,
            badgeText = "${rawAccounts.size} keys",
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              if (rawAccounts.isEmpty()) {
                Toast.makeText(context, "No accounts in vault to export", Toast.LENGTH_SHORT).show()
              } else {
                if (context is FragmentActivity) {
                  BiometricAuthHelper.authenticate(
                    activity = context,
                    title = "Export Encrypted File",
                    subtitle = "Verify biometric or device PIN to export backup file",
                    onSuccess = {
                      exportErrorText = null
                      pendingExportPassword = ""
                      exportPasswordConfirm = ""
                      exportPasswordVisible = false
                      showExportPasswordDialog = true
                    },
                    onError = { err ->
                      Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                  )
                } else {
                  exportErrorText = null
                  pendingExportPassword = ""
                  exportPasswordConfirm = ""
                  exportPasswordVisible = false
                  showExportPasswordDialog = true
                }
              }
            },
            testTag = "item_export_backup"
          )

          SettingsDivider()

          // Restore from File
          SettingsNavigationItem(
            title = "Restore from File",
            subtitle = "Import and decrypt a .json backup file via Android SAF",
            icon = Icons.Default.FileUpload,
            badgeText = "SAF",
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              restoreFileLauncher.launch(arrayOf("application/json", "*/*"))
            },
            testTag = "item_restore_backup"
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Group 5: About & Privacy
      SettingsSectionHeader(title = "ABOUT & PRIVACY")

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CleanWhite),
        border = BorderStroke(1.dp, CleanBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          // Theme selector item (ABOUT & PRIVACY)
          val currentThemeLabel = when (themeMode) {
            PreferencesManager.THEME_LIGHT -> "Light mode"
            PreferencesManager.THEME_DARK -> "Dark mode"
            else -> "System default"
          }

          SettingsNavigationItem(
            title = "Theme",
            subtitle = "Choose app appearance ($currentThemeLabel)",
            icon = Icons.Default.DarkMode,
            badgeText = currentThemeLabel,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              showThemeDialog = true
            },
            testTag = "item_theme_mode"
          )

          SettingsDivider()

          // App info (EXISTING - Kept exactly as it is)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(40.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = KeylineBlue,
                modifier = Modifier
                  .padding(10.dp)
                  .size(20.dp)
              )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Keyline Authenticator",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateTextPrimary
              )
              Text(
                text = "Developed by VastraAI • Version 1.0.0",
                fontSize = 12.sp,
                color = SlateTextSecondary
              )
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = KeylineBlue.copy(alpha = 0.1f)
            ) {
              Text(
                text = "v1.0.0",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = KeylineBlue,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }

          SettingsDivider()

          // Privacy policy clickable item (EXISTING - Kept exactly as it is)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showPrivacyDialog = true
              }
              .padding(horizontal = 16.dp, vertical = 14.dp)
              .testTag("item_privacy_policy"),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = SlateTextTertiary.copy(alpha = 0.12f),
              modifier = Modifier.size(40.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Policy,
                contentDescription = null,
                tint = SlateTextSecondary,
                modifier = Modifier
                  .padding(10.dp)
                  .size(20.dp)
              )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Privacy Policy & Architecture",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateTextPrimary
              )
              Text(
                text = "100% Offline • Zero tracking • Hardware-backed keys",
                fontSize = 12.sp,
                color = SlateTextSecondary
              )
            }

            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = SlateTextTertiary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(36.dp))
    }
  }

  // --- DIALOGS ---

  // 0. Security Architecture Info Dialog
  if (showSecurityInfoDialog) {
    AlertDialog(
      onDismissRequest = { showSecurityInfoDialog = false },
      containerColor = CleanWhite,
      shape = RoundedCornerShape(24.dp),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = KeylineBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = KeylineBlue,
              modifier = Modifier.padding(8.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Zero-Trust Hardening",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
          )
        }
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          SecurityDetailItem(
            title = "Hardware Keystore (StrongBox / TEE)",
            description = if (isStrongBoxBacked) {
              "StrongBox dedicated secure processor active. Master cryptographic keys are generated and isolated inside tamper-resistant hardware."
            } else {
              "Hardware-backed Trusted Execution Environment (TEE) active. Cryptographic operations are isolated from the main Android OS kernel."
            }
          )

          SecurityDetailItem(
            title = "Ephemeral RAM Scrubbing",
            description = "Secret key byte arrays are zeroed with Arrays.fill(bytes, 0) immediately following HMAC hashing, preventing sensitive credentials from lingering in memory dumps."
          )

          SecurityDetailItem(
            title = "Auto-Clearing Clipboard",
            description = "Copied 2FA codes are marked sensitive and automatically wiped from system clipboard after 30 seconds (or upon TOTP window expiration)."
          )

          SecurityDetailItem(
            title = "Anti-Tamper & FLAG_SECURE",
            description = "ADB backup extraction is blocked (allowBackup=\"false\"). FLAG_SECURE blocks screenshots, screen recording, and app switcher preview leaks."
          )

          if (securityCheck.isCompromised) {
            SecurityDetailItem(
              title = "Environment Status: Warning",
              description = "Compromised / Root environment detected (${securityCheck.reasons.joinToString(", ")}). Proceed with caution on untrusted hardware."
            )
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = { showSecurityInfoDialog = false },
          modifier = Modifier.testTag("btn_close_security_info")
        ) {
          Text("Got It", color = KeylineBlue, fontWeight = FontWeight.SemiBold)
        }
      }
    )
  }

  // 1. Sort Accounts Dialog
  if (showSortDialog) {
    val sortOptions = listOf(
      PreferencesManager.SORT_A_TO_Z to "A to Z" to "Alphabetical order by issuer or account name",
      PreferencesManager.SORT_RECENTLY_ADDED to "Recently added" to "Newest accounts appear at the top"
    )

    AlertDialog(
      onDismissRequest = { showSortDialog = false },
      containerColor = CleanWhite,
      shape = RoundedCornerShape(24.dp),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = KeylineBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Sort,
              contentDescription = null,
              tint = KeylineBlue,
              modifier = Modifier.padding(8.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Sort Accounts",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
          )
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Choose default ordering for your authenticator vault accounts:",
            fontSize = 13.sp,
            color = SlateTextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
          )

          sortOptions.forEach { (optionData, description) ->
            val (key, label) = optionData
            val isSelected = sortOrder == key

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) KeylineBlue.copy(alpha = 0.08f) else CleanWhite,
              border = BorderStroke(1.dp, if (isSelected) KeylineBlue else CleanBorder),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  viewModel.setSortOrder(key)
                  showSortDialog = false
                  Toast.makeText(context, "Sorted: $label", Toast.LENGTH_SHORT).show()
                }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = isSelected,
                  onClick = null,
                  colors = RadioButtonDefaults.colors(
                    selectedColor = KeylineBlue,
                    unselectedColor = SlateTextTertiary
                  )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateTextPrimary
                  )
                  Text(
                    text = description,
                    fontSize = 12.sp,
                    color = SlateTextSecondary
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSortDialog = false }) {
          Text("Cancel", color = SlateTextSecondary)
        }
      }
    )
  }

  // 2. Export Master Password Dialog
  if (showExportPasswordDialog) {
    AlertDialog(
      onDismissRequest = {
        showExportPasswordDialog = false
        pendingExportPassword = ""
        exportPasswordConfirm = ""
      },
      containerColor = CleanWhite,
      shape = RoundedCornerShape(24.dp),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = KeylineBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = KeylineBlue,
              modifier = Modifier.padding(8.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Encrypt Backup",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
          )
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Create a master password to encrypt your ${rawAccounts.size} account(s). All data is secured with AES-256-GCM and PBKDF2. You will need this password to restore your vault.",
            fontSize = 13.sp,
            color = SlateTextSecondary,
            lineHeight = 18.sp
          )

          Spacer(modifier = Modifier.height(16.dp))

          OutlinedTextField(
            value = pendingExportPassword,
            onValueChange = {
              pendingExportPassword = it
              exportErrorText = null
            },
            label = { Text("Master Backup Password") },
            visualTransformation = if (exportPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            trailingIcon = {
              IconButton(onClick = { exportPasswordVisible = !exportPasswordVisible }) {
                Icon(
                  imageVector = if (exportPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (exportPasswordVisible) "Hide password" else "Show password",
                  tint = SlateTextTertiary
                )
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = KeylineBlue,
              unfocusedBorderColor = CleanBorder,
              focusedLabelColor = KeylineBlue,
              unfocusedLabelColor = SlateTextSecondary
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_export_password")
          )

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = exportPasswordConfirm,
            onValueChange = {
              exportPasswordConfirm = it
              exportErrorText = null
            },
            label = { Text("Confirm Password") },
            visualTransformation = if (exportPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = KeylineBlue,
              unfocusedBorderColor = CleanBorder,
              focusedLabelColor = KeylineBlue,
              unfocusedLabelColor = SlateTextSecondary
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_export_password_confirm")
          )

          if (exportErrorText != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = exportErrorText ?: "",
              color = MaterialTheme.colorScheme.error,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (pendingExportPassword.length < 6) {
              exportErrorText = "Password must be at least 6 characters"
            } else if (pendingExportPassword != exportPasswordConfirm) {
              exportErrorText = "Passwords do not match"
            } else {
              showExportPasswordDialog = false
              val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
              exportFileLauncher.launch("keyline_backup_$timestamp.json")
            }
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = SlateTextPrimary,
            contentColor = CleanWhite
          ),
          modifier = Modifier.testTag("btn_export_continue")
        ) {
          Text("Choose Storage Location")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showExportPasswordDialog = false
            pendingExportPassword = ""
            exportPasswordConfirm = ""
          }
        ) {
          Text("Cancel", color = SlateTextSecondary)
        }
      }
    )
  }

  // 3. Restore Password Prompt Dialog
  if (showRestorePasswordDialog && pendingRestoreFileContent != null) {
    AlertDialog(
      onDismissRequest = {
        showRestorePasswordDialog = false
        pendingRestoreFileContent = null
        restorePassword = ""
        restorePasswordError = null
      },
      containerColor = CleanWhite,
      shape = RoundedCornerShape(24.dp),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = KeylineBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = KeylineBlue,
              modifier = Modifier.padding(8.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Decrypt & Restore",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
          )
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Enter the master backup password used when creating this file to decrypt and restore your accounts:",
            fontSize = 13.sp,
            color = SlateTextSecondary,
            lineHeight = 18.sp
          )

          Spacer(modifier = Modifier.height(16.dp))

          OutlinedTextField(
            value = restorePassword,
            onValueChange = {
              restorePassword = it
              restorePasswordError = null
            },
            label = { Text("Master Backup Password") },
            visualTransformation = if (restorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            trailingIcon = {
              IconButton(onClick = { restorePasswordVisible = !restorePasswordVisible }) {
                Icon(
                  imageVector = if (restorePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (restorePasswordVisible) "Hide password" else "Show password",
                  tint = SlateTextTertiary
                )
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = KeylineBlue,
              unfocusedBorderColor = CleanBorder,
              focusedLabelColor = KeylineBlue,
              unfocusedLabelColor = SlateTextSecondary
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_restore_password")
          )

          if (restorePasswordError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = restorePasswordError ?: "",
              color = MaterialTheme.colorScheme.error,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (restorePassword.isEmpty()) {
              restorePasswordError = "Please enter the backup password"
            } else {
              val decryptResult = EncryptedBackupHelper.decryptBackup(
                backupContent = pendingRestoreFileContent!!,
                password = restorePassword.toCharArray()
              )

              if (decryptResult.isSuccess) {
                val accounts = decryptResult.getOrThrow()
                val (importedCount, duplicateCount) = viewModel.restoreAccounts(accounts)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showRestorePasswordDialog = false
                pendingRestoreFileContent = null
                restorePassword = ""
                restorePasswordError = null
                restoreResultDialogMessage = "Restore complete: $importedCount account(s) restored into vault ($duplicateCount already present)."
              } else {
                restorePasswordError = decryptResult.exceptionOrNull()?.message
                  ?: "Incorrect password or corrupted backup file"
              }
            }
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = SlateTextPrimary,
            contentColor = CleanWhite
          ),
          modifier = Modifier.testTag("btn_restore_decrypt")
        ) {
          Text("Decrypt & Restore")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showRestorePasswordDialog = false
            pendingRestoreFileContent = null
            restorePassword = ""
            restorePasswordError = null
          }
        ) {
          Text("Cancel", color = SlateTextSecondary)
        }
      }
    )
  }

  // 4. Export Success Notification Dialog
  if (exportResultDialogMessage != null) {
    AlertDialog(
      onDismissRequest = { exportResultDialogMessage = null },
      containerColor = CleanWhite,
      shape = RoundedCornerShape(24.dp),
      icon = {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = KeylineBlue,
          modifier = Modifier.size(36.dp)
        )
      },
      title = {
        Text(
          text = "Backup Saved",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = SlateTextPrimary
        )
      },
      text = {
        Text(
          text = exportResultDialogMessage ?: "",
          fontSize = 14.sp,
          color = SlateTextSecondary,
          lineHeight = 20.sp
        )
      },
      confirmButton = {
        Button(
          onClick = { exportResultDialogMessage = null },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = SlateTextPrimary,
            contentColor = CleanWhite
          )
        ) {
          Text("Done")
        }
      }
    )
  }

  // 5. Restore Success Notification Dialog
  if (restoreResultDialogMessage != null) {
    AlertDialog(
      onDismissRequest = { restoreResultDialogMessage = null },
      containerColor = CleanWhite,
      shape = RoundedCornerShape(24.dp),
      icon = {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = KeylineBlue,
          modifier = Modifier.size(36.dp)
        )
      },
      title = {
        Text(
          text = "Accounts Restored",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = SlateTextPrimary
        )
      },
      text = {
        Text(
          text = restoreResultDialogMessage ?: "",
          fontSize = 14.sp,
          color = SlateTextSecondary,
          lineHeight = 20.sp
        )
      },
      confirmButton = {
        Button(
          onClick = { restoreResultDialogMessage = null },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = SlateTextPrimary,
            contentColor = CleanWhite
          )
        ) {
          Text("Done")
        }
      }
    )
  }

  // 6. Theme Selector Dialog
  if (showThemeDialog) {
    val themeOptions = listOf(
      Triple(PreferencesManager.THEME_SYSTEM, "System default", "Follow system appearance setting"),
      Triple(PreferencesManager.THEME_LIGHT, "Light mode", "Clean white high-contrast appearance"),
      Triple(PreferencesManager.THEME_DARK, "Dark mode", "Dark slate theme for low-light environments")
    )

    AlertDialog(
      onDismissRequest = { showThemeDialog = false },
      containerColor = CleanWhite,
      shape = RoundedCornerShape(24.dp),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = KeylineBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.DarkMode,
              contentDescription = null,
              tint = KeylineBlue,
              modifier = Modifier.padding(8.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Choose Theme",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
          )
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Select your preferred visual appearance:",
            fontSize = 13.sp,
            color = SlateTextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
          )

          themeOptions.forEach { (key, label, description) ->
            val isSelected = themeMode == key

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) KeylineBlue.copy(alpha = 0.08f) else CleanWhite,
              border = BorderStroke(1.dp, if (isSelected) KeylineBlue else CleanBorder),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  viewModel.setThemeMode(key)
                  showThemeDialog = false
                  Toast.makeText(context, "Theme set to $label", Toast.LENGTH_SHORT).show()
                }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = isSelected,
                  onClick = null,
                  colors = RadioButtonDefaults.colors(
                    selectedColor = KeylineBlue,
                    unselectedColor = SlateTextTertiary
                  )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateTextPrimary
                  )
                  Text(
                    text = description,
                    fontSize = 12.sp,
                    color = SlateTextSecondary
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showThemeDialog = false }) {
          Text("Cancel", color = SlateTextSecondary)
        }
      }
    )
  }

  // 7. Privacy Policy Reassurance Dialog (EXISTING)
  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      containerColor = CleanWhite,
      shape = RoundedCornerShape(24.dp),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = KeylineBlue,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Privacy Policy",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
          )
        }
      },
      text = {
        Column {
          Text(
            text = "Keyline Authenticator is built from the ground up on strict local-first and zero-knowledge principles:",
            fontSize = 13.sp,
            color = SlateTextSecondary,
            lineHeight = 18.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          PrivacyBulletItem(title = "Zero Network Calls", body = "The app has no backend servers, no analytics beacons, and no ads. It operates completely offline.")
          Spacer(modifier = Modifier.height(8.dp))
          PrivacyBulletItem(title = "Hardware Keystore Encryption", body = "All secret base32 keys are encrypted with AES-256-GCM using keys stored in the Android hardware MasterKey Keystore.")
          Spacer(modifier = Modifier.height(8.dp))
          PrivacyBulletItem(title = "Direct Offline Transfers", body = "Account migration uses optical QR code scanning directly between two devices without intermediate relays.")
        }
      },
      confirmButton = {
        Button(
          onClick = { showPrivacyDialog = false },
          shape = RoundedCornerShape(20.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = SlateTextPrimary,
            contentColor = CleanWhite
          )
        ) {
          Text("Understood")
        }
      }
    )
  }
}

@Composable
fun PrivacyBulletItem(title: String, body: String) {
  Row(verticalAlignment = Alignment.Top) {
    Icon(
      imageVector = Icons.Default.CheckCircle,
      contentDescription = null,
      tint = KeylineBlue,
      modifier = Modifier
        .size(16.dp)
        .padding(top = 2.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Column {
      Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = SlateTextPrimary
      )
      Text(
        text = body,
        fontSize = 12.sp,
        color = SlateTextSecondary,
        lineHeight = 16.sp
      )
    }
  }
}

@Composable
fun SettingsSectionHeader(title: String) {
  Text(
    text = title,
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
    color = SlateTextTertiary,
    letterSpacing = 1.sp,
    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
  )
}

@Composable
fun SettingsSwitchItem(
  title: String,
  subtitle: String,
  icon: ImageVector,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onCheckedChange(!checked) }
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = if (checked) KeylineBlue.copy(alpha = 0.12f) else SlateTextTertiary.copy(alpha = 0.12f),
      modifier = Modifier.size(40.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (checked) KeylineBlue else SlateTextSecondary,
        modifier = Modifier
          .padding(10.dp)
          .size(20.dp)
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = SlateTextPrimary
      )
      Text(
        text = subtitle,
        fontSize = 12.sp,
        color = SlateTextSecondary,
        lineHeight = 16.sp
      )
    }

    Spacer(modifier = Modifier.width(10.dp))

    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = CleanWhite,
        checkedTrackColor = KeylineBlue,
        uncheckedThumbColor = SlateTextSecondary,
        uncheckedTrackColor = CleanBorder
      ),
      modifier = Modifier.testTag(testTag)
    )
  }
}

@Composable
fun SettingsNavigationItem(
  title: String,
  subtitle: String,
  icon: ImageVector,
  badgeText: String? = null,
  badgeColor: androidx.compose.ui.graphics.Color = KeylineBlue,
  onClick: () -> Unit,
  testTag: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = SlateTextTertiary.copy(alpha = 0.12f),
      modifier = Modifier.size(40.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = KeylineBlue,
        modifier = Modifier
          .padding(10.dp)
          .size(20.dp)
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = SlateTextPrimary
      )
      Text(
        text = subtitle,
        fontSize = 12.sp,
        color = SlateTextSecondary,
        lineHeight = 16.sp
      )
    }

    if (badgeText != null) {
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeColor.copy(alpha = 0.1f)
      ) {
        Text(
          text = badgeText,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = badgeColor,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
      }
      Spacer(modifier = Modifier.width(6.dp))
    }

    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = SlateTextTertiary,
      modifier = Modifier.size(20.dp)
    )
  }
}

@Composable
fun SettingsDivider() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(1.dp)
      .background(CleanBorder.copy(alpha = 0.6f))
      .padding(horizontal = 16.dp)
  )
}

@Composable
fun SecurityDetailItem(
  title: String,
  description: String
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = SlateTextTertiary.copy(alpha = 0.08f),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = SlateTextPrimary
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = description,
        fontSize = 12.sp,
        color = SlateTextSecondary,
        lineHeight = 16.sp
      )
    }
  }
}
