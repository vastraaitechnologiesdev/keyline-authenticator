package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.KeylineBlue
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.OutlinedButton
import com.example.ui.theme.ExpiringRed
import com.example.ui.theme.ExpiringRedLight

@Composable
fun VaultLockScreen(
  errorMessage: String? = null,
  onOpenSettings: (() -> Unit)? = null,
  onUnlockClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(CleanWhite)
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Image(
      painter = painterResource(id = R.drawable.ic_keyline_logo),
      contentDescription = "Keyline Security Logo",
      modifier = Modifier.size(108.dp)
    )

    Spacer(modifier = Modifier.height(28.dp))

    Text(
      text = "Keyline Authenticator",
      fontSize = 22.sp,
      fontWeight = FontWeight.Bold,
      color = SlateTextPrimary,
      letterSpacing = (-0.4).sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Vault Locked",
      fontSize = 15.sp,
      fontWeight = FontWeight.SemiBold,
      color = KeylineBlue
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "App Lock is enabled. Verify biometric or device credential to access your 2FA accounts.",
      fontSize = 14.sp,
      color = SlateTextSecondary,
      textAlign = TextAlign.Center,
      lineHeight = 20.sp
    )

    if (!errorMessage.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(20.dp))
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = ExpiringRedLight,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = "Security Alert",
              tint = ExpiringRed,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Device Lock Required",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = ExpiringRed
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = errorMessage,
            fontSize = 12.sp,
            color = SlateTextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
          )
          if (onOpenSettings != null) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
              onClick = onOpenSettings,
              shape = RoundedCornerShape(18.dp)
            ) {
              Text(
                text = "Open Security Settings",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(36.dp))

    Button(
      onClick = onUnlockClick,
      shape = RoundedCornerShape(27.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = SlateTextPrimary,
        contentColor = CleanWhite
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("btn_unlock_vault")
    ) {
      Icon(
        imageVector = Icons.Default.Fingerprint,
        contentDescription = null,
        modifier = Modifier.size(22.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = "Unlock Vault",
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}
