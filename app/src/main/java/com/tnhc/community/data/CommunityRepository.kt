package com.tnhc.community.data

import android.content.Context
import android.content.Intent
import android.util.Base64
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.Cipher

data class CommunitySession(val userId: String)

data class ProjectFilter(val stage: String? = null, val ownerCategory: String? = null, val tag: String? = null)

data class ProjectPage(val projects: List<Project>, val nextCursor: String?)

suspend fun CommunityRepository.loadAllProjects(filter: ProjectFilter = ProjectFilter(), maxPages: Int = 10): List<Project> {
    require(maxPages in 1..20) { "Project page limit must be between 1 and 20" }
    val projects = mutableListOf<Project>()
    var cursor: String? = null
    repeat(maxPages) {
        val page = loadProjects(cursor, filter)
        projects += page.projects
        cursor = page.nextCursor ?: return projects
    }
    return projects
}

data class MemberProfile(
    val id: String,
    val handle: String,
    val displayName: String?,
    val bio: String?,
    val interests: List<String>,
    val visibility: String,
)

data class CommunityTopic(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val visibility: String,
)

data class CommunityPost(
    val id: String,
    val authorId: String,
    val topicId: String,
    val body: String,
    val createdAt: String,
)

interface CommunityRepository {
    val session: StateFlow<CommunitySession?>
    suspend fun isFounder(): Boolean = false
    suspend fun isActiveMember(): Boolean = false
    fun handleAuthLink(intent: Intent)
    suspend fun signIn(email: String, password: String)
    suspend fun signOut()
    suspend fun setPassword(password: String)
    suspend fun loadProjects(cursor: String? = null, filter: ProjectFilter = ProjectFilter()): ProjectPage
    suspend fun loadProfile(): MemberProfile
    suspend fun loadFollowedProjectIds(): Set<String>
    suspend fun updateProfile(displayName: String?, bio: String?, interests: List<String>, visibility: String)
    suspend fun setProjectFollow(projectId: String, followed: Boolean)
    suspend fun loadTopics(): List<CommunityTopic> = emptyList()
    suspend fun loadJoinedTopicIds(): Set<String> = emptySet()
    suspend fun setTopicMembership(topicId: String, joined: Boolean) = Unit
    suspend fun loadTopicPosts(topicId: String): List<CommunityPost> = emptyList()
    suspend fun createTopicPost(topicId: String, body: String) = Unit
}

interface CommunityRemoteDataSource {
    val sessions: StateFlow<CommunitySession?>
    suspend fun isFounder(): Boolean = false
    suspend fun isActiveMember(): Boolean = false
    fun handleAuthLink(intent: Intent)
    suspend fun signIn(email: String, password: String)
    suspend fun signOut()
    suspend fun setPassword(password: String)
    suspend fun loadProjects(cursor: String?, filter: ProjectFilter): ProjectPage
    suspend fun loadProfile(): MemberProfile
    suspend fun loadFollowedProjectIds(): Set<String>
    suspend fun updateProfile(displayName: String?, bio: String?, interests: List<String>, visibility: String)
    suspend fun setProjectFollow(projectId: String, followed: Boolean)
    suspend fun loadTopics(): List<CommunityTopic> = emptyList()
    suspend fun loadJoinedTopicIds(): Set<String> = emptySet()
    suspend fun setTopicMembership(topicId: String, joined: Boolean) = Unit
    suspend fun loadTopicPosts(topicId: String): List<CommunityPost> = emptyList()
    suspend fun createTopicPost(topicId: String, body: String) = Unit
}

class DefaultCommunityRepository(private val remote: CommunityRemoteDataSource) : CommunityRepository {
    override val session: StateFlow<CommunitySession?> get() = remote.sessions
    override suspend fun isFounder() = remote.isFounder()
    override suspend fun isActiveMember() = session.value != null && remote.isActiveMember()
    override fun handleAuthLink(intent: Intent) = remote.handleAuthLink(intent)

    override suspend fun signIn(email: String, password: String) = remote.signIn(email.trim(), password)
    override suspend fun signOut() = remote.signOut()
    override suspend fun setPassword(password: String) {
        require(password.length >= 12) { "Use a password with at least 12 characters" }
        remote.setPassword(password)
    }
    override suspend fun loadProjects(cursor: String?, filter: ProjectFilter) = remote.loadProjects(cursor, filter)
    override suspend fun loadProfile() = remote.loadProfile()
    override suspend fun loadFollowedProjectIds() = remote.loadFollowedProjectIds()
    override suspend fun updateProfile(displayName: String?, bio: String?, interests: List<String>, visibility: String) =
        remote.updateProfile(displayName?.trim(), bio?.trim(), interests.map(String::trim).filter(String::isNotEmpty).distinct(), visibility)

    override suspend fun setProjectFollow(projectId: String, followed: Boolean) = remote.setProjectFollow(projectId, followed)

