package com.tnhc.community.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tnhc.community.data.CommunityPost
import com.tnhc.community.data.CommunityRepository
import com.tnhc.community.data.CommunitySession
import com.tnhc.community.data.CommunityTopic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@Composable
fun CommunityScreen(
    repository: CommunityRepository,
    session: CommunitySession?,
    onSignIn: () -> Unit,
) {
    var reload by remember(repository, session?.userId) { mutableIntStateOf(0) }
    var topics by remember(repository, session?.userId) { mutableStateOf<List<CommunityTopic>?>(null) }
    var joinedIds by remember(repository, session?.userId) { mutableStateOf<Set<String>>(emptySet()) }
    var previews by remember(repository, session?.userId) { mutableStateOf<Map<String, CommunityPost?>>(emptyMap()) }
    var previewFailures by remember(repository, session?.userId) { mutableStateOf<Set<String>>(emptySet()) }
    var previewLoading by remember(repository, session?.userId) { mutableStateOf(false) }
    var loadError by remember(repository, session?.userId) { mutableStateOf(false) }
    var membershipError by remember(repository, session?.userId) { mutableStateOf(false) }
    var selectedTopicId by remember(repository, session?.userId) { mutableStateOf<String?>(null) }
    var pendingTopicIds by remember(repository, session?.userId) { mutableStateOf<Set<String>>(emptySet()) }
    var actionError by remember(repository, session?.userId, selectedTopicId) { mutableStateOf<String?>(null) }
    var postBodies by remember(repository, session?.userId) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var pendingPostTopicIds by remember(repository, session?.userId) { mutableStateOf<Set<String>>(emptySet()) }
    var postRefresh by remember(repository, session?.userId) { mutableIntStateOf(0) }
    var feedPosts by remember(repository, session?.userId, selectedTopicId) { mutableStateOf<List<CommunityPost>?>(null) }
    var feedError by remember(repository, session?.userId, selectedTopicId) { mutableStateOf(false) }
    var feedLoading by remember(repository, session?.userId, selectedTopicId) { mutableStateOf(false) }
    var activeMember by remember(repository, session?.userId) { mutableStateOf(false) }
    var activeMemberChecked by remember(repository, session?.userId) { mutableStateOf(false) }
    var activeMemberCheckError by remember(repository, session?.userId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, session?.userId, reload) {
        activeMember = false
        activeMemberChecked = session == null
        activeMemberCheckError = false
        if (session != null) {
            try {
                activeMember = repository.isActiveMember()
                activeMemberChecked = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                activeMemberCheckError = true
                activeMemberChecked = true
            }
        }
    }

    LaunchedEffect(repository, session?.userId, reload) {
        topics = null
        joinedIds = emptySet()
        previews = emptyMap()
        previewFailures = emptySet()
        loadError = false
        membershipError = false
        try {
            val loadedTopics = repository.loadTopics()
            topics = loadedTopics
            if (session != null) {
                try {
                    joinedIds = repository.loadJoinedTopicIds()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    membershipError = true
                }
            }
            previewLoading = loadedTopics.isNotEmpty()
            val previewResults = supervisorScope {
                loadedTopics.map { topic ->
                    async {
                        topic.id to try {
                            Result.success(repository.loadTopicPosts(topic.id).firstOrNull())
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            Result.failure<CommunityPost?>(IllegalStateException("Recent posts could not load"))
                        }
                    }
                }.awaitAll().toMap()
            }
            previews = previewResults.mapValues { (_, result) -> result.getOrNull() }
            previewFailures = previewResults.filterValues { it.isFailure }.keys
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loadError = true
        } finally {
            previewLoading = false
        }
    }

    val selectedTopic = topics?.firstOrNull { it.id == selectedTopicId }
    BackHandler(enabled = selectedTopic != null) {
        selectedTopicId = null
        actionError = null
    }
    LaunchedEffect(repository, session?.userId, selectedTopicId, postRefresh) {
        val topicId = selectedTopicId
        if (topicId == null || topics?.none { it.id == topicId } != false) return@LaunchedEffect
        feedLoading = true
        feedError = false
        feedPosts = null
        try {
            val posts = repository.loadTopicPosts(topicId)
            feedPosts = posts
            previews = previews + (topicId to posts.firstOrNull())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            feedError = true
        } finally {
            feedLoading = false
        }
    }

    fun toggleMembership(topic: CommunityTopic) {
        if (session == null) {
            onSignIn()
            return
        }
        if (topic.id in pendingTopicIds) return
        val join = topic.id !in joinedIds
        pendingTopicIds = pendingTopicIds + topic.id
        actionError = null
        scope.launch {
            try {
                repository.setTopicMembership(topic.id, join)
                joinedIds = if (join) joinedIds + topic.id else joinedIds - topic.id
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                actionError = "Your membership change could not be saved. Try again."
            } finally {
                pendingTopicIds = pendingTopicIds - topic.id
            }
        }
    }

    fun submitPost(topic: CommunityTopic) {
        val draft = postBodies[topic.id].orEmpty()
        val body = draft.trim()
        if (session == null) {
            onSignIn()
            return
        }
        if (!activeMember || body.isEmpty() || topic.id in pendingPostTopicIds) return
        pendingPostTopicIds = pendingPostTopicIds + topic.id
        actionError = null
        scope.launch {
            try {
                repository.createTopicPost(topic.id, body)
                if (postBodies[topic.id] == draft) postBodies = postBodies - topic.id
                postRefresh++
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                actionError = "Your post could not be published. Check your connection and try again."
            } finally {
                pendingPostTopicIds = pendingPostTopicIds - topic.id
            }
        }
    }

    when {
        loadError -> CommunityMessage(
            title = "Communities could not load",
            message = "Check your connection and try again. Your community data is shared across Member and Founder apps.",
            action = "Try again",
            tag = "community-retry",
            onAction = { reload++ },
        )
        topics == null -> CommunityLoading()
        topics.isNullOrEmpty() -> CommunityMessage(
            title = "No communities yet",
            message = "There are no public topic communities available right now.",
            action = "Refresh",
            tag = "community-retry",
            onAction = { reload++ },
        )
        selectedTopic != null -> CommunityTopicFeed(
            topic = selectedTopic,
            posts = feedPosts.orEmpty(),
            currentUserId = session?.userId,
            joined = selectedTopic.id in joinedIds,
            signedIn = session != null,
            loading = feedLoading,
            error = feedError,
            pending = selectedTopic.id in pendingTopicIds,
            membershipError = membershipError,
            actionError = actionError,
            postBody = postBodies[selectedTopic.id].orEmpty(),
            posting = selectedTopic.id in pendingPostTopicIds,
            canPost = activeMember,
            activeMemberChecked = activeMemberChecked,
            activeMemberCheckError = activeMemberCheckError,
            onBack = { selectedTopicId = null; actionError = null },
            onToggleMembership = { toggleMembership(selectedTopic) },
            onPostBody = { body -> postBodies = postBodies + (selectedTopic.id to body) },
            onSubmitPost = { submitPost(selectedTopic) },
            onRetry = { postRefresh++ },
            onRetryMembership = { reload++ },
            onRetryAccess = { reload++ },
        )
        else -> CommunityDirectory(
            topics = topics.orEmpty(),
            joinedIds = joinedIds,
            previews = previews,
            previewFailures = previewFailures,
            currentUserId = session?.userId,
            previewLoading = previewLoading,
            pendingTopicIds = pendingTopicIds,
            membershipError = membershipError,
            actionError = actionError,
            onOpen = { selectedTopicId = it.id },
            onToggleMembership = ::toggleMembership,
            onRefreshMembership = { reload++ },
            onRetry = { reload++ },
        )
    }
}

