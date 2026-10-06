package com.tnhc.community.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CommunityRepositoryTest {
    @Test
    fun founderRoleLookupIsDelegatedToTheAuthenticatedRemote() = runBlocking {
        val remote = FakeCommunityRemote().apply { founder = true }
        val repository = DefaultCommunityRepository(remote)

        assertEquals(true, repository.isFounder())
        remote.founder = false
        assertEquals(false, repository.isFounder())
    }

    @Test
    fun postingAccessIsCheckedByTheRemoteAndFailsClosedWithoutASession() = runBlocking {
        val remote = FakeCommunityRemote().apply {
            sessions.value = CommunitySession("member-id")
            activeMember = true
        }
        val repository = DefaultCommunityRepository(remote)

        assertEquals(true, repository.isActiveMember())
        remote.activeMember = false
        assertEquals(false, repository.isActiveMember())
        remote.sessions.value = null
        assertEquals(false, repository.isActiveMember())
    }

    @Test
    fun signInPublishesSessionAndSignOutClearsIt() = runBlocking {
        val remote = FakeCommunityRemote()
        val repository = DefaultCommunityRepository(remote)

        repository.signIn("member@example.test", "correct horse battery staple")
        assertEquals(CommunitySession("member-id"), repository.session.value)
        repository.signOut()
        assertNull(repository.session.value)
    }

    @Test
    fun projectPagePreservesOpaqueCursorAndFilters() = runBlocking {
        val remote = FakeCommunityRemote()
        val repository = DefaultCommunityRepository(remote)
        val filter = ProjectFilter(stage = "prototype", tag = "game")
        val page = repository.loadProjects("server-issued-cursor", filter)

        assertEquals("server-issued-cursor", remote.cursor)
        assertEquals(filter, remote.filter)
        assertEquals("next-cursor", page.nextCursor)
    }

    @Test fun fullProjectCatalogueFollowsEveryServerCursor() = runBlocking {
        val first = Project("a", "Nexus", "Chat", "Core", "Released", listOf("nexus"), "Chat", "Official", false)
        val second = Project("b", "DevTrack", "Local-first", "Developer Tools", "Released", listOf("devtrack"), "Planning", "Community", false)
        val remote = FakeCommunityRemote().apply {
            pages = mapOf(null to ProjectPage(listOf(first), "page-2"), "page-2" to ProjectPage(listOf(second), null))
        }

        assertEquals(listOf(first, second), DefaultCommunityRepository(remote).loadAllProjects())
        assertEquals(listOf(null, "page-2"), remote.requestedCursors)
    }

    @Test
    fun followAndUnfollowArePersistentRemoteOperations() = runBlocking {
        val remote = FakeCommunityRemote()
        val repository = DefaultCommunityRepository(remote)

        repository.setProjectFollow("project-id", true)
        repository.setProjectFollow("project-id", false)

        assertEquals(listOf("project-id" to true, "project-id" to false), remote.followChanges)
    }

    @Test
    fun followedProjectIdsAreLoadedAsASet() = runBlocking {
        val remote = FakeCommunityRemote().apply { followedIds = setOf("project-a", "project-b") }
        val repository = DefaultCommunityRepository(remote)

        assertEquals(setOf("project-a", "project-b"), repository.loadFollowedProjectIds())
    }

    @Test
    fun passwordChangesAreDelegatedWithoutTrimmingCredentials() = runBlocking {
        val remote = FakeCommunityRemote()
        val repository = DefaultCommunityRepository(remote)

        repository.setPassword("  user-chosen-passphrase  ")

        assertEquals("  user-chosen-passphrase  ", remote.updatedPassword)
    }

    @Test
    fun passwordChangeRejectsValuesBelowBackendMinimum() = runBlocking {
        val remote = FakeCommunityRemote()
        val repository = DefaultCommunityRepository(remote)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.setPassword("too-short") }
        }
        assertNull(remote.updatedPassword)
    }

    @Test
    fun profileInputIsNormalizedBeforeSending() = runBlocking {
        val remote = FakeCommunityRemote()
        val repository = DefaultCommunityRepository(remote)

        repository.updateProfile("  Ada  ", "  Making things  ", listOf("Art", " Art ", "", "Testing"), "members")

        assertEquals(ProfileUpdate("Ada", "Making things", listOf("Art", "Testing"), "members"), remote.profileUpdate)
    }

    @Test
    fun expiredSessionIsReflectedByRepositoryState() = runBlocking {
        val remote = FakeCommunityRemote()
        val repository = DefaultCommunityRepository(remote)
        remote.sessions.value = CommunitySession("member-id")
        remote.sessions.value = null

        assertNull(repository.session.value)
    }

    @Test
    fun remoteNetworkFailureIsReturnedToCallerForRetryUi() = runBlocking {
        val remote = FakeCommunityRemote().apply { failure = IllegalStateException("offline") }
        val repository = DefaultCommunityRepository(remote)

        val error = assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.loadProfile() }
        }
        assertEquals("offline", error.message)
    }

    @Test fun topicMembershipAndPostWritesRequireAnAuthenticatedSession() = runBlocking {
        val remote = FakeCommunityRemote()
        val repository = DefaultCommunityRepository(remote)

        assertThrows(IllegalStateException::class.java) { runBlocking { repository.setTopicMembership("topic-1", true) } }
        assertThrows(IllegalStateException::class.java) { runBlocking { repository.createTopicPost("topic-1", "Hello") } }
        assertEquals(emptyList<Pair<String, Boolean>>(), remote.topicMembershipChanges)
        assertNull(remote.createdTopicPost)
    }

    @Test fun topicPostBodyIsTrimmedAndCannotExceedTheDatabaseLimit() = runBlocking {
        val remote = FakeCommunityRemote().apply { sessions.value = CommunitySession("member-id") }
        val repository = DefaultCommunityRepository(remote)

        repository.createTopicPost("topic-1", "  A useful update  ")
        assertEquals(TopicPostInput("topic-1", "A useful update"), remote.createdTopicPost)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.createTopicPost("topic-1", " ") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.createTopicPost("topic-1", "x".repeat(5001)) }
        }
        assertEquals(TopicPostInput("topic-1", "A useful update"), remote.createdTopicPost)
    }

    @Test fun publicTopicsAndRecentTopicPostsArePassedThrough() = runBlocking {
        val topic = CommunityTopic("topic-1", "project-releases", "Project Releases", "Real updates", "public")
        val post = CommunityPost("post-1", "author-1", "topic-1", "A release", "2026-10-05T10:00:00Z")
        val remote = FakeCommunityRemote().apply {
            topics = listOf(topic)
            topicPosts = listOf(post)
        }
        val repository = DefaultCommunityRepository(remote)

        assertEquals(listOf(topic), repository.loadTopics())
        assertEquals(listOf(post), repository.loadTopicPosts("topic-1"))
    }

    private class FakeCommunityRemote : CommunityRemoteDataSource {
        override val sessions = MutableStateFlow<CommunitySession?>(null)
        override fun handleAuthLink(intent: android.content.Intent) = Unit
        var cursor: String? = null
        var filter: ProjectFilter? = null
        val followChanges = mutableListOf<Pair<String, Boolean>>()
        var followedIds: Set<String> = emptySet()
        var updatedPassword: String? = null
        var profileUpdate: ProfileUpdate? = null
        var failure: RuntimeException? = null
        var founder = false
        var activeMember = false
        var pages: Map<String?, ProjectPage> = emptyMap()
        val requestedCursors = mutableListOf<String?>()
        var topics: List<CommunityTopic> = emptyList()
        var topicPosts: List<CommunityPost> = emptyList()
        val topicMembershipChanges = mutableListOf<Pair<String, Boolean>>()
        var createdTopicPost: TopicPostInput? = null

        override suspend fun isFounder() = founder
        override suspend fun isActiveMember() = activeMember

        override suspend fun signIn(email: String, password: String) {
            failure?.let { throw it }
            sessions.value = CommunitySession("member-id")
        }

        override suspend fun signOut() {
            sessions.value = null
        }

        override suspend fun setPassword(password: String) {
            updatedPassword = password
        }

        override suspend fun loadProjects(cursor: String?, filter: ProjectFilter): ProjectPage {
            failure?.let { throw it }
            requestedCursors += cursor
            this.cursor = cursor
            this.filter = filter
            return pages[cursor] ?: ProjectPage(emptyList(), "next-cursor")
        }

        override suspend fun loadProfile(): MemberProfile {
            failure?.let { throw it }
            return MemberProfile("member-id", "member-handle", "Member", null, emptyList(), "members")
        }

        override suspend fun loadFollowedProjectIds(): Set<String> = followedIds

        override suspend fun updateProfile(displayName: String?, bio: String?, interests: List<String>, visibility: String) {
            failure?.let { throw it }
            profileUpdate = ProfileUpdate(displayName, bio, interests, visibility)
        }

        override suspend fun setProjectFollow(projectId: String, followed: Boolean) {
            failure?.let { throw it }
            followChanges += projectId to followed
        }
        override suspend fun loadTopics() = topics
        override suspend fun loadJoinedTopicIds() = emptySet<String>()
        override suspend fun setTopicMembership(topicId: String, joined: Boolean) { topicMembershipChanges += topicId to joined }
        override suspend fun loadTopicPosts(topicId: String) = topicPosts
        override suspend fun createTopicPost(topicId: String, body: String) { createdTopicPost = TopicPostInput(topicId, body) }
    }

    private data class ProfileUpdate(val displayName: String?, val bio: String?, val interests: List<String>, val visibility: String)
    private data class TopicPostInput(val topicId: String, val body: String)
}