    override suspend fun loadTopics() = remote.loadTopics().sortedBy { it.title.lowercase() }
    override suspend fun loadJoinedTopicIds(): Set<String> = if (session.value == null) emptySet() else remote.loadJoinedTopicIds()
    override suspend fun setTopicMembership(topicId: String, joined: Boolean) {
        require(topicId.isNotBlank()) { "Choose a community first" }
        check(session.value != null) { "Sign in with your invitation to join communities" }
        remote.setTopicMembership(topicId, joined)
    }
    override suspend fun loadTopicPosts(topicId: String): List<CommunityPost> {
        require(topicId.isNotBlank()) { "Choose a community first" }
        return remote.loadTopicPosts(topicId).sortedByDescending { it.createdAt }.take(50)
    }
    override suspend fun createTopicPost(topicId: String, body: String) {
        require(topicId.isNotBlank()) { "Choose a community first" }
        check(session.value != null) { "Sign in with your invitation to post" }
        val normalizedBody = body.trim()
        require(normalizedBody.isNotEmpty() && normalizedBody.length <= 5000) { "Posts must contain 1 to 5000 characters" }
        remote.createTopicPost(topicId, normalizedBody)
    }
}

class EncryptedAndroidSessionManager(context: Context) : SessionManager {
    private val preferences = context.applicationContext.getSharedPreferences("supabase-auth-session", Context.MODE_PRIVATE)

