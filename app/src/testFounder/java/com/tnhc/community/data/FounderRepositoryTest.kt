package com.tnhc.community.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FounderRepositoryTest {
    @Test fun memberDirectoryPreservesCursorAndBoundsPageSize() = runBlocking {
        val remote = FakeFounderRemote()
        val repository = DefaultFounderRepository(remote)

        repository.loadMembers(cursor = "opaque-cursor", pageSize = 90)

        assertEquals("opaque-cursor", remote.memberCursor)
        assertEquals(50, remote.memberLimit)
    }

    @Test fun memberStateChangesAreDelegated() = runBlocking {
        val remote = FakeFounderRemote()
        DefaultFounderRepository(remote).setMemberState("member-id", "suspended")

        assertEquals("member-id" to "suspended", remote.memberStateChange)
    }

    @Test fun platformRoleChangesAreDelegated() = runBlocking {
        val remote = FakeFounderRemote()
        DefaultFounderRepository(remote).setPlatformRole("member-id", "moderator", true)

        assertEquals(Triple("member-id", "moderator", true), remote.platformRoleChange)
    }

    @Test fun invitationEmailIsNormalized() = runBlocking {
        val remote = FakeFounderRemote()
        DefaultFounderRepository(remote).inviteMember("  New.Member@Example.Test ")

        assertEquals("new.member@example.test", remote.invitedEmail)
    }

    @Test fun invalidMemberStateAndRoleAreRejectedBeforeRequests() {
        val remote = FakeFounderRemote()
        val repository = DefaultFounderRepository(remote)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.setMemberState("member-id", "pending") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.setPlatformRole("member-id", "founder", true) }
        }
        assertEquals(null, remote.memberStateChange)
        assertEquals(null, remote.platformRoleChange)
    }

    @Test fun applicationSettingsAcceptOnlyTypedValues() {
        runBlocking {
            val remote = FakeFounderRemote()
            val repository = DefaultFounderRepository(remote)
            val flags = buildJsonObject { put("community_feed", JsonPrimitive(true)) }
            repository.setAppSetting("feature_flags", flags)

            assertEquals("feature_flags" to flags, remote.settingChange)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repository.setAppSetting("maintenance_notice", JsonPrimitive(42)) }
            }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repository.setAppSetting("arbitrary", JsonPrimitive("value")) }
            }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repository.setAppSetting("feature_flags", buildJsonObject { put("messaging", JsonPrimitive("yes")) }) }
            }
        }
    }

    @Test fun auditHistoryUsesTheRemoteCursor() = runBlocking {
        val remote = FakeFounderRemote()
        DefaultFounderRepository(remote).loadAudit("audit-cursor", 7)

        assertEquals("audit-cursor", remote.auditCursor)
        assertEquals(7, remote.auditLimit)
    }

    @Test fun catalogAndModerationInputsAreValidatedBeforeRequests() {
        val remote = FakeFounderRemote()
        val repository = DefaultFounderRepository(remote)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.upsertProject(FounderProjectDraft(null, "Bad Slug", "community", "Title", "", "idea", emptyList(), "public")) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.setContentModeration("message", "content-id", "hidden") }
        }
        assertEquals(null, remote.projectDraft)
        assertEquals(null, remote.moderationChange)
    }

    @Test fun inDevelopmentIsAnAcceptedProjectStage() = runBlocking {
        val remote = FakeFounderRemote()
        val draft = FounderProjectDraft(null, "nexus-sample", "official", "Nexus Sample", "A real project", "in_development", listOf("nexus"), "public")

        DefaultFounderRepository(remote).upsertProject(draft)

        assertEquals(draft, remote.projectDraft)
    }

    @Test fun projectLinksMustUseHttpsBeforeSaving() {
        val remote = FakeFounderRemote()
        val project = FounderProjectDraft(null, "nexus-sample", "official", "Nexus Sample", "A real project", "in_development",
            listOf("nexus"), "public", "http://example.com", "https://github.com/example/project")

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { DefaultFounderRepository(remote).upsertProject(project) }
        }
        assertEquals(null, remote.projectDraft)
    }

    @Test fun validProjectLinksAreSavedWithTheDraft() = runBlocking {
        val remote = FakeFounderRemote()
        val draft = FounderProjectDraft(null, "nexus-sample", "official", "Nexus Sample", "A real project", "in_development",
            listOf("nexus"), "public", "https://tnhc.dev/projects/nexus-sample", "https://github.com/The-No-Hands-company/Nexus-Systems")

        DefaultFounderRepository(remote).upsertProject(draft)

        assertEquals(draft, remote.projectDraft)
    }

    private class FakeFounderRemote : FounderRemoteDataSource {
        var memberCursor: String? = null
        var memberLimit: Int? = null
        var auditCursor: String? = null
        var auditLimit: Int? = null
        var memberStateChange: Pair<String, String>? = null
        var platformRoleChange: Triple<String, String, Boolean>? = null
        var invitedEmail: String? = null
        var settingChange: Pair<String, JsonElement>? = null
        var projectDraft: FounderProjectDraft? = null
        var moderationChange: Triple<String, String, String>? = null

        override suspend fun loadMembers(cursor: String?, limit: Int): FounderMemberPage {
            memberCursor = cursor
            memberLimit = limit
            return FounderMemberPage(emptyList(), null)
        }
        override suspend fun loadAudit(cursor: String?, limit: Int): FounderAuditPage {
            auditCursor = cursor
            auditLimit = limit
            return FounderAuditPage(emptyList(), null)
        }
        override suspend fun loadSettings(): List<FounderAppSetting> = emptyList()
        override suspend fun setMemberState(userId: String, state: String) { memberStateChange = userId to state }
        override suspend fun setPlatformRole(userId: String, role: String, enabled: Boolean) { platformRoleChange = Triple(userId, role, enabled) }
        override suspend fun inviteMember(email: String) { invitedEmail = email }
        override suspend fun setAppSetting(key: String, value: JsonElement) { settingChange = key to value }
        override suspend fun loadProjects(cursor: String?) = FounderProjectPage(emptyList(), null)
        override suspend fun loadTopics(cursor: String?) = FounderTopicPage(emptyList(), null)
        override suspend fun upsertProject(project: FounderProjectDraft) { projectDraft = project }
        override suspend fun setProjectMembership(projectId: String, userId: String, role: String?) = Unit
        override suspend fun upsertTopic(topic: FounderTopicDraft) = Unit
        override suspend fun setTopicMembership(topicId: String, userId: String, enabled: Boolean) = Unit
        override suspend fun loadContent(state: String, cursor: String?, limit: Int) = FounderContentPage(emptyList(), null)
        override suspend fun setContentModeration(contentType: String, contentId: String, state: String) { moderationChange = Triple(contentType, contentId, state) }
    }
}
