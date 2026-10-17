package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.crypto.BiometricAuthHelper
import com.example.crypto.QrCodeGenerator
import com.example.model.OtpAccount
import com.example.model.OtpType
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.KeylineBlue
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SlateTextTertiary
import com.example.viewmodel.AuthenticatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportAccountsScreen(
  viewModel: AuthenticatorViewModel,
  onNavigateBack: () -> Unit
) {
  val context = LocalContext.current
  val accounts by viewModel.rawAccounts.collectAsState()

  var isAuthenticated by remember { mutableStateOf(false) }
  var authErrorMessage by remember { mutableStateOf<String?>(null) }

  var selectedAccountIds by remember { mutableStateOf(accounts.map { it.id }.toSet()) }
  var exportQrBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var exportedCount by remember { mutableStateOf(0) }

  DisposableEffect(Unit) {
    onDispose {
      try {
        exportQrBitmap?.recycle()
      } catch (_: Throwable) {}
      exportQrBitmap = null
    }
  }

  fun triggerAuthentication() {
    if (context is FragmentActivity) {
      BiometricAuthHelper.authenticate(
        activity = context,
        title = "Unlock Export Vault",
        subtitle = "Verify biometric or device PIN to export 2FA accounts",
        onSuccess = {
          isAuthenticated = true
          authErrorMessage = null
        },
        onError = { err ->
          authErrorMessage = err
        }
      )
    } else {
      authErrorMessage = "Device lock or biometric authentication is required."
    }
  }

  LaunchedEffect(Unit) {
    triggerAuthentication()
  }

  // Update selection if accounts change
  LaunchedEffect(accounts) {
    if (selectedAccountIds.isEmpty() && accounts.isNotEmpty()) {
      selectedAccountIds = accounts.map { it.id }.toSet()
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
            modifier = Modifier.testTag("btn_export_back")
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
            text = "Export accounts",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = SlateTextPrimary
          )
        }
      )
    },
    bottomBar = {
      if (isAuthenticated && accounts.isNotEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(CleanWhite)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
          Button(
            onClick = {
              val result = viewModel.exportSelectedAccounts(selectedAccountIds)
              if (result.isSuccess) {
                val payload = result.getOrThrow()
                try {
                  val bitmap = QrCodeGenerator.generateQrBitmap(payload, sizePx = 650)
                  exportQrBitmap = bitmap
                  exportedCount = selectedAccountIds.size
                } catch (e: Exception) {
                  Toast.makeText(context, "Failed to generate QR: ${e.message}", Toast.LENGTH_SHORT).show()
                }
              } else {
                Toast.makeText(context, result.exceptionOrNull()?.message ?: "Export failed", Toast.LENGTH_SHORT).show()
              }
            },
            enabled = selectedAccountIds.isNotEmpty(),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("btn_generate_export_qr"),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = SlateTextPrimary,
              contentColor = CleanWhite,
              disabledContainerColor = SlateTextTertiary.copy(alpha = 0.3f),
              disabledContentColor = CleanWhite.copy(alpha = 0.6f)
            )
          ) {
            Icon(
              imageVector = Icons.Default.QrCode,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (selectedAccountIds.isEmpty()) "Select accounts" else "Export ${selectedAccountIds.size} ${if (selectedAccountIds.size == 1) "account" else "accounts"}",
              fontSize = 15.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }
  ) { innerPadding ->
    if (!isAuthenticated) {
      // Biometric / Device Lock required fallback screen
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .navigationBarsPadding()
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(80.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Biometric Lock",
            tint = KeylineBlue,
            modifier = Modifier
              .padding(18.dp)
              .size(44.dp)
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
          text = "Authentication Required",
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = SlateTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = authErrorMessage ?: "Please authenticate with your fingerprint, face, or device PIN to view and export secret 2FA keys.",
          fontSize = 14.sp,
          color = SlateTextSecondary,
          textAlign = TextAlign.Center,
          lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
          onClick = { triggerAuthentication() },
          shape = RoundedCornerShape(24.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = SlateTextPrimary,
            contentColor = CleanWhite
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_retry_auth")
        ) {
          Text("Authenticate", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    } else if (accounts.isEmpty()) {
      // Empty vault state
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .navigationBarsPadding()
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Surface(
          shape = CircleShape,
          color = SlateTextTertiary.copy(alpha = 0.15f),
          modifier = Modifier.size(72.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = SlateTextSecondary,
            modifier = Modifier
              .padding(18.dp)
              .size(36.dp)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "No Accounts in Vault",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = SlateTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "You don't have any 2FA accounts to export yet. Add your first account on the home screen.",
          fontSize = 14.sp,
          color = SlateTextSecondary,
          textAlign = TextAlign.Center,
          lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
          onClick = onNavigateBack,
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier.testTag("btn_empty_back")
        ) {
          Text("Return to Home")
        }
      }
    } else {
      // Multi-select list
      val allSelected = selectedAccountIds.size == accounts.size

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
      ) {
        item {
          // Select All Header row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                selectedAccountIds = if (allSelected) {
                  emptySet()
                } else {
                  accounts.map { it.id }.toSet()
                }
              }
              .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = allSelected,
              onCheckedChange = { isChecked ->
                selectedAccountIds = if (isChecked) {
                  accounts.map { it.id }.toSet()
                } else {
                  emptySet()
                }
              },
              colors = CheckboxDefaults.colors(
                checkedColor = KeylineBlue,
                checkmarkColor = CleanWhite
              ),
              modifier = Modifier.testTag("checkbox_select_all")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
              Text(
                text = "Select all accounts",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateTextPrimary
              )
              Text(
                text = "${selectedAccountIds.size} of ${accounts.size} selected",
                fontSize = 12.sp,
                color = SlateTextSecondary
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
        }

        items(accounts, key = { it.id }) { account ->
          val isSelected = selectedAccountIds.contains(account.id)
          ExportAccountItemCard(
            account = account,
            isSelected = isSelected,
            onToggle = {
              val current = selectedAccountIds.toMutableSet()
              if (isSelected) {
                current.remove(account.id)
              } else {
                current.add(account.id)
              }
              selectedAccountIds = current
            }
          )
          Spacer(modifier = Modifier.height(10.dp))
        }

        item {
          Spacer(modifier = Modifier.height(80.dp))
        }
      }
    }
  }

  // QR Code Display Modal Dialog
  if (exportQrBitmap != null) {
    AlertDialog(
      onDismissRequest = {
        try {
          exportQrBitmap?.recycle()
        } catch (_: Throwable) {}
        exportQrBitmap = null
      },
      containerColor = CleanWhite,
      shape = RoundedCornerShape(24.dp),
      title = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Export QR Code",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "$exportedCount ${if (exportedCount == 1) "account" else "accounts"} encoded",
            fontSize = 12.sp,
            color = KeylineBlue,
            fontWeight = FontWeight.Medium
          )
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // QR Code Frame
          Box(
            modifier = Modifier
              .size(260.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(CleanWhite)
              .border(1.dp, CleanBorder, RoundedCornerShape(16.dp))
              .padding(12.dp)
          ) {
            Image(
              bitmap = exportQrBitmap!!.asImageBitmap(),
              contentDescription = "Offline Export QR Code",
              modifier = Modifier.fillMaxSize()
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Open Keyline Authenticator on your other device, go to \"Transfer codes\" → \"Import accounts\", and point the camera here.",
            fontSize = 13.sp,
            color = SlateTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            try {
              exportQrBitmap?.recycle()
            } catch (_: Throwable) {}
            exportQrBitmap = null
            Toast.makeText(context, "Export complete", Toast.LENGTH_SHORT).show()
            onNavigateBack()
          },
          shape = RoundedCornerShape(20.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = SlateTextPrimary,
            contentColor = CleanWhite
          ),
          modifier = Modifier
            .navigationBarsPadding()
            .testTag("btn_close_export_qr")
        ) {
          Text("Done")
        }
      }
    )
  }
}

@Composable
fun ExportAccountItemCard(
  account: OtpAccount,
  isSelected: Boolean,
  onToggle: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) CleanWhite else CleanWhite.copy(alpha = 0.8f)
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = if (isSelected) KeylineBlue else CleanBorder
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onToggle)
      .testTag("item_export_${account.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Checkbox(
        checked = isSelected,
        onCheckedChange = { onToggle() },
        colors = CheckboxDefaults.colors(
          checkedColor = KeylineBlue,
          checkmarkColor = CleanWhite
        )
      )

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        val title = if (account.issuer.isNotEmpty()) account.issuer else account.accountName
        val subtitle = if (account.issuer.isNotEmpty()) account.accountName else null

        Text(
          text = title,
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold,
          color = SlateTextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        if (subtitle != null && subtitle.isNotEmpty()) {
          Text(
            text = subtitle,
            fontSize = 12.sp,
            color = SlateTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = SlateTextTertiary.copy(alpha = 0.15f)
      ) {
        Text(
          text = if (account.type == OtpType.TOTP) "TOTP" else "HOTP",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = SlateTextSecondary,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
    }
  }
}
