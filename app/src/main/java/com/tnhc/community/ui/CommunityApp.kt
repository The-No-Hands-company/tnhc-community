package com.tnhc.community.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import com.tnhc.community.data.DemoProjectRepository
import com.tnhc.community.data.ProjectRepository

private enum class Destination(val icon: ImageVector) {
    Home(Icons.Outlined.Home), Projects(Icons.Outlined.Dashboard),
    Community(Icons.Outlined.Groups), Messages(Icons.Outlined.ChatBubbleOutline),
    Profile(Icons.Outlined.PersonOutline),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityApp(repository: ProjectRepository = DemoProjectRepository) {
    var selectedTab by rememberSaveable { mutableStateOf(Destination.Home.name) }
    var projectId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    val result = remember(repository, attempt) { runCatching { repository.load() } }
    val browse = { selectedTab = Destination.Projects.name; projectId = null }
    val back = { if (projectId != null) projectId = null else selectedTab = Destination.Home.name }
    BackHandler(enabled = projectId != null || selectedTab != Destination.Home.name, onBack = back)

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (projectId == null) "TNHC Community" else "Project preview") },
                navigationIcon = {
                    if (projectId != null) IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            // Five labels share a narrow bar: cap their scale so they stay whole at large font sizes.
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))) {
                NavigationBar {
                    Destination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = selectedTab == destination.name,
                            onClick = { selectedTab = destination.name; projectId = null },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.name, maxLines = 1, softWrap = false) },
                            modifier = Modifier.testTag("tab-${destination.name}"),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    "Demo preview · Fictional projects · Offline",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Box(Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                val projects = result.getOrNull().orEmpty()
                when {
                    projectId != null && result.isFailure -> StatusScreen("Projects could not load", "The local catalogue is unavailable. Try loading it again.", "Try again", { attempt++ })
                    projectId != null -> ProjectDetail(projects.find { it.id == projectId }, back)
                    selectedTab == "Community" -> UpcomingScreen(
                        "Find your people.", "A place for shared curiosity",
                        "Discuss game development, software, design and hardware across projects. Topic conversations are planned for alpha 0.0.3.",
                        Icons.Outlined.Groups, browse,
                    )
                    selectedTab == "Messages" -> UpcomingScreen(
                        "Great work starts with a conversation.", "Private messages come later",
                        "Connect with collaborators when accounts, blocking and moderation are ready. Private messaging is planned for alpha 0.0.5.",
                        Icons.Outlined.ChatBubbleOutline, browse,
                    )
                    selectedTab == "Profile" -> UpcomingScreen(
                        "Bring your curiosity.", "Everyone has something to contribute",
                        "Developer, artist, designer, tester or enthusiast: you belong here. Accounts, interests and skills are planned for alpha 0.0.2. This preview does not create an account.",
                        Icons.Outlined.PersonOutline, browse, showVersion = true,
                    )
                    result.isFailure -> StatusScreen("Projects could not load", "The local catalogue is unavailable. Try loading it again.", "Try again", { attempt++ })
                    projects.isEmpty() -> StatusScreen("No projects yet", "The demo catalogue is empty. Try reloading it.", "Reload projects", { attempt++ })
                    selectedTab == "Projects" -> ProjectsScreen(projects, query, category,
                        { query = it }, { category = it }, { projectId = it })
                    else -> HomeScreen(projects, browse, { projectId = it })
                }
            }
        }
    }
}
