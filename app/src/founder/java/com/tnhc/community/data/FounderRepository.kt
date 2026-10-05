package com.tnhc.community.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

data class FounderMember(
    val id: String,
    val handle: String,
    val displayName: String?,
    val visibility: String,
    val accountState: String,
    val createdAt: String,
    val roles: List<String>,
)

data class FounderMemberPage(val members: List<FounderMember>, val nextCursor: String?)

data class FounderAuditEntry(
    val id: String,
    val actorId: String,
    val action: String,
    val targetType: String,
    val targetId: String?,
    val summary: String,
    val createdAt: String,
)

data class FounderAuditPage(val entries: List<FounderAuditEntry>, val nextCursor: String?)

data class FounderAppSetting(val key: String, val value: JsonElement, val updatedBy: String, val updatedAt: String)

data class FounderProject(val id: String, val slug: String, val ownerCategory: String, val title: String, val summary: String, val stage: String, val tags: List<String>, val visibility: String)
data class FounderProjectPage(val projects: List<FounderProject>, val nextCursor: String?)
data class FounderProjectDraft(val id: String?, val slug: String, val ownerCategory: String, val title: String, val summary: String, val stage: String, val tags: List<String>, val visibility: String)
data class FounderTopic(val id: String, val slug: String, val title: String, val description: String, val visibility: String)
data class FounderTopicPage(val topics: List<FounderTopic>, val nextCursor: String?)
data class FounderTopicDraft(val id: String?, val slug: String, val title: String, val description: String, val visibility: String)
data class FounderContentItem(val id: String, val contentType: String, val authorId: String, val contextId: String, val body: String, val moderationState: String, val createdAt: String)
data class FounderContentPage(val items: List<FounderContentItem>, val nextCursor: String?)

interface FounderRepository {
    suspend fun loadMembers(cursor: String? = null, pageSize: Int = 20): FounderMemberPage
    suspend fun loadAudit(cursor: String? = null, pageSize: Int = 20): FounderAuditPage
    suspend fun loadSettings(): List<FounderAppSetting>
    suspend fun setMemberState(userId: String, state: String)
    suspend fun setPlatformRole(userId: String, role: String, enabled: Boolean)
    suspend fun inviteMember(email: String)
    suspend fun setAppSetting(key: String, value: JsonElement)
    suspend fun loadProjects(cursor: String? = null): FounderProjectPage
    suspend fun loadTopics(cursor: String? = null): FounderTopicPage
    suspend fun upsertProject(project: FounderProjectDraft)
    suspend fun setProjectMembership(projectId: String, userId: String, role: String?)
    suspend fun upsertTopic(topic: FounderTopicDraft)
    suspend fun setTopicMembership(topicId: String, userId: String, enabled: Boolean)
    suspend fun loadContent(state: String = "hidden", cursor: String? = null): FounderContentPage
    suspend fun setContentModeration(contentType: String, contentId: String, state: String)
}

interface FounderRemoteDataSource {
    suspend fun loadMembers(cursor: String?, limit: Int): FounderMemberPage
    suspend fun loadAudit(cursor: String?, limit: Int): FounderAuditPage
    suspend fun loadSettings(): List<FounderAppSetting>
    suspend fun setMemberState(userId: String, state: String)
    suspend fun setPlatformRole(userId: String, role: String, enabled: Boolean)
    suspend fun inviteMember(email: String)
    suspend fun setAppSetting(key: String, value: JsonElement)
    suspend fun loadProjects(cursor: String?): FounderProjectPage
    suspend fun loadTopics(cursor: String?): FounderTopicPage
    suspend fun upsertProject(project: FounderProjectDraft)
    suspend fun setProjectMembership(projectId: String, userId: String, role: String?)
    suspend fun upsertTopic(topic: FounderTopicDraft)
    suspend fun setTopicMembership(topicId: String, userId: String, enabled: Boolean)
    suspend fun loadContent(state: String, cursor: String?, limit: Int): FounderContentPage
    suspend fun setContentModeration(contentType: String, contentId: String, state: String)
}

class DefaultFounderRepository(private val remote: FounderRemoteDataSource) : FounderRepository {
    override suspend fun loadMembers(cursor: String?, pageSize: Int) = remote.loadMembers(cursor, pageSize.coerceIn(1, 50))
    override suspend fun loadAudit(cursor: String?, pageSize: Int) = remote.loadAudit(cursor, pageSize.coerceIn(1, 50))
    override suspend fun loadSettings() = remote.loadSettings()

    override suspend fun setMemberState(userId: String, state: String) {
        require(userId.isNotBlank() && state in MEMBER_STATES) { "Choose a valid member state" }
        remote.setMemberState(userId, state)
    }

