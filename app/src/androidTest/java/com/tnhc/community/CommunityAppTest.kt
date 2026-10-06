package com.tnhc.community

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tnhc.community.data.DemoProjectRepository
import com.tnhc.community.data.CommunityRepository
import com.tnhc.community.data.CommunityPost
import com.tnhc.community.data.CommunitySession
import com.tnhc.community.data.CommunityTopic
import com.tnhc.community.data.MemberProfile
import com.tnhc.community.data.Project
import com.tnhc.community.data.ProjectFilter
import com.tnhc.community.data.ProjectPage
import com.tnhc.community.data.ProjectRepository
import com.tnhc.community.data.EncryptedAndroidSessionManager
import com.tnhc.community.data.SupabaseCommunityRemoteDataSource
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.tnhc.community.ui.CommunityApp
import com.tnhc.community.ui.TnhcTheme
import com.tnhc.community.ui.ProjectDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommunityAppTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun projectDetailShowsOnlyAvailableSourceActions() {
        val linkedProject = Project("linked", "Nexus", "Real-time chat", "Core", "Released", listOf("nexus"), "Chat", "Official",
            isDemo = false, websiteUrl = "https://chat.tnhc.dev/", repositoryUrl = "https://github.com/The-No-Hands-company/Nexus-Systems", statusAsOf = "2026-08-19")
        compose.setContent { TnhcTheme { ProjectDetail(linkedProject, onBack = {}) } }
        compose.onNodeWithTag("project-website").assertIsDisplayed()
        compose.onNodeWithTag("project-repository").assertIsDisplayed()
        compose.onNodeWithText("Source date · 2026-08-19").assertIsDisplayed()

        compose.setContent { TnhcTheme { ProjectDetail(linkedProject.copy(websiteUrl = null, repositoryUrl = null), onBack = {}) } }
        compose.onNodeWithTag("project-website").assertDoesNotExist()
        compose.onNodeWithTag("project-repository").assertDoesNotExist()
    }

    @Test fun allDestinationsOpenAndProjectBackReturnsToCatalogue() {
        compose.setContent { TnhcTheme { CommunityApp() } }
        listOf("Community", "Messages", "Profile", "Home", "Projects").forEach {
            compose.onNodeWithTag("tab-$it").performClick().assertIsSelected()
        }
        compose.onNodeWithTag("project-demo-orbit").performClick()
        compose.onNodeWithText("Current focus").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("project-search").assertIsDisplayed()
    }

    @Test fun communityShowsPublicTopicsAndTheirVisiblePosts() {
        val repository = FakeCommunityRepository()
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Community").performClick()
        compose.waitUntil(5_000) { repository.topicLoads > 0 }
        compose.onNodeWithText("Start Here").assertIsDisplayed()
        compose.onNodeWithText("Welcome to the TNHC community.").assertIsDisplayed()
    }

    @Test fun signedInMemberCanJoinPostAndLeaveACommunity() {
        val repository = FakeCommunityRepository().apply {
            currentSession.value = CommunitySession("member-1")
        }
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Community").performClick()
        compose.waitUntil(5_000) { repository.topicLoads > 0 }
        compose.onNodeWithTag("community-join-start-here").performClick()
        compose.onNodeWithTag("community-topic-start-here").performClick()
        compose.onNodeWithTag("community-post-composer").performTextInput("Hello builders")
        compose.onNodeWithTag("community-post-submit").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            repository.createdPosts.contains("Hello builders") &&
                compose.onAllNodesWithText("Hello builders").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Hello builders").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("community-leave").performScrollTo().performClick()
        compose.onNodeWithTag("community-post-composer").assertDoesNotExist()
        assertEquals(listOf("start-here" to true, "start-here" to false), repository.membershipChanges)
    }

    @Test fun inactiveMemberCannotSeePostComposer() {
        val repository = FakeCommunityRepository().apply {
            currentSession.value = CommunitySession("member-1")
            joinedTopics += "start-here"
            activeMember = false
        }
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Community").performClick()
        compose.onNodeWithTag("community-topic-start-here").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Your account is not active for posting right now.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Your account is not active for posting right now.").assertIsDisplayed()
        compose.onNodeWithTag("community-post-composer").assertDoesNotExist()
    }

    @Test fun visitorCanReadTopicsButJoiningLeadsToInviteOnlySignIn() {
        val repository = FakeCommunityRepository()
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Community").performClick()
        compose.waitUntil(5_000) { repository.topicLoads > 0 }
        compose.onNodeWithText("Start Here").assertIsDisplayed()
        compose.onNodeWithTag("community-join-start-here").performClick()
        compose.onNodeWithTag("tab-Profile").assertIsSelected()
        compose.onNodeWithText("New accounts require an invitation from TNHC.").assertIsDisplayed()
        assertTrue(repository.membershipChanges.isEmpty())
    }

    @Test fun communityLoadFailureCanBeRetried() {
        val repository = FakeCommunityRepository().apply { topicLoadFailure = true }
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Community").performClick()
        compose.onNodeWithText("Communities could not load").assertIsDisplayed()
        compose.runOnIdle { repository.topicLoadFailure = false }
        compose.onNodeWithTag("community-retry").performClick()
        compose.onNodeWithText("Start Here").assertIsDisplayed()
    }

    @Test fun emptyTopicFeedExplainsHowToStart() {
        val repository = FakeCommunityRepository().apply { topicPosts.clear() }
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Community").performClick()
        compose.onNodeWithTag("community-topic-start-here").performClick()
        compose.onNodeWithText("No posts yet in this community.").assertIsDisplayed()
    }

    @Test fun topicPostLoadFailureCanBeRetried() {
        val repository = FakeCommunityRepository().apply { topicPostLoadFailure = true }
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Community").performClick()
        compose.onNodeWithTag("community-topic-start-here").performClick()
        compose.onNodeWithText("Posts could not load.").assertIsDisplayed()
        compose.runOnIdle { repository.topicPostLoadFailure = false }
        compose.onNodeWithTag("community-feed-retry").performClick()
        compose.onNodeWithText("Welcome to the TNHC community.").assertIsDisplayed()
    }

    @Test fun failedCommunityPostKeepsDraftAndExplainsFailure() {
        val repository = FakeCommunityRepository().apply {
            currentSession.value = CommunitySession("member-1")
            postCreateFailure = true
        }
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Community").performClick()
        compose.onNodeWithTag("community-join-start-here").performClick()
        compose.onNodeWithTag("community-topic-start-here").performClick()
        compose.onNodeWithTag("community-post-composer").performTextInput("Please keep this draft")
        compose.onNodeWithTag("community-post-submit").performScrollTo().performClick()
        compose.onNodeWithText("Your post could not be published. Check your connection and try again.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("community-post-composer").assertTextContains("Please keep this draft")
        assertTrue(repository.createdPosts.isEmpty())
    }

    @Test fun emptyCommunityCatalogueExplainsThereAreNoTopicsYet() {
        val repository = FakeCommunityRepository().apply { topicCatalogue = emptyList() }
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Community").performClick()
        compose.onNodeWithText("No communities yet").assertIsDisplayed()
    }

    @Test fun memberBuildNeverShowsFounderNavigation() {
        val repository = FakeCommunityRepository().apply {
            currentSession.value = CommunitySession("member-1")
            founderRole = true
        }
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository, founderBuild = false) } }
        compose.onNodeWithText("TNHC Community").assertIsDisplayed()
        compose.onNodeWithTag("tab-Founder").assertDoesNotExist()
        assertEquals(0, repository.founderRoleChecks)
    }

    @Test fun founderBuildShowsConsoleOnlyAfterServerConfirmsFounder() {
        val repository = FakeCommunityRepository().apply {
            currentSession.value = CommunitySession("founder-1")
            founderRole = true
        }
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository, founderBuild = true) } }
        compose.waitUntil(5_000) { repository.founderRoleChecks > 0 }
        compose.onNodeWithText("TNHC Founder").assertIsDisplayed()
        compose.onNodeWithTag("tab-Founder").assertIsDisplayed()
    }

    @Test fun regularAccountInFounderBuildDoesNotSeeConsole() {
        val repository = FakeCommunityRepository().apply {
            currentSession.value = CommunitySession("member-1")
            founderRole = false
        }
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository, founderBuild = true) } }
        compose.waitUntil(5_000) { repository.founderRoleChecks > 0 }
        compose.onNodeWithText("TNHC Founder").assertIsDisplayed()
        compose.onNodeWithTag("tab-Founder").assertDoesNotExist()
    }

    @Test fun founderRoleLoadFailureFailsClosed() {
        val repository = FakeCommunityRepository().apply {
            currentSession.value = CommunitySession("founder-1")
            founderRoleFailure = true
        }
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository, founderBuild = true) } }
        compose.waitUntil(5_000) { repository.founderRoleChecks > 0 }
        compose.onNodeWithTag("tab-Founder").assertDoesNotExist()
    }

    @Test fun noMatchesCanBeCleared() {
        compose.setContent { TnhcTheme { CommunityApp() } }
        compose.onNodeWithTag("tab-Projects").performClick()
        compose.onNodeWithTag("project-search").performTextInput("nothingmatches")
        compose.onNodeWithTag("project-search").performImeAction()
        compose.onNodeWithTag("projects-list").performScrollToNode(hasText("No matching projects"))
        compose.onNodeWithText("No matching projects").assertIsDisplayed()
        compose.onNodeWithText("Clear filters").performClick()
        compose.onNodeWithTag("project-demo-orbit").assertExists()
    }

    @Test fun selectedProjectAndSearchSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { TnhcTheme { CommunityApp() } }
        compose.onNodeWithTag("tab-Projects").performClick()
        compose.onNodeWithTag("project-search").performTextInput("Orbit")
        compose.onNodeWithTag("project-search").performImeAction()
        compose.onNodeWithTag("category-Games").performClick()
        compose.onNodeWithTag("category-Games").assertIsSelected()
        compose.onNodeWithTag("projects-list").performScrollToNode(hasTestTag("project-demo-orbit"))
        compose.onNodeWithTag("project-demo-orbit").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Current focus").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("project-search").assertTextContains("Orbit")
        compose.onNodeWithTag("category-Games").assertIsSelected()
    }

    @Test fun retryRecoversFromRepositoryFailure() {
        var failed = true
        val repository = ProjectRepository {
            if (failed) error("Synthetic failure") else DemoProjectRepository.load()
        }
        compose.setContent { TnhcTheme { CommunityApp(repository) } }
        compose.onNodeWithText("Projects could not load").assertIsDisplayed()
        compose.runOnIdle { failed = false }
        compose.onNodeWithText("Try again").performClick()
        compose.onNodeWithText("Build something\ntogether.").assertIsDisplayed()
    }

    @Test fun emptyRepositoryExplainsState() {
        compose.setContent { TnhcTheme { CommunityApp(ProjectRepository { emptyList() }) } }
        compose.onNodeWithText("No projects yet").assertIsDisplayed()
    }

    @Test fun restoredDetailCanRetryWithoutLosingSelectedProject() {
        var failed = false
        val repository = ProjectRepository {
            if (failed) error("Synthetic failure") else DemoProjectRepository.load()
        }
        val restoration = StateRestorationTester(compose)
        restoration.setContent { TnhcTheme { CommunityApp(repository) } }
        compose.onNodeWithTag("tab-Projects").performClick()
        compose.onNodeWithTag("project-demo-orbit").performClick()
        compose.runOnIdle { failed = true }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Projects could not load").assertIsDisplayed()
        compose.runOnIdle { failed = false }
        compose.onNodeWithText("Try again").performClick()
        compose.onNodeWithText("Current focus").assertExists()
    }

    @Test fun systemBackClosesDetailsThenReturnsHome() {
        compose.setContent { TnhcTheme { CommunityApp() } }
        compose.onNodeWithTag("tab-Projects").performClick()
        compose.onNodeWithTag("project-demo-orbit").performClick()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("project-search").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("tab-Home").assertIsSelected()
    }

    @Test fun missingRestoredProjectCanReturnToCatalogue() {
        var removed = false
        val repository = ProjectRepository {
            DemoProjectRepository.load().filter { !removed || it.id != "demo-orbit" }
        }
        val restoration = StateRestorationTester(compose)
        restoration.setContent { TnhcTheme { CommunityApp(repository) } }
        compose.onNodeWithTag("tab-Projects").performClick()
        compose.onNodeWithTag("project-demo-orbit").performClick()
        compose.runOnIdle { removed = true }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Project unavailable").assertIsDisplayed()
        compose.onNodeWithText("Go back").performClick()
        compose.onNodeWithTag("project-search").assertIsDisplayed()
    }

    @Test fun profileOffersInviteOnlySignInAndNeverOpenRegistration() {
        val repository = FakeCommunityRepository()
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Profile").performClick()
        compose.onNodeWithText("Sign in").assertIsDisplayed()
        compose.onNodeWithText("New accounts require an invitation from TNHC.").assertIsDisplayed()
        compose.onNodeWithText("Create account").assertDoesNotExist()
        compose.onNodeWithTag("account-email").performTextInput("member@example.com")
        compose.onNodeWithTag("account-password").performTextInput("correct horse battery")
        compose.onNodeWithTag("account-password").performImeAction()
        compose.onNodeWithTag("account-sign-in").assertIsEnabled()
        compose.onNodeWithTag("account-sign-in").performClick()
        compose.waitUntil(5_000) { repository.currentSession.value?.userId == "member-1" }
        compose.waitUntil(5_000) { repository.profileLoads > 0 }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("@pilotmember").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("@pilotmember").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("account-display-name").performScrollTo().performTextClearance()
        compose.onNodeWithTag("account-display-name").performTextInput("A Pilot")
        compose.onNodeWithTag("account-interests").performScrollTo().performTextClearance()
        compose.onNodeWithTag("account-interests").performTextInput("Games, Design")
        compose.onNodeWithTag("account-save").performScrollTo().performClick()
        compose.onNodeWithText("Profile saved.").assertIsDisplayed()
        assert(repository.savedDisplayName == "A Pilot")
        assert(repository.savedInterests == listOf("Games", "Design"))
        compose.onNodeWithTag("account-new-password").performScrollTo().performTextInput("a much longer passphrase")
        compose.onNodeWithTag("account-new-password").performImeAction()
        compose.onNodeWithTag("account-set-password").assertIsEnabled()
        compose.onNodeWithTag("account-set-password").performScrollTo().performClick()
        compose.onNodeWithText("Password updated.").assertIsDisplayed()
        assert(repository.passwordUpdates == 1)
        compose.onNodeWithTag("account-sign-out").performScrollTo().performClick()
        compose.waitUntil(5_000) { repository.currentSession.value == null }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("New accounts require an invitation from TNHC.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("New accounts require an invitation from TNHC.").assertIsDisplayed()
    }

    @Test fun signInPasswordCanBeShownAndHidden() {
        val repository = FakeCommunityRepository()
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Profile").performClick()

        compose.onNodeWithTag("account-password-visibility-toggle")
            .assertContentDescriptionEquals("Show password")
            .performClick()
            .assertContentDescriptionEquals("Hide password")
        compose.onNodeWithTag("account-password-visibility-toggle").performClick()
            .assertContentDescriptionEquals("Show password")
    }

    @Test fun passwordUpdateCanBeShownAndHidden() {
        val repository = FakeCommunityRepository().apply {
            currentSession.value = CommunitySession("member-1")
        }
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Profile").performClick()
        compose.waitUntil(5_000) { repository.profileLoads > 0 }

        compose.onNodeWithTag("account-new-password-visibility-toggle").performScrollTo()
            .assertContentDescriptionEquals("Show password")
            .performClick()
            .assertContentDescriptionEquals("Hide password")
        compose.onNodeWithTag("account-new-password-visibility-toggle").performClick()
            .assertContentDescriptionEquals("Show password")
    }

    @Test fun projectFollowUpdatesAfterRepositoryAcceptsRequest() {
        val repository = FakeCommunityRepository().apply {
            currentSession.value = CommunitySession("member-1")
        }
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Projects").performClick()
        compose.onNodeWithTag("project-demo-orbit").performClick()
        compose.onNodeWithTag("project-detail-list").performScrollToNode(hasText("Help shape what comes next"))
        compose.onNodeWithTag("project-follow").assertTextEquals("Follow project")
        compose.onNodeWithTag("project-follow").performClick()
        compose.onNodeWithTag("project-follow").assertTextEquals("Following · tap to unfollow")
        compose.onNodeWithTag("project-follow").performClick()
        compose.onNodeWithTag("project-follow").assertTextEquals("Follow project")
        assert(repository.followChanges == listOf("demo-orbit" to true, "demo-orbit" to false))
    }

    @Test fun catalogueReloadsAndDiscardsPreviousAccountProjectsOnIdentityChange() {
        val repository = FakeCommunityRepository()
        val firstMemberPage = CompletableDeferred<ProjectPage>()
        val secondMemberPage = CompletableDeferred<ProjectPage>()
        val visitorPage = CompletableDeferred<ProjectPage>()
        val project = DemoProjectRepository.load().first().copy(isDemo = false)
        repository.projectLoader = {
            when (repository.currentSession.value?.userId) {
                "member-1" -> firstMemberPage.await()
                "member-2" -> secondMemberPage.await()
                else -> visitorPage.await()
            }
        }
        visitorPage.complete(ProjectPage(listOf(project.copy(id = "visitor-project")), null))
        compose.setContent { TnhcTheme { CommunityApp(communityRepository = repository) } }
        compose.onNodeWithTag("tab-Projects").performClick()
        compose.onNodeWithTag("project-visitor-project").assertExists()

        compose.runOnIdle { repository.currentSession.value = CommunitySession("member-1") }
        compose.onNodeWithTag("project-visitor-project").assertDoesNotExist()
        compose.onNodeWithText("Loading projects").assertExists()
        compose.runOnIdle { firstMemberPage.complete(ProjectPage(listOf(project.copy(id = "first-private")), null)) }
        compose.onNodeWithTag("project-first-private").assertExists()

        compose.runOnIdle { repository.currentSession.value = CommunitySession("member-2") }
        compose.onNodeWithTag("project-first-private").assertDoesNotExist()
        compose.onNodeWithText("Loading projects").assertExists()
        compose.runOnIdle { secondMemberPage.complete(ProjectPage(listOf(project.copy(id = "second-private")), null)) }
        compose.onNodeWithTag("project-second-private").assertExists()

        compose.runOnIdle { repository.currentSession.value = null }
        compose.onNodeWithTag("project-second-private").assertDoesNotExist()
        compose.onNodeWithTag("project-visitor-project").assertExists()
    }

    @Test fun offlineSignOutClearsRealSdkAndEncryptedStoredSession() = verifySignOutCleanup(cancelRequest = false)

    @Test fun cancelledSignOutStillCompletesSuspendingSessionCleanup() = verifySignOutCleanup(cancelRequest = true)

    private fun verifySignOutCleanup(cancelRequest: Boolean) = runBlocking {
        val isolatedContext = object : android.content.ContextWrapper(compose.activity) {
            override fun getApplicationContext(): android.content.Context = this
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences =
                super.getSharedPreferences("$name-logout-regression", mode)
        }
        val storage = EncryptedAndroidSessionManager(isolatedContext)
        val client = createSupabaseClient("http://127.0.0.1:1", "synthetic-public-key") {
            install(Auth) {
                autoLoadFromStorage = false
                alwaysAutoRefresh = false
                sessionManager = object : SessionManager by storage {
                    override suspend fun deleteSession() {
                        delay(1) // Cleanup must complete even when the caller has been cancelled.
                        storage.deleteSession()
                    }
                }
            }
        }
        try {
            client.auth.importSession(UserSession("synthetic-access-token", "synthetic-refresh-token", expiresIn = 3600, tokenType = "bearer"), autoRefresh = false)
            assertNotNull(storage.loadSession())
            val remote = SupabaseCommunityRemoteDataSource(client)
            if (cancelRequest) {
                val job = launch(start = CoroutineStart.UNDISPATCHED) {
                    currentCoroutineContext().cancel()
                    remote.signOut()
                }
                job.join()
                assertTrue(job.isCancelled)
            } else {
                val failure = runCatching { remote.signOut() }.exceptionOrNull()
                assertNotNull("The unreachable Auth endpoint must exercise the failure path", failure)
            }
            assertNull(client.auth.currentSessionOrNull())
            assertTrue(runCatching { storage.loadSession() }.exceptionOrNull() is NoSessionFoundException)
        } finally {
            storage.deleteSession()
            client.close()
        }
    }

    private class FakeCommunityRepository : CommunityRepository {
        val currentSession = MutableStateFlow<CommunitySession?>(null)
        override val session: StateFlow<CommunitySession?> = currentSession
        var followChanges = mutableListOf<Pair<String, Boolean>>()
        private val followedProjects = mutableSetOf<String>()
        var savedDisplayName: String? = null
        var savedInterests: List<String> = emptyList()
        var passwordUpdates = 0
        var profileLoads = 0
        var topicLoads = 0
        var topicLoadFailure = false
        var topicCatalogue = listOf(CommunityTopic("start-here", "start-here", "Start Here", "Introductions and community news", "public"))
        val membershipChanges = mutableListOf<Pair<String, Boolean>>()
        val createdPosts = mutableListOf<String>()
        val joinedTopics = mutableSetOf<String>()
        val topicPosts = mutableListOf(
            CommunityPost("welcome", "founder-1", "start-here", "Welcome to the TNHC community.", "2026-10-01T12:00:00Z"),
        )
        var topicPostLoadFailure = false
        var postCreateFailure = false
        var activeMember = true
        var founderRole = false
        var founderRoleFailure = false
        var founderRoleChecks = 0
        override suspend fun isFounder(): Boolean {
            founderRoleChecks++
            if (founderRoleFailure) error("Role lookup unavailable")
            return founderRole
        }
        override suspend fun isActiveMember() = activeMember
        var projectLoader: (suspend () -> ProjectPage)? = null
        private val project = DemoProjectRepository.load().first { it.id == "demo-orbit" }.copy(isDemo = false)
        override fun handleAuthLink(intent: android.content.Intent) = Unit
        override suspend fun signIn(email: String, password: String) {
            currentSession.value = CommunitySession("member-1")
        }
        override suspend fun signOut() { currentSession.value = null }
        override suspend fun setPassword(password: String) { passwordUpdates++ }
        override suspend fun loadProjects(cursor: String?, filter: ProjectFilter) = projectLoader?.invoke() ?: ProjectPage(listOf(project), null)
        override suspend fun loadProfile(): MemberProfile {
            profileLoads++
            return MemberProfile("member-1", "pilotmember", "Pilot Member", "", listOf("Games"), "members")
        }
        override suspend fun loadFollowedProjectIds() = followedProjects.toSet()
        override suspend fun updateProfile(displayName: String?, bio: String?, interests: List<String>, visibility: String) {
            savedDisplayName = displayName
            savedInterests = interests
        }
        override suspend fun setProjectFollow(projectId: String, followed: Boolean) {
            followChanges += projectId to followed
            if (followed) followedProjects += projectId else followedProjects -= projectId
        }
        override suspend fun loadTopics(): List<CommunityTopic> {
            topicLoads++
            if (topicLoadFailure) error("Synthetic community lookup failure")
            return topicCatalogue
        }
        override suspend fun loadJoinedTopicIds() = joinedTopics.toSet()
        override suspend fun setTopicMembership(topicId: String, joined: Boolean) {
            membershipChanges += topicId to joined
            if (joined) joinedTopics += topicId else joinedTopics -= topicId
        }
        override suspend fun loadTopicPosts(topicId: String): List<CommunityPost> {
            if (topicPostLoadFailure) error("Synthetic post lookup failure")
            return topicPosts.filter { it.topicId == topicId }.toList()
        }
        override suspend fun createTopicPost(topicId: String, body: String) {
            if (postCreateFailure) error("Synthetic post creation failure")
            createdPosts += body
            topicPosts += CommunityPost("post-${topicPosts.size}", "member-1", topicId, body, "2026-10-06T12:00:00Z")
        }
    }

}
