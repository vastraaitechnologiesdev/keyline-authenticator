package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.KeylineBlue
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.viewmodel.AuthenticatorViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
  viewModel: AuthenticatorViewModel,
  onNavigateBack: () -> Unit,
  onEnterKeyManually: () -> Unit
) {
  val context = LocalContext.current
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
      ) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { isGranted ->
      hasCameraPermission = isGranted
      if (!isGranted) {
        Toast.makeText(
          context,
          "Camera permission is required to scan QR codes",
          Toast.LENGTH_LONG
        ).show()
      }
    }
  )

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  Scaffold(
    containerColor = Color.Black,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Black.copy(alpha = 0.6f),
          titleContentColor = CleanWhite
        ),
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("btn_scanner_back")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = CleanWhite
            )
          }
        },
        title = {
          Text(
            text = "Scan QR Code",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = CleanWhite
          )
        }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (hasCameraPermission) {
        CameraPreviewWithScanner(
          onQrFound = { rawContent ->
            val result = viewModel.addAccountFromQr(rawContent)
            if (result.isSuccess) {
              val account = result.getOrThrow()
              val label = if (account.issuer.isNotEmpty()) account.issuer else account.accountName
              Toast.makeText(context, "Account \"$label\" added", Toast.LENGTH_SHORT).show()
              onNavigateBack()
            } else {
              val errorMsg = result.exceptionOrNull()?.message ?: "Invalid QR code"
              Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
          }
        )

        // Overlay with scanning frame & guidance
        ScannerOverlay(
          modifier = Modifier.fillMaxSize(),
          onManualClick = onEnterKeyManually
        )
      } else {
        // Permission Request Fallback View
        CameraPermissionFallback(
          onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
          onManualClick = onEnterKeyManually,
          modifier = Modifier.align(Alignment.Center)
        )
      }
    }
  }
}

@Composable
fun CameraPermissionFallback(
  onRequestPermission: () -> Unit,
  onManualClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .padding(32.dp)
      .fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Surface(
      shape = CircleShape,
      color = CleanWhite.copy(alpha = 0.1f),
      modifier = Modifier.size(80.dp)
    ) {
      Icon(
        imageVector = Icons.Default.CameraAlt,
        contentDescription = null,
        tint = CleanWhite,
        modifier = Modifier
          .padding(20.dp)
          .size(40.dp)
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "Camera Access Required",
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = CleanWhite
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "To scan 2FA setup QR codes, Keyline Authenticator needs permission to use your camera.",
      fontSize = 14.sp,
      color = SlateTextSecondary,
      textAlign = TextAlign.Center,
      lineHeight = 20.sp
    )

    Spacer(modifier = Modifier.height(28.dp))

    Button(
      onClick = onRequestPermission,
      shape = RoundedCornerShape(24.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = KeylineBlue,
        contentColor = CleanWhite
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("btn_grant_camera")
    ) {
      Text(
        text = "Grant Camera Permission",
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
      onClick = onManualClick,
      shape = RoundedCornerShape(24.dp),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanWhite),
      border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhite.copy(alpha = 0.4f)),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("btn_manual_fallback")
    ) {
      Text(
        text = "Enter setup key manually",
        fontSize = 14.sp
      )
    }
  }
}