    override suspend fun setPlatformRole(userId: String, role: String, enabled: Boolean) {
        require(userId.isNotBlank() && role in PLATFORM_ROLES) { "Choose a manageable platform role" }
        remote.setPlatformRole(userId, role, enabled)
    }

    override suspend fun inviteMember(email: String) {
        val normalizedEmail = email.trim().lowercase()
        require(normalizedEmail.length <= 254 && EMAIL.matches(normalizedEmail)) { "Enter a valid email address" }
        remote.inviteMember(normalizedEmail)
    }

    override suspend fun setAppSetting(key: String, value: JsonElement) {
        require(SETTING_KEYS.contains(key)) { "Application setting is not available" }
        when (key) {
            "maintenance_notice" -> require((value as? JsonPrimitive)?.isString == true && value.jsonPrimitive.content.length <= 240) {
                "Maintenance notice must be text up to 240 characters"
            }
            "feature_flags" -> {
                val flags = value as? JsonObject ?: throw IllegalArgumentException("Feature flags must be an object")
                require(flags.size <= 64 && flags.values.all { (it as? JsonPrimitive)?.booleanOrNull != null }) {
                    "Feature flags must contain at most 64 boolean values"
                }
            }
        }
        remote.setAppSetting(key, value)
    }

    override suspend fun loadProjects(cursor: String?) = remote.loadProjects(cursor)
    override suspend fun loadTopics(cursor: String?) = remote.loadTopics(cursor)
    override suspend fun upsertProject(project: FounderProjectDraft) {
        require(SLUG.matches(project.slug) && project.title.trim().isNotEmpty() && project.title.trim().length <= 160 &&
            project.summary.length <= 2000 && project.ownerCategory in OWNER_CATEGORIES && project.stage in PROJECT_STAGES &&
            project.visibility in VISIBILITIES && project.tags.size <= 32 && project.tags.all { it.isNotBlank() && it.length <= 48 }) {
            "Check the project fields and try again"
        }
        remote.upsertProject(project.copy(slug = project.slug.trim(), title = project.title.trim(), tags = project.tags.map(String::trim)))
    }
    override suspend fun setProjectMembership(projectId: String, userId: String, role: String?) {
        require(projectId.isNotBlank() && userId.isNotBlank() && (role == null || role in PROJECT_ROLES)) { "Choose a valid project role" }
        remote.setProjectMembership(projectId, userId, role)
    }
    override suspend fun upsertTopic(topic: FounderTopicDraft) {
        require(SLUG.matches(topic.slug) && topic.title.trim().isNotEmpty() && topic.title.trim().length <= 160 &&
            topic.description.length <= 2000 && topic.visibility in VISIBILITIES) { "Check the topic fields and try again" }
        remote.upsertTopic(topic.copy(slug = topic.slug.trim(), title = topic.title.trim()))
    }
    override suspend fun setTopicMembership(topicId: String, userId: String, enabled: Boolean) {
        require(topicId.isNotBlank() && userId.isNotBlank()) { "Choose a valid topic and member" }
        remote.setTopicMembership(topicId, userId, enabled)
    }
    override suspend fun loadContent(state: String, cursor: String?) = remote.loadContent(state, cursor, 30)
    override suspend fun setContentModeration(contentType: String, contentId: String, state: String) {
        require(contentType in CONTENT_TYPES && contentId.isNotBlank() && state in CONTENT_STATES) { "Choose a valid moderation action" }
        remote.setContentModeration(contentType, contentId, state)
    }

    private companion object {
        val MEMBER_STATES = setOf("active", "suspended", "closed")
        val PLATFORM_ROLES = setOf("administrator", "moderator")
        val SETTING_KEYS = setOf("maintenance_notice", "feature_flags")
        val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        val SLUG = Regex("^[a-z0-9]+([a-z0-9-]*[a-z0-9])?$")
        val OWNER_CATEGORIES = setOf("official", "community", "partner")
        val PROJECT_STAGES = setOf("idea", "prototype", "alpha", "beta", "released", "paused", "archived")
        val PROJECT_ROLES = setOf("owner", "maintainer", "contributor", "tester")
        val VISIBILITIES = setOf("public", "members", "private")
        val CONTENT_TYPES = setOf("post", "comment")
        val CONTENT_STATES = setOf("visible", "hidden", "removed")
    }
}

class SupabaseFounderRemoteDataSource internal constructor(private val client: SupabaseClient) : FounderRemoteDataSource {
    override suspend fun loadMembers(cursor: String?, limit: Int): FounderMemberPage = client.postgrest
        .rpc("founder_list_members", jsonCodec.encodeToJsonElement(FounderPageRequest(cursor, limit)).jsonObject)
        .decodeAs<FounderMemberPageResponse>()
        .toDomain()

