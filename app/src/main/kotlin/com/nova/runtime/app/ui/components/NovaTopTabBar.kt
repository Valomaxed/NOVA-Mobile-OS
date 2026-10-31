package com.nova.runtime.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.runtime.app.ui.theme.NovaColors

/** Brand and contextual heading. Primary destinations live in the bottom navigation. */
@Composable
fun NovaTopTabBar(
    selectedTab: NovaScreenTab,
    onTabSelected: (NovaScreenTab) -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("NOVA", style = MaterialTheme.typography.titleMedium, color = NovaColors.textPrimary)
                Text(selectedTab.displayName, style = MaterialTheme.typography.labelLarge, color = NovaColors.textSecondary)
            }
            IconButton(onClick = { onTabSelected(NovaScreenTab.COMMAND) }) {
                Icon(Icons.Filled.Terminal, contentDescription = "Open commands", tint = NovaColors.textPrimary)
            }
            IconButton(onClick = onProfileClick) {
                Icon(Icons.Filled.AccountCircle, contentDescription = "Edit your profile", tint = NovaColors.textPrimary)
            }
        }
    }
}
