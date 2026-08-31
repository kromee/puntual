package com.example.puntual.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.example.puntual.ui.theme.ScreenBackground

@Composable
fun PuntualApp(
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

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
