package com.example.puntual.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.puntual.ui.theme.TextSecondary

@Composable
fun PuntualApp(
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    if (!authState.isSessionResolved) {
        PuntualLaunchLoadingScreen()
        return
    }

    if (!authState.isAuthenticated) {
        LoginScreen(viewModel = authViewModel)
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
