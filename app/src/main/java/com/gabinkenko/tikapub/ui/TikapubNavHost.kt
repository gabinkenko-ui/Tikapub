package com.gabinkenko.tikapub.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gabinkenko.tikapub.AppContainer
import com.gabinkenko.tikapub.ui.common.SimpleViewModelFactory
import com.gabinkenko.tikapub.ui.history.HistoryScreen
import com.gabinkenko.tikapub.ui.history.HistoryViewModel
import com.gabinkenko.tikapub.ui.home.HomeScreen
import com.gabinkenko.tikapub.ui.home.HomeViewModel
import com.gabinkenko.tikapub.ui.quotes.QuotesScreen
import com.gabinkenko.tikapub.ui.quotes.QuotesViewModel
import com.gabinkenko.tikapub.ui.settings.SettingsScreen
import com.gabinkenko.tikapub.ui.settings.SettingsViewModel

private sealed class Destination(val route: String, val label: String) {
    data object Home : Destination("home", "Accueil")
    data object Quotes : Destination("quotes", "Citations")
    data object History : Destination("history", "Historique")
    data object Settings : Destination("settings", "Réglages")
}

private val destinations = listOf(Destination.Home, Destination.Quotes, Destination.History, Destination.Settings)

@Composable
fun TikapubNavHost(container: AppContainer) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                destinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(iconFor(destination), contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Home.route) {
                val vm: HomeViewModel = viewModel(factory = SimpleViewModelFactory { HomeViewModel(container) })
                HomeScreen(vm)
            }
            composable(Destination.Quotes.route) {
                val vm: QuotesViewModel = viewModel(factory = SimpleViewModelFactory { QuotesViewModel(container) })
                QuotesScreen(vm)
            }
            composable(Destination.History.route) {
                val vm: HistoryViewModel = viewModel(factory = SimpleViewModelFactory { HistoryViewModel(container) })
                HistoryScreen(vm)
            }
            composable(Destination.Settings.route) {
                val vm: SettingsViewModel = viewModel(factory = SimpleViewModelFactory { SettingsViewModel(container) })
                SettingsScreen(vm, container.authManager)
            }
        }
    }
}

private fun iconFor(destination: Destination) = when (destination) {
    Destination.Home -> Icons.Filled.Home
    Destination.Quotes -> Icons.Filled.FormatQuote
    Destination.History -> Icons.Filled.History
    Destination.Settings -> Icons.Filled.Settings
}