    override suspend fun saveSession(session: UserSession) {
        val serialized = Json.encodeToString(UserSession.serializer(), session)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        val ciphertext = Base64.encodeToString(cipher.doFinal(serialized.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        check(preferences.edit().putString(IV_KEY, iv).putString(SESSION_KEY, ciphertext).commit()) {
            "Could not persist the authentication session"
        }
    }

    override suspend fun loadSession(): UserSession {
        val ciphertext = preferences.getString(SESSION_KEY, null) ?: throw NoSessionFoundException()
        val iv = preferences.getString(IV_KEY, null) ?: throw NoSessionFoundException()
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), javax.crypto.spec.GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            val serialized = cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)).toString(Charsets.UTF_8)
            Json.decodeFromString(UserSession.serializer(), serialized)
        }
            .getOrElse {
                deleteSession()
                throw NoSessionFoundException()
            }
    }

    override suspend fun deleteSession() {
        preferences.edit().remove(SESSION_KEY).remove(IV_KEY).commit()
    }

    private fun encryptionKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private companion object {
        const val SESSION_KEY = "session"
        const val IV_KEY = "session_iv"
        const val KEY_ALIAS = "tnhc-community-auth-session"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

@Serializable
private data class ProjectListParameters(
    val p_cursor: String? = null,
    val p_limit: Int = 20,
    val p_stage: String? = null,
    val p_owner_category: String? = null,
    val p_tag: String? = null,
)

@Serializable
private data class ProjectListResponse(val items: List<ProjectRecord>, val next_cursor: String?)

@Serializable
private data class ProjectRecord(
    val id: String,
    val slug: String,
    val owner_category: String,
    val title: String,
    val summary: String,
    val stage: String,
    val tags: List<String>,
    val visibility: String,
    val website_url: String? = null,
    val repository_url: String? = null,
    val status_as_of: String? = null,
)

@Serializable
private data class ProfileRecord(
    val id: String,
    val handle: String,
    val display_name: String? = null,
    val bio: String? = null,
    val interests: List<String> = emptyList(),
    val visibility: String,
)

@Serializable
private data class FollowRecord(val project_id: String)

@Serializable
private data class FollowedProjectRecord(val project_id: String)

@Serializable
private data class PlatformRoleRecord(val role: String)
@Serializable private data class TopicRecord(val id: String, val slug: String, val title: String, val description: String, val visibility: String)
@Serializable private data class TopicMembershipRecord(val topic_id: String)
@Serializable private data class TopicMembershipInsert(val topic_id: String)
@Serializable private data class TopicPostRecord(val id: String, val author_id: String, val topic_id: String, val body: String, val created_at: String)
@Serializable private data class TopicPostInsert(val topic_id: String, val body: String)

class SupabaseCommunityRemoteDataSource internal constructor(internal val client: SupabaseClient) : CommunityRemoteDataSource {
    constructor(context: Context, config: BackendConfig) : this(createSupabaseClient(config.url, config.publishableKey) {
        install(Auth) {
            sessionManager = EncryptedAndroidSessionManager(context)
            autoLoadFromStorage = true
            alwaysAutoRefresh = true
            scheme = "tnhccommunity"
            host = "invite"
        }
        install(Postgrest)
        install(Functions)
    })
    private val mutableSessions = MutableStateFlow<CommunitySession?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override val sessions: StateFlow<CommunitySession?> = mutableSessions

    override suspend fun isFounder(): Boolean {
        val id = requireMemberId()
        return client.from("platform_roles").select {
            filter {
                eq("user_id", id)
                eq("role", "founder")
            }
            limit(1)
        }.decodeList<PlatformRoleRecord>().isNotEmpty()
    }

    override suspend fun isActiveMember(): Boolean {
        if (sessions.value == null) return false
        return client.postgrest.rpc("viewer_is_active_member").decodeAs<Boolean>()
    }

    init {
        scope.launch {
            client.auth.sessionStatus.collect { status ->
                mutableSessions.value = (status as? SessionStatus.Authenticated)
                    ?.session?.user?.id?.let(::CommunitySession)
            }
        }
    }

    override suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut() {
        try {
            client.auth.signOut()
        } finally {
            withContext(NonCancellable) { client.auth.clearSession() }
        }
    }

    override suspend fun setPassword(password: String) {
        require(password.length >= 12) { "Use a password with at least 12 characters" }
        client.auth.updateUser { this.password = password }
    }

    override suspend fun loadProjects(cursor: String?, filter: ProjectFilter): ProjectPage {
        val response = client.postgrest.rpc(
            "list_projects",
            Json.encodeToJsonElement(ProjectListParameters(cursor, 20, filter.stage, filter.ownerCategory, filter.tag)).jsonObject,
        ).decodeAs<ProjectListResponse>()
        return ProjectPage(response.items.map { record ->
            Project(
                id = record.id,
                title = record.title,
                summary = record.summary,
                category = record.tags.firstOrNull() ?: record.owner_category,
                stage = projectStageLabel(record.stage),
                tags = record.tags,
                focus = record.summary,
                affiliation = record.owner_category,
                isDemo = false,
                websiteUrl = record.website_url,
                repositoryUrl = record.repository_url,
                statusAsOf = record.status_as_of,
            )
        }, response.next_cursor)
    }

    override suspend fun loadProfile(): MemberProfile {
        val id = requireMemberId()
        val profile = client.from("profiles").select {
            filter { eq("id", id) }
            limit(1)
        }.decodeSingle<ProfileRecord>()
        return profile.toMemberProfile()
    }

    override suspend fun loadFollowedProjectIds(): Set<String> {
        val id = requireMemberId()
        return client.from("project_follows").select {
            filter { eq("user_id", id) }
        }.decodeList<FollowedProjectRecord>().mapTo(linkedSetOf()) { it.project_id }
    }

    override suspend fun updateProfile(displayName: String?, bio: String?, interests: List<String>, visibility: String) {
        val id = requireMemberId()
        client.from("profiles").update({
            set("display_name", displayName)
            set("bio", bio)
            set("interests", interests)
            set("visibility", visibility)
        }) {
            filter { eq("id", id) }
        }
    }

    override suspend fun setProjectFollow(projectId: String, followed: Boolean) {
        val id = requireMemberId()
        if (followed) {
            client.from("project_follows").upsert(
                FollowRecord(project_id = projectId),
            ) {
                onConflict = "project_id,user_id"
                ignoreDuplicates = true
            }
        } else {
            client.from("project_follows").delete {
                filter {
                    eq("project_id", projectId)
                    eq("user_id", id)
                }
            }
        }
    }

    override suspend fun loadTopics(): List<CommunityTopic> = client.from("topics").select {
        filter { eq("visibility", "public") }
    }.decodeList<TopicRecord>().map { CommunityTopic(it.id, it.slug, it.title, it.description, it.visibility) }

    override suspend fun loadJoinedTopicIds(): Set<String> {
        val id = requireMemberId()
        return client.from("topic_memberships").select {
            filter { eq("user_id", id) }
        }.decodeList<TopicMembershipRecord>().mapTo(linkedSetOf()) { it.topic_id }
    }

    override suspend fun setTopicMembership(topicId: String, joined: Boolean) {
        val id = requireMemberId()
        if (joined) {
            client.from("topic_memberships").upsert(TopicMembershipInsert(topic_id = topicId)) {
                onConflict = "topic_id,user_id"
                ignoreDuplicates = true
            }
        } else {
            client.from("topic_memberships").delete {
                filter { eq("topic_id", topicId); eq("user_id", id) }
            }
        }
    }

    override suspend fun loadTopicPosts(topicId: String): List<CommunityPost> = client.from("posts").select {
        filter { eq("topic_id", topicId); eq("moderation_state", "visible") }
        limit(50)
    }.decodeList<TopicPostRecord>()
        .map { CommunityPost(it.id, it.author_id, it.topic_id, it.body, it.created_at) }

    override suspend fun createTopicPost(topicId: String, body: String) {
        requireMemberId()
        client.from("posts").insert(TopicPostInsert(topic_id = topicId, body = body))
    }

    override fun handleAuthLink(intent: Intent) {
        client.handleDeeplinks(intent)
    }

    private suspend fun requireMemberId(): String {
        client.auth.awaitInitialization()
        return client.auth.currentUserOrNull()?.id ?: throw IllegalStateException("Sign in to continue")
    }

    private fun ProfileRecord.toMemberProfile() = MemberProfile(id, handle, display_name, bio, interests, visibility)
}
