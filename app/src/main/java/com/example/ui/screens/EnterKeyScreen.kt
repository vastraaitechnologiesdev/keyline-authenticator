package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.Base32
import com.example.model.OtpType
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.ExpiringRed
import com.example.ui.theme.KeylineBlue
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SlateTextTertiary
import com.example.viewmodel.AuthenticatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterKeyScreen(
  viewModel: AuthenticatorViewModel,
  onNavigateBack: () -> Unit
) {
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current

  var codeName by remember { mutableStateOf("") }
  var secretKey by remember { mutableStateOf("") }
  var selectedType by remember { mutableStateOf(OtpType.TOTP) }
  var dropdownExpanded by remember { mutableStateOf(false) }

  val sanitizedKey by remember {
    derivedStateOf { Base32.sanitize(secretKey) }
  }

  val isKeyValid by remember {
    derivedStateOf {
      sanitizedKey.isNotEmpty() && Base32.isValid(sanitizedKey)
    }
  }

  val isFormValid by remember {
    derivedStateOf {
      codeName.trim().isNotEmpty() && isKeyValid
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
            modifier = Modifier.testTag("btn_back")
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
            text = "Enter code details",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = SlateTextPrimary
          )
        }
      )
    },
    bottomBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(CleanWhite)
          .navigationBarsPadding()
          .padding(horizontal = 20.dp, vertical = 16.dp)
      ) {
        Button(
          onClick = {
            focusManager.clearFocus()
            val result = viewModel.addManualAccount(
              name = codeName,
              secretKey = secretKey,
              type = selectedType
            )
            if (result.isSuccess) {
              Toast.makeText(context, "Account \"${codeName.trim()}\" added", Toast.LENGTH_SHORT).show()
              onNavigateBack()
            } else {
              Toast.makeText(
                context,
                result.exceptionOrNull()?.message ?: "Failed to add account",
                Toast.LENGTH_SHORT
              ).show()
            }
          },
          enabled = isFormValid,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("btn_add_account"),
          shape = RoundedCornerShape(27.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = SlateTextPrimary,
            contentColor = CleanWhite,
            disabledContainerColor = SlateTextTertiary.copy(alpha = 0.3f),
            disabledContentColor = CleanWhite.copy(alpha = 0.6f)
          )
        ) {
          Text(
            text = "Add account",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Enter the account name and secret key provided by the service you want to secure.",
        fontSize = 14.sp,
        color = SlateTextSecondary,
        lineHeight = 20.sp
      )

      Spacer(modifier = Modifier.height(24.dp))

      // OutlinedTextField 1: "Code name" (Single line)
      OutlinedTextField(
        value = codeName,
        onValueChange = { codeName = it },
        label = { Text("Code name") },
        placeholder = { Text("e.g., Google, GitHub (work)") },
        singleLine = true,
        maxLines = 1,
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Sentences,
          imeAction = ImeAction.Next
        ),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = KeylineBlue,
          unfocusedBorderColor = CleanBorder,
          focusedLabelColor = KeylineBlue,
          unfocusedLabelColor = SlateTextSecondary,
          focusedTextColor = SlateTextPrimary,
          unfocusedTextColor = SlateTextPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_code_name")
      )

      Spacer(modifier = Modifier.height(20.dp))

      // OutlinedTextField 2: "Your key" (Base32 format; auto-capitalize, trim whitespace)
      val keyHasError = secretKey.isNotEmpty() && !isKeyValid
      OutlinedTextField(
        value = secretKey,
        onValueChange = { input ->
          // Auto-capitalize and strip space/dashes on paste/type
          secretKey = input.uppercase().replace(" ", "")
        },
        label = { Text("Your key") },
        placeholder = { Text("e.g., JBSWY3DPEHPK3PXP") },
        singleLine = true,
        isError = keyHasError,
        supportingText = {
          if (keyHasError) {
            Text(
              text = "Invalid Base32 secret key (only letters A-Z and digits 2-7 are valid)",
              color = ExpiringRed,
              fontSize = 12.sp
            )
          } else {
            Text(
              text = "Secret key in Base32 format (spaces automatically removed)",
              color = SlateTextTertiary,
              fontSize = 12.sp
            )
          }
        },
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Characters,
          keyboardType = KeyboardType.Ascii,
          imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
          onDone = { focusManager.clearFocus() }
        ),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = KeylineBlue,
          unfocusedBorderColor = CleanBorder,
          focusedLabelColor = KeylineBlue,
          unfocusedLabelColor = SlateTextSecondary,
          focusedTextColor = SlateTextPrimary,
          unfocusedTextColor = SlateTextPrimary,
          errorBorderColor = ExpiringRed
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_secret_key")
      )

      Spacer(modifier = Modifier.height(16.dp))

      // OutlinedTextField / Dropdown: "Type of key" (Options: "Time based" [TOTP] and "Counter based" [HOTP])
      ExposedDropdownMenuBox(
        expanded = dropdownExpanded,
        onExpandedChange = { dropdownExpanded = it },
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedTextField(
          value = if (selectedType == OtpType.TOTP) "Time based" else "Counter based",
          onValueChange = {},
          readOnly = true,
          label = { Text("Type of key") },
          trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KeylineBlue,
            unfocusedBorderColor = CleanBorder,
            focusedLabelColor = KeylineBlue,
            unfocusedLabelColor = SlateTextSecondary,
            focusedTextColor = SlateTextPrimary,
            unfocusedTextColor = SlateTextPrimary
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            .testTag("dropdown_key_type")
        )

        ExposedDropdownMenu(
          expanded = dropdownExpanded,
          onDismissRequest = { dropdownExpanded = false },
          containerColor = CleanWhite,
          shape = RoundedCornerShape(12.dp)
        ) {
          DropdownMenuItem(
            text = {
              Column {
                Text(
                  text = "Time based",
                  fontWeight = FontWeight.Medium,
                  color = SlateTextPrimary
                )
                Text(
                  text = "Changes every 30 seconds (Standard TOTP)",
                  fontSize = 12.sp,
                  color = SlateTextSecondary
                )
              }
            },
            onClick = {
              selectedType = OtpType.TOTP
              dropdownExpanded = false
            },
            modifier = Modifier.testTag("option_totp")
          )
          DropdownMenuItem(
            text = {
              Column {
                Text(
                  text = "Counter based",
                  fontWeight = FontWeight.Medium,
                  color = SlateTextPrimary
                )
                Text(
                  text = "Changes on demand with counter (HOTP)",
                  fontSize = 12.sp,
                  color = SlateTextSecondary
                )
              }
            },
            onClick = {
              selectedType = OtpType.HOTP
              dropdownExpanded = false
            },
            modifier = Modifier.testTag("option_hotp")
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
