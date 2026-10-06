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
import com.tnhc.community.data.CommunityRepository
import com.tnhc.community.data.Project
import com.tnhc.community.data.CommunitySession
import com.tnhc.community.data.loadAllProjects
import com.tnhc.community.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class Destination(val icon: ImageVector) {
    Home(Icons.Outlined.Home), Projects(Icons.Outlined.Dashboard),
    Community(Icons.Outlined.Groups), Messages(Icons.Outlined.ChatBubbleOutline),
    Profile(Icons.Outlined.PersonOutline), Founder(Icons.Outlined.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityApp(
    repository: ProjectRepository = DemoProjectRepository,
    communityRepository: CommunityRepository? = null,
    founderConsoleContent: @Composable () -> Unit = {},
    founderBuild: Boolean = BuildConfig.FOUNDER_BUILD,
) {
    var selectedTab by rememberSaveable { mutableStateOf(Destination.Home.name) }
    var projectId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    val result = remember(repository, attempt) { runCatching { repository.load() } }
    var liveAttempt by remember { mutableIntStateOf(0) }
    val sessionState = communityRepository?.session?.collectAsState(initial = null)
    val session: CommunitySession? = sessionState?.value
    var founderAccess by remember(communityRepository, session?.userId, founderBuild) {
        mutableStateOf<FounderAccess>(FounderAccess.Loading)
    }
    LaunchedEffect(communityRepository, session?.userId, founderBuild) {
        founderAccess = FounderAccess.Loading
        founderAccess = resolveFounderAccess(
            founderBuild = founderBuild,
            signedIn = session != null,
        ) {
            communityRepository?.isFounder() == true
        }
    }
    LaunchedEffect(founderAccess, session?.userId, founderBuild) {
        if ((!founderBuild || founderAccess != FounderAccess.Allowed) && selectedTab == Destination.Founder.name) {
            selectedTab = Destination.Home.name
        }
    }
    var liveProjects by remember(communityRepository, session?.userId) { mutableStateOf<List<Project>?>(null) }
    var liveLoadError by remember(communityRepository, session?.userId) { mutableStateOf(false) }
    var liveLoading by remember(communityRepository, session?.userId) { mutableStateOf(communityRepository != null) }
    val scope = rememberCoroutineScope()
    var followedProjectIds by remember(communityRepository, session?.userId) { mutableStateOf<Set<String>?>(null) }
    var followLoadError by remember(communityRepository, session?.userId) { mutableStateOf(false) }
    var followPendingId by remember { mutableStateOf<String?>(null) }
    var followActionError by remember { mutableStateOf<String?>(null) }
    var followAttempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(communityRepository, session?.userId, followAttempt) {
        followedProjectIds = null
        followLoadError = false
        if (communityRepository == null || session == null) return@LaunchedEffect
        try {
            followedProjectIds = communityRepository.loadFollowedProjectIds()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            followLoadError = true
        }
    }
    LaunchedEffect(communityRepository, session?.userId, liveAttempt) {
        if (communityRepository == null) return@LaunchedEffect
        liveLoading = true
        liveLoadError = false
        try {
            liveProjects = communityRepository.loadAllProjects()
            liveLoadError = false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            liveLoadError = true
        } finally {
            liveLoading = false
        }
    }
    val browse = { selectedTab = Destination.Projects.name; projectId = null }
    val back = { if (projectId != null) projectId = null else selectedTab = Destination.Home.name }
    BackHandler(enabled = projectId != null || selectedTab != Destination.Home.name, onBack = back)

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            projectId != null -> "Project details"
                            founderBuild -> "TNHC Founder"
                            else -> "TNHC Community"
                        },
                    )
                },
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
                        Destination.entries.filter { it != Destination.Founder || founderAccess == FounderAccess.Allowed }.forEach { destination ->
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
                    if (communityRepository == null) "Demo preview · Fictional projects · Offline"
                    else "Live project catalogue · Invite-only community",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Box(Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                val projects = if (communityRepository == null) result.getOrNull().orEmpty() else liveProjects.orEmpty()
                when {
                    projectId != null && ((communityRepository == null && result.isFailure) || liveLoadError) -> StatusScreen("Projects could not load", "The project catalogue is unavailable. Check your connection and try again.", "Try again", { if (communityRepository == null) attempt++ else liveAttempt++ })
                    projectId != null -> {
                        val selectedProject = projects.find { it.id == projectId }
                        val liveFollow = selectedProject?.takeIf { !it.isDemo } != null && communityRepository != null
                        ProjectDetail(
                            selectedProject, back,
                            following = if (liveFollow && session != null && !followLoadError) followedProjectIds?.contains(projectId) else null,
                            followPending = followPendingId == projectId,
                            followError = when {
                                followActionError != null -> followActionError
                                followLoadError -> "Your follow status could not load. Tap to retry."
                                else -> null
                            },
                            onToggleFollow = if (liveFollow && session != null && followedProjectIds != null) ({
                                val id = projectId!!
                                val shouldFollow = !followedProjectIds!!.contains(id)
                                followPendingId = id
                                followActionError = null
                                scope.launch {
                                    try {
                                        communityRepository!!.setProjectFollow(id, shouldFollow)
                                        followedProjectIds = if (shouldFollow) followedProjectIds.orEmpty() + id else followedProjectIds.orEmpty() - id
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        followActionError = "Your change could not be saved. Check your connection and try again."
                                    } finally {
                                        followPendingId = null
                                    }
                                }
                            }) else null,
                            onSignIn = if (liveFollow && session == null) ({ selectedTab = Destination.Profile.name; projectId = null }) else null,
                            signedIn = liveFollow && session != null,
                        )
                        if (liveFollow && session != null && followLoadError) {
                            TextButton(onClick = { followAttempt++ }) { Text("Retry") }
                        }
                    }
                    selectedTab == "Community" -> UpcomingScreen(
                        "Find your people.", "A place for shared curiosity",
                        "Discuss game development, software, design and hardware across projects. Topic conversations are planned for alpha 0.0.3.",
                        Icons.Outlined.Groups, browse,
                    )
                    selectedTab == Destination.Founder.name && founderAccess == FounderAccess.Allowed ->
                        founderConsoleContent()
                    selectedTab == "Messages" -> UpcomingScreen(
                        "Great work starts with a conversation.", "Private messages come later",
                        "Connect with collaborators when accounts, blocking and moderation are ready. Private messaging is planned for alpha 0.0.5.",
                        Icons.Outlined.ChatBubbleOutline, browse,
                    )
                    selectedTab == "Profile" && communityRepository != null -> AccountScreen(communityRepository)
                    selectedTab == "Profile" -> UpcomingScreen(
                        "Bring your curiosity.", "Everyone has something to contribute",
                        "Developer, artist, designer, tester or enthusiast: you belong here. Sign-in and project follows are available when the app is connected to the TNHC backend.",
                        Icons.Outlined.PersonOutline, browse, showVersion = true,
                    )
                    (communityRepository == null && result.isFailure) || liveLoadError -> StatusScreen("Projects could not load", "The project catalogue is unavailable. Check your connection and try again.", "Try again", { if (communityRepository == null) attempt++ else liveAttempt++ })
                    liveLoading -> StatusScreen("Loading projects", "Connecting to the TNHC project catalogue…", "Try again", { liveAttempt++ })
                    projects.isEmpty() -> StatusScreen("No projects yet", if (communityRepository == null) "The demo catalogue is empty. Try reloading it." else "There are no projects visible to the community yet.", if (communityRepository == null) "Reload projects" else "Try again", { if (communityRepository == null) attempt++ else liveAttempt++ })
                    selectedTab == "Projects" -> ProjectsScreen(projects, query, category,
                        { query = it }, { category = it }, { projectId = it })
                    else -> HomeScreen(projects, browse, { projectId = it })
                }
            }
        }
    }
}
