package com.tnhc.community.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tnhc.community.BuildConfig

@Composable
fun UpcomingScreen(title: String, subtitle: String, body: String, icon: ImageVector, onBrowse: () -> Unit, showVersion: Boolean = false) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onBrowse) { Text("Explore projects") }
        if (showVersion) {
            HorizontalDivider()
            Text("Development build ${BuildConfig.VERSION_NAME}\nAlpha target ${BuildConfig.TARGET_VERSION}", style = MaterialTheme.typography.labelLarge)
            Text("This offline preview uses no account and sends no data. Search and navigation state may be restored by Android; there is no member profile yet.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun StatusScreen(title: String, message: String, action: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onAction) { Text(action) }
    }
}
