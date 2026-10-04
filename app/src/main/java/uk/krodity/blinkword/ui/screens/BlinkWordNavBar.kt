package uk.krodity.blinkword.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import uk.krodity.blinkword.ui.Screen

@Composable
fun BlinkWordNavBar(
    current: Screen,
    onLibrary: () -> Unit,
    onDiscover: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = current == Screen.LIBRARY,
            onClick = onLibrary,
            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
            label = { Text("Library") },
        )
        NavigationBarItem(
            selected = current == Screen.DISCOVER,
            onClick = onDiscover,
            icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
            label = { Text("Discover") },
        )
        NavigationBarItem(
            selected = current == Screen.STATS,
            onClick = onStats,
            icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
            label = { Text("Stats") },
        )
        // Settings is a sheet rather than a destination, so this item never shows selected.
        NavigationBarItem(
            selected = false,
            onClick = onSettings,
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text("Settings") },
        )
    }
}
