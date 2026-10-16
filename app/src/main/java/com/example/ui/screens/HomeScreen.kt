package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OtpType
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.ExpiringRed
import com.example.ui.theme.KeylineBlue
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SlateTextTertiary
import com.example.viewmodel.AccountUiItem
import com.example.viewmodel.AuthenticatorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: AuthenticatorViewModel,
  onOpenAddAccountSheet: () -> Unit,
  onNavigateToTransfer: () -> Unit,
  onNavigateToHowItWorks: () -> Unit,
  onNavigateToSettings: () -> Unit
) {
  val context = LocalContext.current
  val accounts by viewModel.uiAccounts.collectAsState()
  val isTapToCopyEnabled by viewModel.isTapToCopyEnabled.collectAsState()
  val securityCheck by viewModel.securityCheckResult.collectAsState()
  val isRootWarningDismissed by viewModel.isRootWarningDismissed.collectAsState()
  val pendingDeleteState by viewModel.pendingDeleteState.collectAsState()
  var accountToDelete by remember { mutableStateOf<AccountUiItem?>(null) }

  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val coroutineScope = rememberCoroutineScope()

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      KeylineDrawerContent(
        onTransferClick = {
          coroutineScope.launch {
            drawerState.close()
            onNavigateToTransfer()
          }
        },
        onHowItWorksClick = {
          coroutineScope.launch {
            drawerState.close()
            onNavigateToHowItWorks()
          }
        },
        onSettingsClick = {
          coroutineScope.launch {
            drawerState.close()
            onNavigateToSettings()
          }
        }
      )
    }
  ) {
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
              onClick = {
                coroutineScope.launch {
                  drawerState.open()
                }
              },
              modifier = Modifier.testTag("btn_menu")
            ) {
              Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open Navigation Menu",
                tint = SlateTextPrimary
              )
            }
          },
          title = {
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(34.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Security,
                  contentDescription = "Keyline Security Shield",
                  tint = KeylineBlue,
                  modifier = Modifier
                    .padding(6.dp)
                    .size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Keyline Authenticator",
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = SlateTextPrimary,
                  letterSpacing = (-0.3).sp
                )
                Text(
                  text = "Offline 2-Factor Vault",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = SlateTextSecondary
                )
              }
            }
          }
        )
      },
      snackbarHost = {
        AnimatedVisibility(
          visible = pendingDeleteState != null,
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          pendingDeleteState?.let { state ->
            UndoDeleteBanner(
              state = state,
              onUndo = { viewModel.undoDelete() }
            )
          }
        }
      },
      floatingActionButton = {
        FloatingActionButton(
          onClick = onOpenAddAccountSheet,
          containerColor = SlateTextPrimary,
          contentColor = CleanWhite,
          shape = CircleShape,
          modifier = Modifier
            .size(56.dp)
            .testTag("fab_add_account")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Account",
            modifier = Modifier.size(26.dp)
          )
        }
      }
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .background(CleanWhite)
      ) {
        if (securityCheck.isCompromised && !isRootWarningDismissed) {
          RootWarningBanner(
            reasons = securityCheck.reasons,
            onDismiss = { viewModel.dismissRootWarning() }
          )
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          if (accounts.isEmpty()) {
            EmptyVaultState(modifier = Modifier.fillMaxSize())
          } else {
            AccountListContent(
              accounts = accounts,
              isTapToCopyEnabled = isTapToCopyEnabled,
              onCopy = { item -> viewModel.copyToClipboard(context, item) },
              onDelete = { item -> accountToDelete = item },
              onRefreshHotp = { item -> viewModel.incrementHotp(item.id) },
              onToggleVisibility = { item -> viewModel.toggleCodeVisibility(item.id) },
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }

  // Delete Confirmation Dialog
  if (accountToDelete != null) {
    val target = accountToDelete!!
    AlertDialog(
      onDismissRequest = { accountToDelete = null },
      containerColor = CleanWhite,
      shape = RoundedCornerShape(20.dp),
      title = {
        Text(
          text = "Remove Account?",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = SlateTextPrimary
        )
      },
      text = {
        Text(
          text = "Are you sure you want to remove ${if (target.issuer.isNotEmpty()) target.issuer else target.accountName}? You will no longer be able to generate 2FA codes for this account unless you have saved your backup key.",
          fontSize = 14.sp,
          color = SlateTextSecondary,
          lineHeight = 20.sp
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteAccountWithUndo(target.id)
            accountToDelete = null
          },
          modifier = Modifier.testTag("btn_confirm_delete")
        ) {
          Text(
            text = "Remove",
            color = ExpiringRed,
            fontWeight = FontWeight.Bold
          )
        }
      },
      dismissButton = {
        TextButton(
          onClick = { accountToDelete = null },
          modifier = Modifier.testTag("btn_cancel_delete")
        ) {
          Text(
            text = "Cancel",
            color = SlateTextPrimary
          )
        }
      }
    )
  }
}

@Composable
fun KeylineDrawerContent(
  onTransferClick: () -> Unit,
  onHowItWorksClick: () -> Unit,
  onSettingsClick: () -> Unit
) {
  ModalDrawerSheet(
    drawerContainerColor = CleanWhite,
    drawerContentColor = SlateTextPrimary,
    modifier = Modifier.width(310.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
      // Modern Drawer Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(46.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = KeylineBlue,
            modifier = Modifier
              .padding(10.dp)
              .size(26.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
          Text(
            text = "Keyline Authenticator",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary,
            letterSpacing = (-0.3).sp
          )
          Text(
            text = "Hardware-backed offline vault",
            fontSize = 11.sp,
            color = SlateTextSecondary
          )
        }
      }

      // 100% Offline Badge
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = KeylineBlue.copy(alpha = 0.1f),
        modifier = Modifier
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(KeylineBlue)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "100% OFFLINE & ENCRYPTED",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = KeylineBlue,
            letterSpacing = 0.8.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = CleanBorder, thickness = 1.dp)
      Spacer(modifier = Modifier.height(14.dp))

      // Drawer Item 1: Transfer codes
      DrawerMenuItem(
        title = "Transfer codes",
        subtitle = "Offline export & import via QR",
        icon = Icons.Default.SwapHoriz,
        onClick = onTransferClick,
        testTag = "menu_transfer_codes"
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Drawer Item 2: How it works?
      DrawerMenuItem(
        title = "How it works?",
        subtitle = "Interactive 2FA walkthrough",
        icon = Icons.Default.HelpOutline,
        onClick = onHowItWorksClick,
        testTag = "menu_how_it_works"
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Drawer Item 3: Settings
      DrawerMenuItem(
        title = "Settings",
        subtitle = "Security, App lock & Clock sync",
        icon = Icons.Default.Settings,
        onClick = onSettingsClick,
        testTag = "menu_settings"
      )

      Spacer(modifier = Modifier.weight(1f))

      HorizontalDivider(color = CleanBorder, thickness = 1.dp)
      Spacer(modifier = Modifier.height(12.dp))

      // Footer branding
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Keyline Authenticator",
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = SlateTextSecondary
        )
        Text(
          text = "v1.0.0 • VastraAI",
          fontSize = 11.sp,
          color = SlateTextTertiary
        )
      }
    }
  }
}

