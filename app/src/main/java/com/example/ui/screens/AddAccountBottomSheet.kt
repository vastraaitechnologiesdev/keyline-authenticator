package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.KeylineBlue
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountBottomSheet(
  onDismiss: () -> Unit,
  onScanQrClick: () -> Unit,
  onEnterKeyClick: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    dragHandle = {
      Surface(
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
        shape = RoundedCornerShape(2.dp),
        color = CleanBorder
      ) {
        Spacer(modifier = Modifier.size(width = 36.dp, height = 4.dp))
      }
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 12.dp)
        .padding(bottom = 16.dp)
    ) {
      Text(
        text = "Add account",
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = SlateTextPrimary,
        modifier = Modifier.padding(bottom = 16.dp)
      )

      // Option 1: Scan a QR code
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            onDismiss()
            onScanQrClick()
          }
          .padding(vertical = 16.dp)
          .testTag("option_scan_qr"),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.size(48.dp)
        ) {
          Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = "Scan a QR code",
            tint = KeylineBlue,
            modifier = Modifier
              .padding(12.dp)
              .size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
          Text(
            text = "Scan a QR code",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = SlateTextPrimary
          )
          Text(
            text = "Use your device camera to scan setup code",
            fontSize = 13.sp,
            color = SlateTextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Option 2: Enter a setup key
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            onDismiss()
            onEnterKeyClick()
          }
          .padding(vertical = 16.dp)
          .testTag("option_enter_key"),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.size(48.dp)
        ) {
          Icon(
            imageVector = Icons.Default.VpnKey,
            contentDescription = "Enter a setup key",
            tint = KeylineBlue,
            modifier = Modifier
              .padding(12.dp)
              .size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
          Text(
            text = "Enter a setup key",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = SlateTextPrimary
          )
          Text(
            text = "Type or paste your Base32 secret key manually",
            fontSize = 13.sp,
            color = SlateTextSecondary
          )
        }
      }
    }
  }
}
