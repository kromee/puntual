package com.example.puntual.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.puntual.ui.history.HistoryScreen
import com.example.puntual.ui.home.HomeScreen
import com.example.puntual.ui.settings.SettingsScreen

@Composable
fun PuntualNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = PuntualRoute.Home.route,
        modifier = modifier,
    ) {
        composable(PuntualRoute.Home.route) {
            HomeScreen()
        }
        composable(PuntualRoute.History.route) {
            HistoryScreen()
        }
        composable(PuntualRoute.Settings.route) {
            SettingsScreen(onSignOut = onSignOut)
        }
    }
}