    override suspend fun loadAudit(cursor: String?, limit: Int): FounderAuditPage = client.postgrest
        .rpc("founder_list_audit", jsonCodec.encodeToJsonElement(FounderPageRequest(cursor, limit)).jsonObject)
        .decodeAs<FounderAuditPageResponse>()
        .toDomain()

    override suspend fun loadSettings(): List<FounderAppSetting> = client.from("app_settings")
        .select()
        .decodeList<FounderSettingRecord>()
        .map { FounderAppSetting(it.key, it.value, it.updatedBy, it.updatedAt) }

    override suspend fun setMemberState(userId: String, state: String) {
        client.postgrest.rpc("founder_set_member_state", jsonCodec.encodeToJsonElement(FounderSetMemberStateRequest(userId, state)).jsonObject)
    }

    override suspend fun setPlatformRole(userId: String, role: String, enabled: Boolean) {
        client.postgrest.rpc("founder_set_platform_role", jsonCodec.encodeToJsonElement(FounderSetPlatformRoleRequest(userId, role, enabled)).jsonObject)
    }

    override suspend fun inviteMember(email: String) {
        val response = client.functions.invoke("invite-member", FounderInviteRequest(email)).body<FounderInviteResponse>()
        check(response.status == "sent") { "Invitation could not be confirmed" }
    }

    override suspend fun setAppSetting(key: String, value: JsonElement) {
        client.postgrest.rpc("founder_set_app_setting", jsonCodec.encodeToJsonElement(FounderSetAppSettingRequest(key, value)).jsonObject)
    }

    override suspend fun loadProjects(cursor: String?): FounderProjectPage = client.postgrest.rpc(
        "list_projects", jsonCodec.encodeToJsonElement(FounderProjectListRequest(cursor)).jsonObject,
    ).decodeAs<FounderProjectPageResponse>().let { response ->
        FounderProjectPage(response.items.map { FounderProject(it.id, it.slug, it.ownerCategory, it.title, it.summary, it.stage, it.tags, it.visibility) }, response.nextCursor)
    }

    override suspend fun loadTopics(cursor: String?): FounderTopicPage = client.postgrest.rpc(
        "founder_list_topics", jsonCodec.encodeToJsonElement(FounderPageRequest(cursor, 50)).jsonObject,
    ).decodeAs<FounderTopicPageResponse>().let { response ->
        FounderTopicPage(response.items.map { FounderTopic(it.id, it.slug, it.title, it.description, it.visibility) }, response.nextCursor)
    }

    override suspend fun upsertProject(project: FounderProjectDraft) {
        client.postgrest.rpc("founder_upsert_project", jsonCodec.encodeToJsonElement(FounderUpsertProjectRequest(
            project.id, project.slug, project.ownerCategory, project.title, project.summary, project.stage, project.tags, project.visibility,
        )).jsonObject)
    }

    override suspend fun setProjectMembership(projectId: String, userId: String, role: String?) {
        client.postgrest.rpc("founder_set_project_membership", jsonCodec.encodeToJsonElement(FounderSetProjectMembershipRequest(projectId, userId, role)).jsonObject)
    }

    override suspend fun upsertTopic(topic: FounderTopicDraft) {
        client.postgrest.rpc("founder_upsert_topic", jsonCodec.encodeToJsonElement(FounderUpsertTopicRequest(
            topic.id, topic.slug, topic.title, topic.description, topic.visibility,
        )).jsonObject)
    }

    override suspend fun setTopicMembership(topicId: String, userId: String, enabled: Boolean) {
        client.postgrest.rpc("founder_set_topic_membership", jsonCodec.encodeToJsonElement(
            FounderSetTopicMembershipRequest(topicId, userId, enabled),
        ).jsonObject)
    }

    override suspend fun loadContent(state: String, cursor: String?, limit: Int): FounderContentPage = client.postgrest.rpc(
        "founder_list_content", jsonCodec.encodeToJsonElement(FounderContentPageRequest(state, cursor, limit)).jsonObject,
    ).decodeAs<FounderContentPageResponse>().let { page ->
        FounderContentPage(page.items.map { FounderContentItem(it.id, it.contentType, it.authorId, it.contextId, it.body, it.moderationState, it.createdAt) }, page.nextCursor)
    }

    override suspend fun setContentModeration(contentType: String, contentId: String, state: String) {
        client.postgrest.rpc("founder_set_content_moderation", jsonCodec.encodeToJsonElement(
            FounderModerationRequest(contentType, contentId, state),
        ).jsonObject)
    }
}

private fun FounderMemberPageResponse.toDomain() = FounderMemberPage(items.map {
    FounderMember(it.id, it.handle, it.displayName, it.visibility, it.accountState, it.createdAt, it.roles)
}, nextCursor)

