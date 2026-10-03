package com.tnhc.community.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tnhc.community.data.Project
import com.tnhc.community.data.filterProjects

@Composable
fun ProjectsScreen(
    projects: List<Project>, query: String, category: String?,
    onQuery: (String) -> Unit, onCategory: (String?) -> Unit, onProject: (String) -> Unit,
) {
    val filtered = filterProjects(projects, query, category)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Find your next curiosity.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Explore a few examples of what a project home can be.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = onQuery, label = { Text("Search demo projects") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("project-search"),
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = category == null, onClick = { onCategory(null) }, label = { Text("All") }) }
                items(projects.map { it.category }.distinct()) { value ->
                    FilterChip(selected = category == value, onClick = { onCategory(value) }, label = { Text(value) })
                }
            }
        }
        item { Text("${filtered.size} demo projects", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (filtered.isEmpty()) {
            item {
                Text("No matching projects", style = MaterialTheme.typography.titleLarge)
                Text("Try another name, interest or category.")
                TextButton(onClick = { onQuery(""); onCategory(null) }) { Text("Clear filters") }
            }
        }
        items(filtered, key = { it.id }) { project -> ProjectCard(project) { onProject(project.id) } }
    }
}

@Composable
fun ProjectCard(project: Project, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("project-${project.id}")) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${project.category.uppercase()}  /  ${project.stage}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(project.title, style = MaterialTheme.typography.titleLarge)
            Text(project.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Demo · ${project.affiliation} example", style = MaterialTheme.typography.labelMedium)
            Text("Explore project →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ProjectDetail(project: Project?, onBack: () -> Unit) {
    if (project == null) {
        StatusScreen("Project unavailable", "This project is no longer in the local catalogue.", "Go back", onBack)
        return
    }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Text(project.category.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(project.title, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            Text(project.summary, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${project.stage} · ${project.affiliation} example", style = MaterialTheme.typography.titleMedium)
                    Text("Fictional demo project. This is an example of a project page, not an announced product.")
                }
            }
        }
        item {
            Text("Current focus", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(project.focus)
        }
        item {
            Text("Interests", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(project.tags.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Help shape what comes next", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Following, feedback and contribution opportunities will arrive in future alphas. For now, explore the catalogue.")
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onBack) { Text("Back to browsing") }
        }
    }
}
