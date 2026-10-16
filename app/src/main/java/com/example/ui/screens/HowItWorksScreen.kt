package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.KeylineBlue
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SlateTextTertiary
import kotlinx.coroutines.launch

data class WalkthroughStep(
  val stepNumber: Int,
  val category: String,
  val title: String,
  val description: String,
  val icon: ImageVector,
  val secondaryText: String
)

val WALKTHROUGH_STEPS = listOf(
  WalkthroughStep(
    stepNumber = 1,
    category = "STEP 1 OF 4",
    title = "What is 2FA?",
    description = "Passwords alone are vulnerable to leaks and phishing. Two-Factor Authentication (2FA) adds a vital security checkpoint by requiring a dynamic code that only your phone can generate.",
    icon = Icons.Default.Security,
    secondaryText = "Hardware-bound secondary proof of identity"
  ),
  WalkthroughStep(
    stepNumber = 2,
    category = "STEP 2 OF 4",
    title = "Adding Accounts",
    description = "When setting up 2FA on Google, GitHub, Amazon, or other platforms, scan the provided QR code or paste the secret base32 key to securely pair your account.",
    icon = Icons.Default.QrCodeScanner,
    secondaryText = "Compatible with all standard RFC 6238 services"
  ),
  WalkthroughStep(
    stepNumber = 3,
    category = "STEP 3 OF 4",
    title = "30-Second Dynamic Codes",
    description = "Every 30 seconds, an offline cryptographic clock hashes your shared secret into a fresh 6-digit code. The circular countdown ring shows how much time remains before the next code.",
    icon = Icons.Default.AccessTime,
    secondaryText = "Cryptographically synchronized offline clock"
  ),
  WalkthroughStep(
    stepNumber = 4,
    category = "STEP 4 OF 4",
    title = "100% Offline & Private",
    description = "Your secret keys are stored in encrypted hardware-backed storage on your device. Keyline never connects to servers, tracks analytics, or uploads data. You are always in control.",
    icon = Icons.Default.Lock,
    secondaryText = "Zero cloud dependencies • True privacy"
  )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowItWorksScreen(
  onNavigateBack: () -> Unit
) {
  val pagerState = rememberPagerState(pageCount = { WALKTHROUGH_STEPS.size })
  val coroutineScope = rememberCoroutineScope()

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
            modifier = Modifier.testTag("btn_how_it_works_back")
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
            text = "How it works?",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = SlateTextPrimary
          )
        },
        actions = {
          // Skip button (visible on steps 1-3)
          if (pagerState.currentPage < WALKTHROUGH_STEPS.size - 1) {
            TextButton(
              onClick = onNavigateBack,
              modifier = Modifier.testTag("btn_skip_walkthrough")
            ) {
              Text(
                text = "Skip",
                color = SlateTextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      )
    },
    bottomBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(CleanWhite)
          .navigationBarsPadding()
          .padding(horizontal = 24.dp, vertical = 16.dp)
      ) {
        // Animated Dot Indicators
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          WALKTHROUGH_STEPS.indices.forEach { index ->
            val isSelected = pagerState.currentPage == index
            val dotWidth by animateDpAsState(
              targetValue = if (isSelected) 24.dp else 8.dp,
              animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
              label = "dotWidth"
            )
            val dotColor = if (isSelected) KeylineBlue else SlateTextTertiary.copy(alpha = 0.3f)

            Box(
              modifier = Modifier
                .padding(horizontal = 4.dp)
                .height(8.dp)
                .width(dotWidth)
                .clip(CircleShape)
                .background(dotColor)
            )
          }
        }

        // Action Button: "Next" or "Get Started"
        val isLastStep = pagerState.currentPage == WALKTHROUGH_STEPS.size - 1

        Button(
          onClick = {
            if (isLastStep) {
              onNavigateBack()
            } else {
              coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
              }
            }
          },
          shape = RoundedCornerShape(27.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = SlateTextPrimary,
            contentColor = CleanWhite
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag(if (isLastStep) "btn_get_started" else "btn_walkthrough_next")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text(
              text = if (isLastStep) "Get Started" else "Next Step",
              fontSize = 16.sp,
              fontWeight = FontWeight.SemiBold
            )
            if (!isLastStep) {
              Spacer(modifier = Modifier.width(8.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }
  ) { innerPadding ->
    HorizontalPager(
      state = pagerState,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) { page ->
      val step = WALKTHROUGH_STEPS[page]
      WalkthroughStepPage(step = step)
    }
  }
}

@Composable
fun WalkthroughStepPage(step: WalkthroughStep) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 28.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // Custom Step Illustration Card
    StepIllustration(step = step)

    Spacer(modifier = Modifier.height(36.dp))

    // Category badge
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
      modifier = Modifier.padding(bottom = 12.dp)
    ) {
      Text(
        text = step.category,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = KeylineBlue,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
      )
    }

    // Step Title
    Text(
      text = step.title,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      color = SlateTextPrimary,
      textAlign = TextAlign.Center,
      letterSpacing = (-0.5).sp
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Step Description
    Text(
      text = step.description,
      fontSize = 15.sp,
      color = SlateTextSecondary,
      textAlign = TextAlign.Center,
      lineHeight = 22.sp
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Secondary pill badge
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = CleanWhite,
      border = androidx.compose.foundation.BorderStroke(1.dp, CleanBorder)
    ) {
      Text(
        text = step.secondaryText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = SlateTextTertiary,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
      )
    }
  }
}

@Composable
fun StepIllustration(step: WalkthroughStep) {
  Box(
    modifier = Modifier
      .size(180.dp)
      .clip(RoundedCornerShape(36.dp))
      .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
      .border(1.dp, CleanBorder, RoundedCornerShape(36.dp)),
    contentAlignment = Alignment.Center
  ) {
    // Background decorative subtle circles
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        color = KeylineBlue.copy(alpha = 0.08f),
        radius = size.minDimension * 0.42f,
        center = center
      )
      drawCircle(
        color = KeylineBlue.copy(alpha = 0.12f),
        radius = size.minDimension * 0.30f,
        center = center
      )
    }

    Surface(
      shape = CircleShape,
      color = CleanWhite,
      shadowElevation = 4.dp,
      modifier = Modifier.size(90.dp)
    ) {
      Icon(
        imageVector = step.icon,
        contentDescription = step.title,
        tint = KeylineBlue,
        modifier = Modifier
          .padding(22.dp)
          .size(46.dp)
      )
    }
  }
}
