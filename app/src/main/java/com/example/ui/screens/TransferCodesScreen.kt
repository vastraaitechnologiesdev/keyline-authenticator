package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.crypto.BiometricAuthHelper
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.KeylineBlue
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SlateTextTertiary
import com.example.viewmodel.AuthenticatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferCodesScreen(
  viewModel: AuthenticatorViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToExport: () -> Unit,
  onNavigateToImport: () -> Unit
) {
  val context = LocalContext.current
  val accounts by viewModel.rawAccounts.collectAsState()

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
            modifier = Modifier.testTag("btn_transfer_back")
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
            text = "Transfer codes",
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
      Spacer(modifier = Modifier.height(12.dp))

      // 100% Offline direct device-to-device badge banner
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, KeylineBlue.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = CircleShape,
            color = KeylineBlue,
            modifier = Modifier.size(40.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = "Offline Guarantee",
              tint = CleanWhite,
              modifier = Modifier
                .padding(8.dp)
                .size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column {
            Text(
              text = "100% Offline Transfer",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = SlateTextPrimary
            )
            Text(
              text = "Direct device-to-device QR exchange. No servers, no cloud, no internet required.",
              fontSize = 12.sp,
              color = SlateTextSecondary,
              lineHeight = 17.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      Text(
        text = "ACTIONS",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = SlateTextTertiary,
        letterSpacing = 1.sp
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Option 1: Export Accounts
      TransferActionCard(
        title = "Export accounts",
        description = "Generate an offline QR code to transfer selected 2FA accounts to another device.",
        icon = Icons.Default.FileUpload,
        badgeText = "${accounts.size} in vault",
        onClick = {
          if (accounts.isEmpty()) {
            Toast.makeText(context, "No accounts in vault to export", Toast.LENGTH_SHORT).show()
          } else {
            if (context is FragmentActivity) {
              BiometricAuthHelper.authenticate(
                activity = context,
                title = "Unlock Export Vault",
                subtitle = "Verify biometric or device PIN to export 2FA accounts",
                onSuccess = onNavigateToExport,
                onError = { err ->
                  Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
              )
            } else {
              onNavigateToExport()
            }
          }
        },
        testTag = "card_export_accounts"
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Option 2: Import Accounts
      TransferActionCard(
        title = "Import accounts",
        description = "Use your camera to scan an export QR code from another device and import codes directly.",
        icon = Icons.Default.FileDownload,
        badgeText = "Camera Scanner",
        onClick = onNavigateToImport,
        testTag = "card_import_accounts"
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Informative Security Notice
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CleanWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, CleanBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = SlateTextSecondary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Hardware Encryption",
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = SlateTextPrimary
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Exporting requires biometric or screen lock authentication to ensure only authorized owners can view secret keys.",
            fontSize = 12.sp,
            color = SlateTextSecondary,
            lineHeight = 18.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
fun TransferActionCard(
  title: String,
  description: String,
  icon: ImageVector,
  badgeText: String,
  onClick: () -> Unit,
  testTag: String
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CleanWhite),
    border = androidx.compose.foundation.BorderStroke(1.dp, CleanBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        modifier = Modifier.size(48.dp)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = KeylineBlue,
          modifier = Modifier
            .padding(12.dp)
            .size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = SlateTextPrimary
          )

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = SlateTextTertiary.copy(alpha = 0.15f)
          ) {
            Text(
              text = badgeText,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = SlateTextSecondary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = description,
          fontSize = 13.sp,
          color = SlateTextSecondary,
          lineHeight = 18.sp
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = SlateTextTertiary,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
