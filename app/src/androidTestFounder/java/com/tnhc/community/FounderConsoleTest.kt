package com.tnhc.community

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tnhc.community.data.FounderAppSetting
import com.tnhc.community.data.FounderAuditPage
import com.tnhc.community.data.FounderContentPage
import com.tnhc.community.data.FounderMemberPage
import com.tnhc.community.data.FounderProjectDraft
import com.tnhc.community.data.FounderProjectPage
import com.tnhc.community.data.FounderRepository
import com.tnhc.community.data.FounderTopic
import com.tnhc.community.data.FounderTopicPage
import com.tnhc.community.data.FounderTopicDraft
import com.tnhc.community.ui.FounderConsoleScreen
import com.tnhc.community.ui.TnhcTheme
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FounderConsoleTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun maintenanceNoticeSaveIsReadBackFromTheRepository() {
        val repository = FakeFounderRepository()
        compose.setContent { TnhcTheme { FounderConsoleScreen(repository) } }
        compose.onNodeWithText("Founder controls").assertIsDisplayed()
        compose.onNodeWithTag("founder-section-Settings").performScrollTo().performClick()
        compose.onNodeWithTag("founder-maintenance-notice").performTextInput("Planned maintenance tonight")
        compose.onNodeWithTag("founder-save-maintenance").performClick()
        compose.onNodeWithTag("founder-success").assertTextContains("Maintenance notice saved and read back.")
        assertEquals("maintenance_notice", repository.settingKey)
        assertEquals("Planned maintenance tonight", repository.savedNotice)
        assertEquals(2, repository.settingsLoads)
    }

    @Test fun localInviteExplainsThatMailpitCapturesInsteadOfSendingEmail() {
        val repository = FakeFounderRepository()
        compose.setContent { TnhcTheme { FounderConsoleScreen(repository, localEmailCapture = true) } }
        compose.onNodeWithTag("founder-invite-email").performTextInput("member@example.test")
        compose.onNodeWithTag("founder-invite").performClick()
        compose.onNodeWithTag("founder-success").assertTextEquals(
            "Invitation created. Its email is in local Mailpit and was not sent externally.",
        )
        assertEquals("member@example.test", repository.invitedEmail)
    }
}

private class FakeFounderRepository : FounderRepository {
    var settingKey: String? = null
    var settingsLoads = 0
    var savedNotice = ""
    var invitedEmail: String? = null
    override suspend fun loadMembers(cursor: String?, pageSize: Int) = FounderMemberPage(emptyList(), null)
    override suspend fun loadAudit(cursor: String?, pageSize: Int) = FounderAuditPage(emptyList(), null)
    override suspend fun loadSettings(): List<FounderAppSetting> {
        settingsLoads++
        return listOf(FounderAppSetting("maintenance_notice", JsonPrimitive(savedNotice), "founder", "now"))
    }
    override suspend fun setMemberState(userId: String, state: String) = Unit
    override suspend fun setPlatformRole(userId: String, role: String, enabled: Boolean) = Unit
    override suspend fun inviteMember(email: String) { invitedEmail = email }
    override suspend fun setAppSetting(key: String, value: JsonElement) { settingKey = key; savedNotice = (value as JsonPrimitive).content }
    override suspend fun loadProjects(cursor: String?) = FounderProjectPage(emptyList(), null)
    override suspend fun loadTopics(cursor: String?) = FounderTopicPage(emptyList(), null)
    override suspend fun upsertProject(project: FounderProjectDraft) = Unit
    override suspend fun setProjectMembership(projectId: String, userId: String, role: String?) = Unit
    override suspend fun upsertTopic(topic: FounderTopicDraft) = Unit
    override suspend fun setTopicMembership(topicId: String, userId: String, enabled: Boolean) = Unit
    override suspend fun loadContent(state: String, cursor: String?) = FounderContentPage(emptyList(), null)
    override suspend fun setContentModeration(contentType: String, contentId: String, state: String) = Unit
}