@Composable
private fun CommunityDirectory(
    topics: List<CommunityTopic>,
    joinedIds: Set<String>,
    previews: Map<String, CommunityPost?>,
    previewFailures: Set<String>,
    currentUserId: String?,
    previewLoading: Boolean,
    pendingTopicIds: Set<String>,
    membershipError: Boolean,
    actionError: String?,
    onOpen: (CommunityTopic) -> Unit,
    onToggleMembership: (CommunityTopic) -> Unit,
    onRefreshMembership: () -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("community-list"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("${topics.size} topic ${if (topics.size == 1) "community" else "communities"}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Find your people, follow a topic, and share what you are making.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (membershipError) {
            item {
                CommunityNotice(
                    message = "Your joined communities could not load.",
                    action = "Retry",
                    tag = "community-membership-retry",
                    onAction = onRefreshMembership,
                )
            }
        }
        if (actionError != null) item { Text(actionError, color = MaterialTheme.colorScheme.error) }
        items(topics, key = { it.id }) { topic ->
            CommunityTopicCard(
                topic = topic,
                post = previews[topic.id],
                previewFailed = topic.id in previewFailures,
                currentUserId = currentUserId,
                previewLoading = previewLoading && topic.id !in previews,
                joined = topic.id in joinedIds,
                pending = topic.id in pendingTopicIds,
                membershipError = membershipError,
                onOpen = { onOpen(topic) },
                onToggleMembership = { onToggleMembership(topic) },
            )
        }
        item {
            if (previewLoading) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(" Loading recent posts…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                TextButton(onClick = onRetry, modifier = Modifier.testTag("community-refresh")) { Text("Refresh communities") }
            }
        }
    }
}

