package com.tnhc.community.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tnhc.community.data.Project

@Composable
fun HomeScreen(projects: List<Project>, onBrowse: () -> Unit, onProject: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("THE NO HANDS COMPANY", style = MaterialTheme.typography.labelMedium)
                    Text("Build something\ntogether.", style = MaterialTheme.typography.headlineLarge)
                    Text("Discover an idea. Follow your curiosity. Find a place in the making.", style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onBrowse) { Text("Explore projects") }
                }
            }
        }
        item {
            Text("Made for the curious", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Games, tools, engines and experiments. A future home for TNHC’s 300+ projects and the people who help them grow.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Text("A glimpse of the possibilities", style = MaterialTheme.typography.titleMedium) }
        items(projects.take(2), key = { it.id }) { project -> ProjectCard(project) { onProject(project.id) } }
        item {
            Text("This is the beginning", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("These examples are here to explore the app. Real project imports, accounts and conversations are still ahead.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onBrowse) { Text("Browse all demo projects") }
        }
    }
}
