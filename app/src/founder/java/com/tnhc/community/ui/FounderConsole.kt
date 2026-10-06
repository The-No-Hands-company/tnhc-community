package com.tnhc.community.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tnhc.community.data.FounderAppSetting
import com.tnhc.community.data.FounderAuditEntry
import com.tnhc.community.data.FounderMember
import com.tnhc.community.data.FounderRepository
import com.tnhc.community.data.FounderProject
import com.tnhc.community.data.FounderProjectDraft
import com.tnhc.community.data.FounderTopic
import com.tnhc.community.data.FounderTopicDraft
import com.tnhc.community.data.FounderContentItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json

private enum class FounderSection(val title: String) {
    Members("Members"), Projects("Projects"), Topics("Topics"), Moderation("Moderation"), Settings("App settings"), Audit("Audit history"),
}

@Composable
fun FounderConsoleScreen(repository: FounderRepository, localEmailCapture: Boolean = false) {
    var section by remember { mutableStateOf(FounderSection.Members) }
    var refresh by remember { mutableStateOf(0) }
    var members by remember { mutableStateOf<List<FounderMember>>(emptyList()) }
    var audit by remember { mutableStateOf<List<FounderAuditEntry>>(emptyList()) }
    var auditNextCursor by remember { mutableStateOf<String?>(null) }
    var appendAuditPage by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf<List<FounderAppSetting>>(emptyList()) }
    var projects by remember { mutableStateOf<List<FounderProject>>(emptyList()) }
    var topics by remember { mutableStateOf<List<FounderTopic>>(emptyList()) }
    var contentItems by remember { mutableStateOf<List<FounderContentItem>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var contentNextCursor by remember { mutableStateOf<String?>(null) }
    var topicsNextCursor by remember { mutableStateOf<String?>(null) }
    var projectsNextCursor by remember { mutableStateOf<String?>(null) }
    var appendPage by remember { mutableStateOf(false) }
    var appendContentPage by remember { mutableStateOf(false) }
    var appendTopicsPage by remember { mutableStateOf(false) }
    var appendProjectsPage by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var inviteEmail by remember { mutableStateOf("") }
    var maintenanceNotice by remember { mutableStateOf("") }
    var featureFlagsText by remember { mutableStateOf("{}") }
    var busyAction by remember { mutableStateOf(false) }
    var memberPendingConfirmation by remember { mutableStateOf<Pair<FounderMember, String>?>(null) }
    var contentPendingConfirmation by remember { mutableStateOf<Pair<FounderContentItem, String>?>(null) }
    var projectId by remember { mutableStateOf<String?>(null) }
    var projectSlug by remember { mutableStateOf("") }
    var projectTitle by remember { mutableStateOf("") }
    var projectSummary by remember { mutableStateOf("") }
    var projectOwner by remember { mutableStateOf("community") }
    var projectStage by remember { mutableStateOf("idea") }
    var projectVisibility by remember { mutableStateOf("public") }
    var projectTags by remember { mutableStateOf("") }
    var projectWebsiteUrl by remember { mutableStateOf("") }
    var projectRepositoryUrl by remember { mutableStateOf("") }
    var topicId by remember { mutableStateOf<String?>(null) }
    var topicSlug by remember { mutableStateOf("") }
    var topicTitle by remember { mutableStateOf("") }
    var topicDescription by remember { mutableStateOf("") }
    var topicVisibility by remember { mutableStateOf("public") }
    var projectMemberId by remember { mutableStateOf("") }
    var projectMemberRole by remember { mutableStateOf("contributor") }
    var moderationState by remember { mutableStateOf("hidden") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, section, refresh) {
        loading = true
        error = null
        try {
            when (section) {
                FounderSection.Members -> {
                    val page = repository.loadMembers(if (appendPage) nextCursor else null)
                    members = if (appendPage) members + page.members else page.members
                    appendPage = false
                    nextCursor = page.nextCursor
                }
                FounderSection.Settings -> {
                    settings = repository.loadSettings()
                    maintenanceNotice = settings.firstOrNull { it.key == "maintenance_notice" }
                        ?.value?.let { (it as? JsonPrimitive)?.content }.orEmpty()
                    featureFlagsText = settings.firstOrNull { it.key == "feature_flags" }?.value?.toString() ?: "{}"
                }
                FounderSection.Audit -> {
                    val page = repository.loadAudit(if (appendAuditPage) auditNextCursor else null)
                    audit = if (appendAuditPage) audit + page.entries else page.entries
                    auditNextCursor = page.nextCursor
                    appendAuditPage = false
                }
                FounderSection.Projects -> {
                    val page = repository.loadProjects(if (appendProjectsPage) projectsNextCursor else null)
                    projects = if (appendProjectsPage) projects + page.projects else page.projects
                    projectsNextCursor = page.nextCursor
                    appendProjectsPage = false
                }
                FounderSection.Topics -> {
                    val page = repository.loadTopics(if (appendTopicsPage) topicsNextCursor else null)
                    topics = if (appendTopicsPage) topics + page.topics else page.topics
                    topicsNextCursor = page.nextCursor
                    appendTopicsPage = false
                }
                FounderSection.Moderation -> {
                    val page = repository.loadContent(moderationState, if (appendContentPage) contentNextCursor else null)
                    contentItems = if (appendContentPage) contentItems + page.items else page.items
                    contentNextCursor = page.nextCursor
                    appendContentPage = false
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "Could not load this section. Check your connection and retry."
        } finally {
            loading = false
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Founder controls", style = MaterialTheme.typography.headlineMedium)
        Text("Changes are checked by the server and recorded in the audit history.", style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FounderSection.entries.forEach { item ->
                TextButton(onClick = {
                    section = item; refresh = 0; nextCursor = null; notice = null
                    appendPage = false; appendAuditPage = false; appendContentPage = false
                    appendProjectsPage = false; appendTopicsPage = false
                },
                    modifier = Modifier.testTag("founder-section-${item.name}")) { Text(item.title) }
            }
        }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("founder-success")) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("founder-error")) }
        if (loading) Text("Loading…")
        when (section) {
            FounderSection.Members -> {
                OutlinedTextField(inviteEmail, { inviteEmail = it }, label = { Text("Invite by email") }, modifier = Modifier.fillMaxWidth().testTag("founder-invite-email"))
                if (localEmailCapture) {
                    Text("Local development captures invitation email in Mailpit at 127.0.0.1:54324; it is not sent to the recipient.", style = MaterialTheme.typography.bodySmall)
                }
                Button(enabled = !busyAction, onClick = {
                    busyAction = true; error = null; notice = null
                    scope.launch {
                        try {
                            repository.inviteMember(inviteEmail)
                            inviteEmail = ""
                            notice = if (localEmailCapture) {
                                "Invitation created. Its email is in local Mailpit and was not sent externally."
                            } else {
                                "Invitation accepted by the configured email provider."
                            }
                            refresh++
                        }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Invitation failed." }
                        finally { busyAction = false }
                    }
                }, modifier = Modifier.testTag("founder-invite")) { Text("Send invitation") }
                members.forEach { member ->
                    Card(Modifier.fillMaxWidth().testTag("founder-member-${member.id}")) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(member.displayName?.takeIf(String::isNotBlank) ?: "@${member.handle}", style = MaterialTheme.typography.titleMedium)
                            Text("@${member.handle} · ${member.accountState} · ${member.roles.joinToString().ifBlank { "member" }}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val nextState = if (member.accountState == "suspended") "active" else "suspended"
                                OutlinedButton(enabled = !busyAction, onClick = {
                                    if (nextState == "suspended") memberPendingConfirmation = member to nextState
                                    else scope.launch {
                                        busyAction = true; error = null; notice = null
                                        try { repository.setMemberState(member.id, nextState); notice = "Member state updated."; refresh++ }
                                        catch (cancelled: CancellationException) { throw cancelled }
                                        catch (_: Exception) { error = "Change failed." }
                                        finally { busyAction = false }
                                    }
                                }) { Text(if (member.accountState == "suspended") "Reactivate" else "Suspend") }
                                val isModerator = "moderator" in member.roles
                                TextButton(enabled = !busyAction, onClick = {
                                    busyAction = true; error = null; notice = null
                                    scope.launch {
                                        try { repository.setPlatformRole(member.id, "moderator", !isModerator); notice = "Moderator role updated."; refresh++ }
                                        catch (cancelled: CancellationException) { throw cancelled }
                                        catch (_: Exception) { error = "Change failed." }
                                        finally { busyAction = false }
                                    }
                                }) { Text(if (isModerator) "Remove moderator" else "Make moderator") }
                                val isAdministrator = "administrator" in member.roles
                                TextButton(enabled = !busyAction, onClick = {
                                    busyAction = true; error = null; notice = null
                                    scope.launch {
                                        try { repository.setPlatformRole(member.id, "administrator", !isAdministrator); notice = "Administrator role updated."; refresh++ }
                                        catch (cancelled: CancellationException) { throw cancelled }
                                        catch (_: Exception) { error = "Change failed." }
                                        finally { busyAction = false }
                                    }
                                }) { Text(if (isAdministrator) "Remove administrator" else "Make administrator") }
                            }
                        }
                    }
                }
                if (members.isEmpty() && !loading && error == null) Text("No members found.")
                if (nextCursor != null) OutlinedButton(onClick = { appendPage = true; refresh++ }, modifier = Modifier.testTag("founder-load-more")) { Text("Load more members") }
                Text("Use the server dashboard for recovery of the sole Founder account.", style = MaterialTheme.typography.bodySmall)
            }
            FounderSection.Settings -> {
                Text("Maintenance notice", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(maintenanceNotice, { maintenanceNotice = it }, label = { Text("Shown to community members") },
                    supportingText = { Text("Up to 240 characters") }, modifier = Modifier.fillMaxWidth().testTag("founder-maintenance-notice"))
                Button(enabled = !busyAction && maintenanceNotice.length <= 240, onClick = {
                    busyAction = true; error = null; notice = null
                    scope.launch {
                        try {
                            repository.setAppSetting("maintenance_notice", JsonPrimitive(maintenanceNotice))
                            settings = repository.loadSettings(); notice = "Maintenance notice saved and read back."
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Save failed." }
                        finally { busyAction = false }
                    }
                }, modifier = Modifier.testTag("founder-save-maintenance")) { Text("Save notice") }
                val featureFlags = settings.firstOrNull { it.key == "feature_flags" }
                OutlinedTextField(featureFlagsText, { featureFlagsText = it }, label = { Text("Feature flags (JSON booleans)") },
                    supportingText = { Text("Example: {\"community_feed\":true,\"messages\":false}") },
                    modifier = Modifier.fillMaxWidth().testTag("founder-feature-flags"))
                Button(enabled = !busyAction, onClick = {
                    busyAction = true; error = null; notice = null
                    scope.launch {
                        try {
                            repository.setAppSetting("feature_flags", Json.parseToJsonElement(featureFlagsText))
                            settings = repository.loadSettings(); notice = "Feature flags saved and read back."
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Feature flag save failed." }
                        finally { busyAction = false }
                    }
                }, modifier = Modifier.testTag("founder-save-feature-flags")) { Text("Save feature flags") }
                Text("Only allow-listed application settings can be changed. Current: ${featureFlags?.value ?: "{}"}", style = MaterialTheme.typography.bodySmall)
            }
            FounderSection.Projects -> {
                Text("Project catalogue", style = MaterialTheme.typography.titleLarge)
                projects.forEach { project ->
                    TextButton(onClick = {
                        projectId = project.id; projectSlug = project.slug; projectTitle = project.title
                        projectSummary = project.summary; projectOwner = project.ownerCategory; projectStage = project.stage
                        projectVisibility = project.visibility; projectTags = project.tags.joinToString(", ")
                        projectWebsiteUrl = project.websiteUrl.orEmpty(); projectRepositoryUrl = project.repositoryUrl.orEmpty()
                    }, modifier = Modifier.testTag("founder-project-${project.id}")) {
                        Text("${project.title} · ${project.stage} · ${project.visibility}")
                    }
                }
                OutlinedTextField(projectSlug, { projectSlug = it }, label = { Text("Project slug") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(projectTitle, { projectTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(projectSummary, { projectSummary = it }, label = { Text("Summary") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(projectOwner, { projectOwner = it }, label = { Text("Owner category: official, community, partner") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(projectStage, { projectStage = it }, label = { Text("Stage") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(projectVisibility, { projectVisibility = it }, label = { Text("Visibility: public, members, private") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(projectTags, { projectTags = it }, label = { Text("Tags, comma separated") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(projectWebsiteUrl, { projectWebsiteUrl = it }, label = { Text("Project website URL (HTTPS)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(projectRepositoryUrl, { projectRepositoryUrl = it }, label = { Text("Source repository URL (HTTPS)") }, modifier = Modifier.fillMaxWidth())
                Button(enabled = !busyAction, onClick = {
                    busyAction = true; error = null; notice = null
                    scope.launch {
                        try {
                            repository.upsertProject(FounderProjectDraft(projectId, projectSlug, projectOwner, projectTitle, projectSummary,
                                projectStage, projectTags.split(',').map(String::trim).filter(String::isNotEmpty), projectVisibility,
                                projectWebsiteUrl.trim().ifBlank { null }, projectRepositoryUrl.trim().ifBlank { null }))
                            val page = repository.loadProjects(); projects = page.projects; projectsNextCursor = page.nextCursor
                            appendProjectsPage = false; notice = "Project saved and read back."; projectId = projects.firstOrNull { it.slug == projectSlug }?.id
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Project save failed." }
                        finally { busyAction = false }
                    }
                }, modifier = Modifier.testTag("founder-save-project")) { Text(if (projectId == null) "Create project" else "Save project") }
                TextButton(onClick = { projectId = null; projectSlug = ""; projectTitle = ""; projectSummary = ""; projectOwner = "community"; projectStage = "idea"; projectVisibility = "public"; projectTags = ""; projectWebsiteUrl = ""; projectRepositoryUrl = "" }) { Text("New project") }
                if (projectsNextCursor != null) OutlinedButton(onClick = { appendProjectsPage = true; refresh++ }, modifier = Modifier.testTag("founder-project-load-more")) { Text("Load more projects") }
                Text("Assign an active member to a project", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(projectMemberId, { projectMemberId = it }, label = { Text("Member account ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(projectMemberRole, { projectMemberRole = it }, label = { Text("Role: owner, maintainer, contributor, tester; blank removes") }, modifier = Modifier.fillMaxWidth())
                projects.forEach { project ->
                    TextButton(enabled = !busyAction, onClick = {
                        busyAction = true; error = null; notice = null
                        scope.launch {
                            try {
                                repository.setProjectMembership(project.id, projectMemberId, projectMemberRole.trim().ifBlank { null })
                                notice = "Project membership saved and audited."
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { error = "Membership update failed." }
                            finally { busyAction = false }
                        }
                    }) { Text("Apply to ${project.title}") }
                }
            }
            FounderSection.Topics -> {
                Text("Community topics", style = MaterialTheme.typography.titleLarge)
                topics.forEach { topic ->
                    TextButton(onClick = {
                        topicId = topic.id; topicSlug = topic.slug; topicTitle = topic.title
                        topicDescription = topic.description; topicVisibility = topic.visibility
                    }, modifier = Modifier.testTag("founder-topic-${topic.id}")) { Text("${topic.title} · ${topic.visibility}") }
                }
                OutlinedTextField(topicSlug, { topicSlug = it }, label = { Text("Topic slug") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(topicTitle, { topicTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(topicDescription, { topicDescription = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(topicVisibility, { topicVisibility = it }, label = { Text("Visibility: public, members, private") }, modifier = Modifier.fillMaxWidth())
                Button(enabled = !busyAction, onClick = {
                    busyAction = true; error = null; notice = null
                    scope.launch {
                        try {
                            repository.upsertTopic(FounderTopicDraft(topicId, topicSlug, topicTitle, topicDescription, topicVisibility))
                            val page = repository.loadTopics(); topics = page.topics; topicsNextCursor = page.nextCursor
                            notice = "Topic saved and read back."; topicId = topics.firstOrNull { it.slug == topicSlug }?.id
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Topic save failed." }
                        finally { busyAction = false }
                    }
                }, modifier = Modifier.testTag("founder-save-topic")) { Text(if (topicId == null) "Create topic" else "Save topic") }
                TextButton(onClick = { topicId = null; topicSlug = ""; topicTitle = ""; topicDescription = ""; topicVisibility = "public" }) { Text("New topic") }
                if (topicsNextCursor != null) OutlinedButton(onClick = { appendTopicsPage = true; refresh++ }, modifier = Modifier.testTag("founder-topic-load-more")) { Text("Load more topics") }
                OutlinedTextField(projectMemberId, { projectMemberId = it }, label = { Text("Member account ID for topic access") }, modifier = Modifier.fillMaxWidth())
                topics.forEach { topic ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(enabled = !busyAction, onClick = {
                            busyAction = true; error = null; notice = null
                            scope.launch {
                                try { repository.setTopicMembership(topic.id, projectMemberId, true); notice = "Topic access granted and audited." }
                                catch (cancelled: CancellationException) { throw cancelled }
                                catch (_: Exception) { error = "Membership update failed." }
                                finally { busyAction = false }
                            }
                        }) { Text("Add member to ${topic.title}") }
                        TextButton(enabled = !busyAction, onClick = {
                            busyAction = true; error = null; notice = null
                            scope.launch {
                                try { repository.setTopicMembership(topic.id, projectMemberId, false); notice = "Topic access removed and audited." }
                                catch (cancelled: CancellationException) { throw cancelled }
                                catch (_: Exception) { error = "Membership update failed." }
                                finally { busyAction = false }
                            }
                        }) { Text("Remove") }
                    }
                }
            }
            FounderSection.Moderation -> {
                Text("Post and comment review", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("hidden", "visible", "removed", "all").forEach { state ->
                        TextButton(onClick = { moderationState = state; contentNextCursor = null; appendContentPage = false; refresh++ }) { Text(state.replaceFirstChar(Char::uppercase)) }
                    }
                }
                contentItems.forEach { item ->
                    Card(Modifier.fillMaxWidth().testTag("founder-content-${item.id}")) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${item.contentType} · ${item.moderationState} · @${item.authorId.take(8)}", style = MaterialTheme.typography.titleMedium)
                            Text(item.body)
                            Text("${item.createdAt} · context ${item.contextId.take(8)}", style = MaterialTheme.typography.bodySmall)
                            Row {
                                if (item.moderationState != "visible") TextButton(onClick = { contentPendingConfirmation = item to "visible" }) { Text("Restore") }
                                if (item.moderationState != "hidden") TextButton(onClick = { contentPendingConfirmation = item to "hidden" }) { Text("Hide") }
                                if (item.moderationState != "removed") TextButton(onClick = { contentPendingConfirmation = item to "removed" }) { Text("Remove") }
                            }
                        }
                    }
                }
                if (contentItems.isEmpty() && !loading && error == null) Text("No content in this queue.")
                if (contentNextCursor != null) OutlinedButton(onClick = { appendContentPage = true; refresh++ }, modifier = Modifier.testTag("founder-content-load-more")) { Text("Load more content") }
            }
            FounderSection.Audit -> {
                if (audit.isEmpty() && !loading && error == null) Text("No audited changes yet.")
                audit.forEach { entry ->
                    Card(Modifier.fillMaxWidth().testTag("founder-audit-${entry.id}")) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(entry.action, style = MaterialTheme.typography.titleMedium)
                            Text("${entry.targetType}${entry.targetId?.let { " · $it" }.orEmpty()} · ${entry.createdAt}")
                            Text(entry.summary)
                        }
                    }
                }
                if (auditNextCursor != null) OutlinedButton(onClick = { appendAuditPage = true; refresh++ }, modifier = Modifier.testTag("founder-audit-load-more")) { Text("Load more audit entries") }
            }
        }
        TextButton(onClick = {
            appendPage = false; appendAuditPage = false; appendContentPage = false
            appendProjectsPage = false; appendTopicsPage = false
            nextCursor = null; auditNextCursor = null; contentNextCursor = null
            projectsNextCursor = null; topicsNextCursor = null; refresh++
        }) { Text("Refresh") }
    }
    memberPendingConfirmation?.let { (member, state) ->
        AlertDialog(
            onDismissRequest = { if (!busyAction) memberPendingConfirmation = null },
            title = { Text("Suspend @${member.handle}?") },
            text = { Text("This member will lose access until reactivated. The action will be recorded in audit history.") },
            confirmButton = {
                Button(enabled = !busyAction, onClick = {
                    busyAction = true; error = null; notice = null
                    scope.launch {
                        try {
                            repository.setMemberState(member.id, state)
                            notice = "Member state updated."
                            memberPendingConfirmation = null
                            appendPage = false; nextCursor = null; refresh++
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Change failed." }
                        finally { busyAction = false }
                    }
                }) { Text("Suspend member") }
            },
            dismissButton = { TextButton(enabled = !busyAction, onClick = { memberPendingConfirmation = null }) { Text("Cancel") } },
        )
    }
    contentPendingConfirmation?.let { (item, state) ->
        AlertDialog(
            onDismissRequest = { if (!busyAction) contentPendingConfirmation = null },
            title = { Text("${state.replaceFirstChar(Char::uppercase)} this ${item.contentType}?") },
            text = { Text("This changes its visibility and adds an audit entry. The content body is never copied into the audit record.") },
            confirmButton = {
                Button(enabled = !busyAction, onClick = {
                    busyAction = true; error = null; notice = null
                    scope.launch {
                        try {
                            repository.setContentModeration(item.contentType, item.id, state)
                            val page = repository.loadContent(moderationState)
                            contentItems = page.items; contentNextCursor = page.nextCursor; appendContentPage = false
                            notice = "Content state saved and read back."; contentPendingConfirmation = null
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Moderation update failed." }
                        finally { busyAction = false }
                    }
                }) { Text(state.replaceFirstChar(Char::uppercase)) }
            },
            dismissButton = { TextButton(enabled = !busyAction, onClick = { contentPendingConfirmation = null }) { Text("Cancel") } },
        )
    }
}