@Composable
fun CameraPreviewWithScanner(
  onQrFound: (String) -> Unit
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
  var isDetected by remember { mutableStateOf(false) }

  val barcodeScanner = remember {
    try {
      com.google.mlkit.common.sdkinternal.MlKitContext.initializeIfNeeded(context.applicationContext)
    } catch (_: Throwable) {}
    val options = BarcodeScannerOptions.Builder()
      .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
      .build()
    BarcodeScanning.getClient(options)
  }

  DisposableEffect(Unit) {
    onDispose {
      cameraExecutor.shutdown()
      barcodeScanner.close()
    }
  }

  AndroidView(
    factory = { ctx ->
      val previewView = PreviewView(ctx).apply {
        scaleType = PreviewView.ScaleType.FILL_CENTER
      }

      val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
      cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
          it.surfaceProvider = previewView.surfaceProvider
        }

        val imageAnalysis = ImageAnalysis.Builder()
          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
          .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
          val mediaImage = imageProxy.image
          if (mediaImage != null && !isDetected) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(inputImage)
              .addOnSuccessListener { barcodes ->
                if (!isDetected) {
                  for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank() && rawValue.startsWith("otpauth://", ignoreCase = true)) {
                      isDetected = true
                      onQrFound(rawValue)
                      break
                    }
                  }
                }
              }
              .addOnCompleteListener {
                imageProxy.close()
              }
          } else {
            imageProxy.close()
          }
        }

        try {
          cameraProvider.unbindAll()
          cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis
          )
        } catch (_: Exception) {
          // Fallback if binding fails
        }
      }, ContextCompat.getMainExecutor(ctx))

      previewView
    },
    modifier = Modifier.fillMaxSize()
  )
}

@Composable
fun ScannerOverlay(
  modifier: Modifier = Modifier,
  onManualClick: () -> Unit
) {
  Box(modifier = modifier) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val boxSize = size.minDimension * 0.7f
      val left = (size.width - boxSize) / 2f
      val top = (size.height - boxSize) / 2.4f
      val rect = Rect(left, top, left + boxSize, top + boxSize)

      val path = Path().apply {
        addRoundRect(RoundRect(rect, CornerRadius(24.dp.toPx(), 24.dp.toPx())))
      }

      // Darken outside viewfinder cutout
      clipPath(path, clipOp = ClipOp.Difference) {
        drawRect(color = Color(0x99000000))
      }

      // Draw rounded boundary line
      drawRoundRect(
        color = KeylineBlue.copy(alpha = 0.8f),
        topLeft = Offset(left, top),
        size = Size(boxSize, boxSize),
        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
      )

      // Draw 4 corner highlight brackets
      val cornerLen = 28.dp.toPx()
      val strokeW = 4.dp.toPx()
      val cornerColor = KeylineBlue

      // Top-Left
      drawLine(cornerColor, Offset(left, top + cornerLen), Offset(left, top), strokeW, StrokeCap.Round)
      drawLine(cornerColor, Offset(left, top), Offset(left + cornerLen, top), strokeW, StrokeCap.Round)

      // Top-Right
      drawLine(cornerColor, Offset(left + boxSize - cornerLen, top), Offset(left + boxSize, top), strokeW, StrokeCap.Round)
      drawLine(cornerColor, Offset(left + boxSize, top), Offset(left + boxSize, top + cornerLen), strokeW, StrokeCap.Round)

      // Bottom-Left
      drawLine(cornerColor, Offset(left, top + boxSize - cornerLen), Offset(left, top + boxSize), strokeW, StrokeCap.Round)
      drawLine(cornerColor, Offset(left, top + boxSize), Offset(left + cornerLen, top + boxSize), strokeW, StrokeCap.Round)

      // Bottom-Right
      drawLine(cornerColor, Offset(left + boxSize - cornerLen, top + boxSize), Offset(left + boxSize, top + boxSize), strokeW, StrokeCap.Round)
      drawLine(cornerColor, Offset(left + boxSize, top + boxSize - cornerLen), Offset(left + boxSize, top + boxSize), strokeW, StrokeCap.Round)
    }

    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(horizontal = 24.dp, vertical = 40.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Point camera at a 2FA QR code",
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = CleanWhite,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(20.dp))

      OutlinedButton(
        onClick = onManualClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, CleanWhite.copy(alpha = 0.5f)),
        modifier = Modifier.testTag("btn_scanner_enter_manually")
      ) {
        Text(text = "Or enter key manually", fontSize = 13.sp)
      }
    }
  }
}