@Composable
private fun CommunityTopicCard(
    topic: CommunityTopic,
    post: CommunityPost?,
    previewFailed: Boolean,
    currentUserId: String?,
    previewLoading: Boolean,
    joined: Boolean,
    pending: Boolean,
    membershipError: Boolean,
    onOpen: () -> Unit,
    onToggleMembership: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().testTag("community-card-${topic.slug}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                    androidx.compose.material3.Icon(
                        Icons.Outlined.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp).size(24.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(topic.title, style = MaterialTheme.typography.titleMedium)
                    Text(topic.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (joined) {
                    OutlinedButton(onClick = onToggleMembership, enabled = !pending && !membershipError, modifier = Modifier.testTag("community-leave-${topic.slug}")) {
                        Text(if (pending) "Saving…" else "Leave")
                    }
                } else {
                    Button(onClick = onToggleMembership, enabled = !pending && !membershipError, modifier = Modifier.testTag("community-join-${topic.slug}")) {
                        Text(if (pending) "Saving…" else "Join")
                    }
                }
            }
            HorizontalDivider()
            when {
                previewLoading -> Text("Loading recent post…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                previewFailed -> Text("Recent post could not load. Open the discussion to retry.", color = MaterialTheme.colorScheme.error)
                post != null -> {
                    Text(post.body.take(180).let { if (post.body.length > 180) "$it…" else it }, style = MaterialTheme.typography.bodyMedium)
                    Text("${if (post.authorId == currentUserId) "You" else "TNHC member"} · ${post.createdAt.take(10)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> Text("No posts yet. Start the first conversation.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onOpen, modifier = Modifier.testTag("community-topic-${topic.slug}")) {
                Text("Open discussion")
            }
        }
    }
}

@Composable
private fun CommunityTopicFeed(
    topic: CommunityTopic,
    posts: List<CommunityPost>,
    currentUserId: String?,
    joined: Boolean,
    signedIn: Boolean,
    loading: Boolean,
    error: Boolean,
    pending: Boolean,
    membershipError: Boolean,
    actionError: String?,
    postBody: String,
    posting: Boolean,
    canPost: Boolean,
    activeMemberChecked: Boolean,
    activeMemberCheckError: Boolean,
    onBack: () -> Unit,
    onToggleMembership: () -> Unit,
    onPostBody: (String) -> Unit,
    onSubmitPost: () -> Unit,
    onRetry: () -> Unit,
    onRetryMembership: () -> Unit,
    onRetryAccess: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("community-feed"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TextButton(onClick = onBack, modifier = Modifier.testTag("community-back")) {
                androidx.compose.material3.Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                Text(" All communities")
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(topic.title, style = MaterialTheme.typography.headlineSmall)
                    Text(topic.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (joined) {
                        OutlinedButton(onClick = onToggleMembership, enabled = !pending, modifier = Modifier.testTag("community-leave")) {
                            Text(if (pending) "Saving…" else "Leave community")
                        }
                    } else {
                        Button(onClick = onToggleMembership, enabled = !pending && !membershipError, modifier = Modifier.testTag("community-join")) {
                            Text(if (pending) "Saving…" else if (signedIn) "Join community" else "Sign in to join")
                        }
                    }
                }
            }
        }
        item { Text("Recent posts", style = MaterialTheme.typography.titleLarge) }
        if (error) {
            item { CommunityNotice("Posts could not load.", "Retry", "community-feed-retry", onRetry) }
        } else if (loading) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(" Loading discussion…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (posts.isEmpty()) {
            item {
                Text(
                    if (joined) "No posts yet. Start the first conversation below." else "No posts yet in this community.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(posts, key = { it.id }) { post -> CommunityPostCard(post, currentUserId) }
        }
        if (membershipError) item { CommunityNotice("Your membership could not load.", "Retry", "community-membership-retry", onRetryMembership) }
        if (joined && signedIn && canPost) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Start a conversation", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = postBody,
                            onValueChange = onPostBody,
                            modifier = Modifier.fillMaxWidth().testTag("community-post-composer"),
                            label = { Text("Share an update or question") },
                            minLines = 3,
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        )
                        Text("Posts can be up to 5,000 characters.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = onSubmitPost,
                            enabled = !posting && postBody.isNotBlank() && postBody.trim().length <= 5_000,
                            modifier = Modifier.fillMaxWidth().testTag("community-post-submit"),
                        ) { Text(if (posting) "Publishing…" else "Publish post") }
                    }
                }
            }
        } else if (joined && signedIn) {
            item {
                if (!activeMemberChecked) {
                    Text("Checking account access…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (activeMemberCheckError) {
                    CommunityNotice("Posting access could not be checked.", "Retry", "community-access-retry", onRetryAccess)
                } else {
                    Text("Your account is not active for posting right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (actionError != null) item { Text(actionError, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun CommunityPostCard(post: CommunityPost, currentUserId: String?) {
    Card(Modifier.fillMaxWidth().testTag("community-post-${post.id}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                    androidx.compose.material3.Icon(Icons.Outlined.Groups, contentDescription = null, modifier = Modifier.padding(8.dp).size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text("${if (post.authorId == currentUserId) "You" else "TNHC member"} · ${post.createdAt.take(10)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(post.body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CommunityMessage(title: String, message: String, action: String, tag: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAction, modifier = Modifier.testTag(tag)) { Text(action) }
    }
}

@Composable
private fun CommunityLoading() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("Loading communities…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CommunityNotice(message: String, action: String, tag: String, onAction: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onAction, modifier = Modifier.testTag(tag)) { Text(action) }
        }
    }
}
