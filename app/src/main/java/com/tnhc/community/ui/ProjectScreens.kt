package com.tnhc.community.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tnhc.community.data.Project
import com.tnhc.community.data.filterProjects

@Composable
fun ProjectsScreen(
    projects: List<Project>, query: String, category: String?,
    onQuery: (String) -> Unit, onCategory: (String?) -> Unit, onProject: (String) -> Unit,
) {
    val filtered = filterProjects(projects, query, category)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.testTag("projects-list")) {
        item {
            Text("Find your next curiosity.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Explore a few examples of what a project home can be.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            val keyboard = LocalSoftwareKeyboardController.current
            OutlinedTextField(
                value = query, onValueChange = onQuery, label = { Text(if (projects.all { it.isDemo }) "Search demo projects" else "Search projects") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth().testTag("project-search"),
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = category == null, onClick = { onCategory(null) }, label = { Text("All") }) }
                items(projects.map { it.category }.distinct()) { value ->
                    FilterChip(selected = category == value, onClick = { onCategory(value) }, label = { Text(value) }, modifier = Modifier.testTag("category-$value"))
                }
            }
        }
        item { Text("${filtered.size} ${if (projects.all { it.isDemo }) "demo " else ""}projects", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
            Text(if (project.isDemo) "Demo · ${project.affiliation} example" else "${project.affiliation.replaceFirstChar(Char::uppercase)} project", style = MaterialTheme.typography.labelMedium)
            Text("Explore project →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ProjectDetail(
    project: Project?, onBack: () -> Unit,
    following: Boolean? = null, followPending: Boolean = false, followError: String? = null,
    onToggleFollow: (() -> Unit)? = null, onSignIn: (() -> Unit)? = null, signedIn: Boolean = false,
) {
    if (project == null) {
        StatusScreen("Project unavailable", "This project is no longer in the local catalogue.", "Go back", onBack)
        return
    }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.testTag("project-detail-list")) {
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
                    Text(if (project.isDemo) "${project.stage} · ${project.affiliation} example" else "${project.stage} · ${project.affiliation.replaceFirstChar(Char::uppercase)} project", style = MaterialTheme.typography.titleMedium)
                    Text(if (project.isDemo) "Fictional demo project. This is an example of a project page, not an announced product." else "Project information shared by its listed owner.")
                }
            }
        }
        item {
            Text("Current focus", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(project.focus)
        }
        item {
            val uriHandler = LocalUriHandler.current
            if (project.websiteUrl != null || project.repositoryUrl != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    project.websiteUrl?.let { url ->
                        OutlinedButton(onClick = { uriHandler.openUri(url) }, modifier = Modifier.testTag("project-website")) {
                            Text("Visit website")
                        }
                    }
                    project.repositoryUrl?.let { url ->
                        OutlinedButton(onClick = { uriHandler.openUri(url) }, modifier = Modifier.testTag("project-repository")) {
                            Text("Source repository")
                        }
                    }
                }
            }
            project.statusAsOf?.let {
                Spacer(Modifier.height(8.dp))
                Text("Source date · $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
            when {
                project.isDemo -> Text("Following, feedback and contribution opportunities will arrive in future alphas. For now, explore the catalogue.")
                !signedIn && onSignIn != null -> {
                    Text("Sign in with your TNHC invitation to follow this project and keep up with its progress.")
                    OutlinedButton(onClick = onSignIn, modifier = Modifier.testTag("project-sign-in")) { Text("Sign in to follow") }
                }
                following != null && onToggleFollow != null -> {
                    Text("Follow projects to keep them close and support their development.")
                    Button(onClick = onToggleFollow, enabled = !followPending, modifier = Modifier.testTag("project-follow")) {
                        Text(if (followPending) "Saving…" else if (following) "Following · tap to unfollow" else "Follow project")
                    }
                    if (followError != null) Text(followError, color = MaterialTheme.colorScheme.error)
                }
                signedIn -> Text(followError ?: "Loading your project follows…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Text("Project follows and community participation are being connected to your account.")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onBack) { Text("Back to browsing") }
        }
    }
}
