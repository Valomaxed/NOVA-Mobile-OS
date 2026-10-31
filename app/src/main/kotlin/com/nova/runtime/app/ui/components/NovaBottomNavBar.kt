package com.nova.runtime.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.runtime.app.ui.theme.NovaColors

/** Native selection semantics, touch feedback, and system navigation insets. */
@Composable
fun NovaBottomNavBar(
    selectedTab: NovaScreenTab,
    onTabSelected: (NovaScreenTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showLabels = LocalDensity.current.fontScale <= 1.3f
    val destinations = listOf(
        NovaScreenTab.DASHBOARD to Icons.Filled.Analytics,
        NovaScreenTab.DOCUMENTS to Icons.Filled.Folder,
        NovaScreenTab.ACTIONS to Icons.Filled.Bolt,
        NovaScreenTab.COMMAND to Icons.Filled.Terminal,
        NovaScreenTab.SETTINGS to Icons.Filled.Settings,
    )
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = NovaColors.surface, tonalElevation = 0.dp) {
            destinations.forEach { (tab, icon) ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = { Icon(icon, contentDescription = if (showLabels) null else tab.displayName) },
                    label = if (showLabels) {
                        { Text(tab.displayName, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    } else {
                        null
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NovaColors.primary,
                        selectedTextColor = NovaColors.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = NovaColors.textSecondary,
                        unselectedTextColor = NovaColors.textSecondary,
                    ),
                )
            }
        }
    }
}
