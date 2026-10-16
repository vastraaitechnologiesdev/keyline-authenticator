package com.example.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.crypto.BiometricAuthHelper
import com.example.ui.screens.AddAccountBottomSheet
import com.example.ui.screens.EnterKeyScreen
import com.example.ui.screens.ExportAccountsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HowItWorksScreen
import com.example.ui.screens.ImportScannerScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransferCodesScreen
import com.example.viewmodel.AuthenticatorViewModel

object Routes {
  const val HOME = "home"
  const val ENTER_KEY = "enter_key"
  const val QR_SCANNER = "qr_scanner"
  const val TRANSFER_CODES = "transfer_codes"
  const val EXPORT_ACCOUNTS = "export_accounts"
  const val IMPORT_SCANNER = "import_scanner"
  const val HOW_IT_WORKS = "how_it_works"
  const val SETTINGS = "settings"
}

@Composable
fun KeylineNavHost(
  navController: NavHostController = rememberNavController(),
  viewModel: AuthenticatorViewModel = viewModel()
) {
  val context = LocalContext.current
  var showAddBottomSheet by remember { mutableStateOf(false) }

  fun authenticateForAddAccount(onSuccess: () -> Unit) {
    if (context is FragmentActivity) {
      BiometricAuthHelper.authenticate(
        activity = context,
        title = "Add Account",
        subtitle = "Verify biometric or device PIN to add a new account",
        onSuccess = onSuccess,
        onError = { err ->
          Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
        }
      )
    } else {
      onSuccess()
    }
  }

  NavHost(
    navController = navController,
    startDestination = Routes.HOME
  ) {
    composable(Routes.HOME) {
      HomeScreen(
        viewModel = viewModel,
        onOpenAddAccountSheet = {
          authenticateForAddAccount {
            showAddBottomSheet = true
          }
        },
        onNavigateToTransfer = { navController.navigate(Routes.TRANSFER_CODES) },
        onNavigateToHowItWorks = { navController.navigate(Routes.HOW_IT_WORKS) },
        onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
      )
    }

    composable(Routes.ENTER_KEY) {
      EnterKeyScreen(
        viewModel = viewModel,
        onNavigateBack = { navController.popBackStack() }
      )
    }

    composable(Routes.QR_SCANNER) {
      QrScannerScreen(
        viewModel = viewModel,
        onNavigateBack = { navController.popBackStack() },
        onEnterKeyManually = {
          navController.popBackStack()
          navController.navigate(Routes.ENTER_KEY)
        }
      )
    }

    composable(Routes.TRANSFER_CODES) {
      TransferCodesScreen(
        viewModel = viewModel,
        onNavigateBack = { navController.popBackStack() },
        onNavigateToExport = { navController.navigate(Routes.EXPORT_ACCOUNTS) },
        onNavigateToImport = { navController.navigate(Routes.IMPORT_SCANNER) }
      )
    }

    composable(Routes.EXPORT_ACCOUNTS) {
      ExportAccountsScreen(
        viewModel = viewModel,
        onNavigateBack = { navController.popBackStack() }
      )
    }

    composable(Routes.IMPORT_SCANNER) {
      ImportScannerScreen(
        viewModel = viewModel,
        onNavigateBack = { navController.popBackStack() },
        onImportSuccess = {
          navController.popBackStack(Routes.HOME, inclusive = false)
        }
      )
    }

    composable(Routes.HOW_IT_WORKS) {
      HowItWorksScreen(
        onNavigateBack = { navController.popBackStack() }
      )
    }

    composable(Routes.SETTINGS) {
      SettingsScreen(
        viewModel = viewModel,
        onNavigateBack = { navController.popBackStack() }
      )
    }
  }

  if (showAddBottomSheet) {
    AddAccountBottomSheet(
      onDismiss = { showAddBottomSheet = false },
      onScanQrClick = {
        showAddBottomSheet = false
        navController.navigate(Routes.QR_SCANNER)
      },
      onEnterKeyClick = {
        showAddBottomSheet = false
        navController.navigate(Routes.ENTER_KEY)
      }
    )
  }
}