@Composable
fun DrawerMenuItem(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit,
  testTag: String
) {
  NavigationDrawerItem(
    label = {
      Column {
        Text(
          text = title,
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold,
          color = SlateTextPrimary
        )
        Text(
          text = subtitle,
          fontSize = 11.sp,
          color = SlateTextSecondary
        )
      }
    },
    icon = {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = KeylineBlue,
          modifier = Modifier
            .padding(8.dp)
            .size(20.dp)
        )
      }
    },
    selected = false,
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    colors = NavigationDrawerItemDefaults.colors(
      unselectedContainerColor = CleanWhite
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag)
  )
}

@Composable
fun AccountListContent(
  accounts: List<AccountUiItem>,
  isTapToCopyEnabled: Boolean = true,
  onCopy: (AccountUiItem) -> Unit,
  onDelete: (AccountUiItem) -> Unit,
  onRefreshHotp: (AccountUiItem) -> Unit,
  onToggleVisibility: (AccountUiItem) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier.testTag("account_list"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(
      items = accounts,
      key = { it.id }
    ) { item ->
      AccountCard(
        item = item,
        isTapToCopyEnabled = isTapToCopyEnabled,
        onCopy = { onCopy(item) },
        onDelete = { onDelete(item) },
        onRefreshHotp = { onRefreshHotp(item) },
        onToggleVisibility = { onToggleVisibility(item) }
      )
    }
  }
}

@Composable
fun EmptyVaultState(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(96.dp)
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxSize()
      ) {}

      Icon(
        imageVector = Icons.Default.Security,
        contentDescription = null,
        tint = KeylineBlue,
        modifier = Modifier.size(44.dp)
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "No accounts added yet",
      fontSize = 20.sp,
      fontWeight = FontWeight.SemiBold,
      color = SlateTextPrimary,
      letterSpacing = (-0.2).sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Tap the + button below to scan a QR code\nor enter your secret setup key.",
      fontSize = 14.sp,
      color = SlateTextSecondary,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      lineHeight = 20.sp
    )
  }
}