private fun FounderAuditPageResponse.toDomain() = FounderAuditPage(items.map {
    FounderAuditEntry(it.id, it.actorId, it.action, it.targetType, it.targetId, it.summary, it.createdAt)
}, nextCursor)

@Serializable
private data class FounderPageRequest(@SerialName("p_cursor") val cursor: String?, @SerialName("p_limit") val limit: Int)

@Serializable
private data class FounderProjectListRequest(@SerialName("p_cursor") val cursor: String?, @SerialName("p_limit") val limit: Int = 50,
    @SerialName("p_stage") val stage: String? = null, @SerialName("p_owner_category") val ownerCategory: String? = null, @SerialName("p_tag") val tag: String? = null)
@Serializable
private data class FounderUpsertProjectRequest(@SerialName("p_project_id") val projectId: String?, @SerialName("p_slug") val slug: String,
    @SerialName("p_owner_category") val ownerCategory: String, @SerialName("p_title") val title: String, @SerialName("p_summary") val summary: String,
    @SerialName("p_stage") val stage: String, @SerialName("p_tags") val tags: List<String>, @SerialName("p_visibility") val visibility: String)
@Serializable
private data class FounderSetProjectMembershipRequest(@SerialName("p_project_id") val projectId: String, @SerialName("p_user_id") val userId: String, @SerialName("p_role") val role: String?)
@Serializable
private data class FounderUpsertTopicRequest(@SerialName("p_topic_id") val topicId: String?, @SerialName("p_slug") val slug: String,
    @SerialName("p_title") val title: String, @SerialName("p_description") val description: String, @SerialName("p_visibility") val visibility: String)
@Serializable
private data class FounderSetTopicMembershipRequest(@SerialName("p_topic_id") val topicId: String, @SerialName("p_user_id") val userId: String, @SerialName("p_enabled") val enabled: Boolean)
@Serializable
private data class FounderContentPageRequest(@SerialName("p_state") val state: String, @SerialName("p_cursor") val cursor: String?, @SerialName("p_limit") val limit: Int)
@Serializable
private data class FounderModerationRequest(@SerialName("p_content_type") val contentType: String, @SerialName("p_content_id") val contentId: String, @SerialName("p_state") val state: String)

@Serializable
private data class FounderSetMemberStateRequest(@SerialName("p_user_id") val userId: String, @SerialName("p_state") val state: String)

@Serializable
private data class FounderSetPlatformRoleRequest(
    @SerialName("p_user_id") val userId: String,
    @SerialName("p_role") val role: String,
    @SerialName("p_enabled") val enabled: Boolean,
)

@Serializable
private data class FounderSetAppSettingRequest(@SerialName("p_key") val key: String, @SerialName("p_value") val value: JsonElement)

@Serializable
private data class FounderInviteRequest(val email: String)

@Serializable
private data class FounderInviteResponse(val status: String)

@Serializable
private data class FounderSettingRecord(
    val key: String,
    val value: JsonElement,
    @SerialName("updated_by") val updatedBy: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
private data class FounderMemberRecord(
    val id: String,
    val handle: String,
    @SerialName("display_name") val displayName: String?,
    val visibility: String,
    @SerialName("account_state") val accountState: String,
    @SerialName("created_at") val createdAt: String,
    val roles: List<String>,
)

@Serializable
private data class FounderMemberPageResponse(val items: List<FounderMemberRecord>, @SerialName("next_cursor") val nextCursor: String?)

@Serializable
private data class FounderAuditRecord(
    val id: String,
    @SerialName("actor_id") val actorId: String,
    val action: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String?,
    val summary: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
private data class FounderAuditPageResponse(val items: List<FounderAuditRecord>, @SerialName("next_cursor") val nextCursor: String?)

@Serializable
private data class FounderProjectRecord(val id: String, val slug: String, @SerialName("owner_category") val ownerCategory: String, val title: String,
    val summary: String, val stage: String, val tags: List<String>, val visibility: String)
@Serializable
private data class FounderProjectPageResponse(val items: List<FounderProjectRecord>, @SerialName("next_cursor") val nextCursor: String?)
@Serializable
private data class FounderTopicRecord(val id: String, val slug: String, val title: String, val description: String, val visibility: String)
@Serializable
private data class FounderTopicPageResponse(val items: List<FounderTopicRecord>, @SerialName("next_cursor") val nextCursor: String?)
@Serializable
private data class FounderContentRecord(val id: String, @SerialName("content_type") val contentType: String, @SerialName("author_id") val authorId: String,
    @SerialName("context_id") val contextId: String, val body: String, @SerialName("moderation_state") val moderationState: String,
    @SerialName("created_at") val createdAt: String)
@Serializable
private data class FounderContentPageResponse(val items: List<FounderContentRecord>, @SerialName("next_cursor") val nextCursor: String?)

private val jsonCodec = kotlinx.serialization.json.Json
