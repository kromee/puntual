package com.example.puntual.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.puntual.navigation.PuntualNavHost
import com.example.puntual.navigation.PuntualRoute
import com.example.puntual.ui.auth.AuthViewModel
import com.example.puntual.ui.auth.LoginScreen
import com.example.puntual.ui.components.PuntualBottomBar
import com.example.puntual.ui.theme.PuntualGreen
import com.example.puntual.ui.theme.ScreenBackground
import com.example.puntual.ui.theme.TextPrimary
import com.example.puntual.ui.theme.TextSecondary

@Composable
fun PuntualApp(
    activity: FragmentActivity,
    authViewModel: AuthViewModel = hiltViewModel(),
    appSecurityViewModel: AppSecurityViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val securityState by appSecurityViewModel.uiState.collectAsStateWithLifecycle()
    var isLocallyUnlocked by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf<String?>(null) }
    val biometricManager = remember(activity) { BiometricManager.from(activity) }
    val canUseBiometrics = remember(activity) {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }
    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Puntuall")
            .setSubtitle("Confirma tu identidad para entrar")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }
    val biometricPrompt = remember(activity) {
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isLocallyUnlocked = true
                    biometricError = null
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    biometricError = errString.toString()
                }

                override fun onAuthenticationFailed() {
                    biometricError = "No se pudo validar la huella. Intenta de nuevo."
                }
            },
        )
    }

    if (!authState.isSessionResolved) {
        PuntualLaunchLoadingScreen()
        return
    }

    if (!authState.isAuthenticated) {
        LoginScreen(viewModel = authViewModel)
        return
    }

    LaunchedEffect(authState.isAuthenticated) {
        if (!authState.isAuthenticated) {
            isLocallyUnlocked = false
            biometricError = null
        }
    }

    val shouldRequireBiometricUnlock = authState.isAuthenticated &&
        securityState.biometricUnlockEnabled &&
        canUseBiometrics

    LaunchedEffect(shouldRequireBiometricUnlock, isLocallyUnlocked) {
        if (shouldRequireBiometricUnlock && !isLocallyUnlocked) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    if (shouldRequireBiometricUnlock && !isLocallyUnlocked) {
        BiometricUnlockScreen(
            errorMessage = biometricError,
            onUnlockClick = {
                biometricError = null
                biometricPrompt.authenticate(promptInfo)
            },
            onSignOut = authViewModel::signOut,
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        containerColor = ScreenBackground,
        bottomBar = {
            PuntualBottomBar(
                currentDestination = currentDestination,
                onNavigate = { route ->
                    navController.navigate(route.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        PuntualNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            onSignOut = authViewModel::signOut,
        )
    }
}

@Composable
private fun BiometricUnlockScreen(
    errorMessage: String?,
    onUnlockClick: () -> Unit,
    onSignOut: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Puntuall",
                style = MaterialTheme.typography.headlineMedium,
                color = PuntualGreen,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Confirma tu identidad para continuar.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onUnlockClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PuntualGreen),
            ) {
                Text("Usar huella")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            ) {
                Text("Cerrar sesión")
            }
        }
    }
}

@Composable
private fun PuntualLaunchLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PuntualGreen)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Preparando Puntuall...",
                style = MaterialTheme.typography.titleMedium,
                color = PuntualGreen,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Validando sesión",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}