@Composable
fun AccountCard(
  item: AccountUiItem,
  isTapToCopyEnabled: Boolean = true,
  onCopy: () -> Unit,
  onDelete: () -> Unit,
  onRefreshHotp: () -> Unit,
  onToggleVisibility: () -> Unit
) {
  val timerColor by animateColorAsState(
    targetValue = if (item.isExpiring) ExpiringRed else KeylineBlue,
    label = "timerColor"
  )

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("account_item_${item.id}")
      .clickable {
        if (item.isCodeHidden) {
          onToggleVisibility()
        } else if (isTapToCopyEnabled) {
          onCopy()
        }
      },
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CleanWhite),
    border = BorderStroke(1.dp, CleanBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      // Top header: Issuer / Account Name + Type badge + Delete button
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          if (item.issuer.isNotBlank()) {
            Text(
              text = item.issuer.uppercase(),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp,
              color = KeylineBlue,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Text(
            text = item.accountName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = SlateTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Type Badge (TOTP or HOTP)
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.padding(end = 4.dp)
        ) {
          Text(
            text = item.type.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextSecondary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          )
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier
            .size(32.dp)
            .testTag("btn_delete_${item.id}")
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete account",
            tint = SlateTextTertiary,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Middle Row: Formatted 6-digit OTP ('123 456' or '••• •••' if hidden) + Visibility Toggle + Copy button
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        val displayedCode = if (item.isCodeHidden) "••• •••" else item.formattedCode

        Text(
          text = displayedCode,
          fontSize = 32.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 2.sp,
          color = if (item.type == OtpType.TOTP && item.isExpiring && !item.isCodeHidden) ExpiringRed else SlateTextPrimary,
          modifier = Modifier
            .testTag("code_${item.id}")
            .clickable { onToggleVisibility() }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Eye visibility button
          IconButton(
            onClick = onToggleVisibility,
            modifier = Modifier
              .size(36.dp)
              .testTag("btn_visibility_${item.id}")
          ) {
            Icon(
              imageVector = if (item.isCodeHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = if (item.isCodeHidden) "Reveal code" else "Hide code",
              tint = SlateTextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }

          if (item.type == OtpType.HOTP) {
            IconButton(
              onClick = onRefreshHotp,
              modifier = Modifier
                .size(36.dp)
                .testTag("btn_refresh_hotp_${item.id}")
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Next counter code",
                tint = KeylineBlue,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          IconButton(
            onClick = onCopy,
            modifier = Modifier
              .size(36.dp)
              .testTag("btn_copy_${item.id}")
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Copy code to clipboard",
              tint = SlateTextPrimary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Bottom Row: Timer / Countdown indicator
      if (item.type == OtpType.TOTP) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.size(20.dp)
            ) {
              CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = CleanBorder,
                strokeWidth = 2.5.dp
              )
              CircularProgressIndicator(
                progress = { item.progress },
                modifier = Modifier.fillMaxSize(),
                color = timerColor,
                strokeWidth = 2.5.dp,
                trackColor = Color.Transparent
              )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
              text = "${item.remainingSeconds}s remaining",
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = timerColor
            )
          }

          Text(
            text = if (item.isCodeHidden) "Tap to reveal" else "Tap to copy",
            fontSize = 12.sp,
            color = SlateTextTertiary
          )
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Counter: ${item.counter}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = SlateTextSecondary
          )
          Text(
            text = if (item.isCodeHidden) "Tap to reveal" else "Tap to copy",
            fontSize = 12.sp,
            color = SlateTextTertiary
          )
        }
      }
    }
  }
}

@Composable
fun RootWarningBanner(
  reasons: List<String>,
  onDismiss: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = Color(0xFFFFFBEB)
    ),
    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp)
      .testTag("banner_root_warning")
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.Top
    ) {
      Surface(
        shape = CircleShape,
        color = Color(0xFFFEF3C7),
        modifier = Modifier.size(34.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = "Security Alert",
          tint = Color(0xFFD97706),
          modifier = Modifier.padding(7.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Compromised Environment Detected",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF92400E)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "Keyline detected root/su binaries or unverified system keys (${reasons.joinToString(", ")}). On rooted devices, Android Keystore and ephemeral memory boundaries are exposed to superuser inspection.",
          fontSize = 12.sp,
          color = Color(0xFFB45309),
          lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("btn_dismiss_root_warning")
          ) {
            Text(
              text = "Acknowledge Risks",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF92400E)
            )
          }
        }
      }
    }
  }
}

@Composable
fun UndoDeleteBanner(
  state: AuthenticatorViewModel.UndoDeleteState,
  onUndo: () -> Unit,
  modifier: Modifier = Modifier
) {
  val countdownProgress = remember(state.account.id, state.timestamp) {
    Animatable(1f)
  }

  LaunchedEffect(state.account.id, state.timestamp) {
    countdownProgress.snapTo(1f)
    countdownProgress.animateTo(
      targetValue = 0f,
      animationSpec = tween(
        durationMillis = state.durationMillis.toInt(),
        easing = LinearEasing
      )
    )
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = SlateTextPrimary
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    border = BorderStroke(1.dp, Color(0xFF334155)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("undo_delete_snackbar")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f, fill = false)
      ) {
        // Animated 3-second Circular Countdown Timer (20dp)
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(20.dp)
            .testTag("timer_undo_countdown")
        ) {
          CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF334155),
            strokeWidth = 2.5.dp
          )
          CircularProgressIndicator(
            progress = { countdownProgress.value },
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF97316),
            trackColor = Color.Transparent,
            strokeWidth = 2.5.dp
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Label: "'[Account Name]' deleted"
        Text(
          text = "'${state.displayName}' deleted",
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = CleanWhite,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // "UNDO" Text Button: Bold, styled in primary orange / slate blue
      TextButton(
        onClick = onUndo,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.testTag("btn_undo_delete")
      ) {
        Text(
          text = "UNDO",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFFB923C),
          letterSpacing = 0.5.sp
        )
      }
    }
  }
}
