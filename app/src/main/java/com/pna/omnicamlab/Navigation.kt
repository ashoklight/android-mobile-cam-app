package com.pna.omnicamlab

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.pna.omnicamlab.ui.screens.*

@Composable
fun MainNavigation() {
  // Start at Onboarding to enforce permissions check first
  val backStack = rememberNavBackStack(Onboarding)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        // 1. Onboarding Permission Screen
        entry<Onboarding> {
          OnboardingScreen(
            onPermissionsGranted = {
              // Core permission granted, navigate to Home and clear onboarding from history
              backStack.removeLastOrNull()
              backStack.add(Home)
            }
          )
        }

        // 2. Home Dashboard Screen
        entry<Home> {
          HomeScreen(
            onNavigateToCamera = { backStack.add(CameraCapture) },
            onNavigateToReport = { backStack.add(CapabilityReport) },
            onNavigateToSettings = { backStack.add(Settings) },
            onRedirectToOnboarding = {
              backStack.removeLastOrNull()
              backStack.add(Onboarding)
            }
          )
        }

        // 3. Capability Report Screen
        entry<CapabilityReport> {
          CapabilityReportScreen(
            onNavigateBack = { backStack.removeLastOrNull() }
          )
        }

        // 4. Camera Viewfinder Capture Screen
        entry<CameraCapture> {
          CameraCaptureScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            onNavigateToResult = { resultKey -> backStack.add(resultKey) }
          )
        }

        // 5. Capture Verification / Result Screen
        entry<CaptureResult> { key ->
          CaptureResultScreen(
            result = key,
            onNavigateBack = { backStack.removeLastOrNull() }
          )
        }

        // 6. Preferences / Settings Screen
        entry<Settings> {
          SettingsScreen(
            onNavigateBack = { backStack.removeLastOrNull() }
          )
        }
      },
  )
}
