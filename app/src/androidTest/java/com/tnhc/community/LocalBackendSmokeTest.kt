package com.tnhc.community

import androidx.test.platform.app.InstrumentationRegistry
import android.content.Intent
import android.net.Uri
import com.tnhc.community.data.BackendConfig
import com.tnhc.community.data.DefaultCommunityRepository
import com.tnhc.community.data.SupabaseCommunityRemoteDataSource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalBackendSmokeTest {
    @Test fun configuredEmulatorBuildLoadsProjectsFromLocalSupabase() = runBlocking {
        val config = BackendConfig.fromBuildConfig()
        assumeTrue("This smoke test requires a backend-configured debug build", config.isConfigured)
        val repository = DefaultCommunityRepository(
            SupabaseCommunityRemoteDataSource(
                InstrumentationRegistry.getInstrumentation().targetContext.applicationContext,
                config,
            ),
        )
        val page = repository.loadProjects()
        assertTrue("The local seeded project catalogue should be reachable", page.projects.isNotEmpty())
        assertFalse("Backend records must be distinguished from offline demo fixtures", page.projects.first().isDemo)
    }

    // Run accept and restore in separate instrumentation processes to exercise disk persistence.
    @Test fun invitedSessionAndFollowSurviveProcessRestart() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val phase = arguments.getString("localBackendPhase")
        assumeTrue("Requires the local invitation fixture", phase == "accept" || phase == "restore")
        val memberId = requireNotNull(arguments.getString("localBackendMemberId"))
        val config = BackendConfig.fromBuildConfig()
        assertTrue(config.isConfigured)
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val repository = DefaultCommunityRepository(SupabaseCommunityRemoteDataSource(context, config))
        if (phase == "accept") {
            val link = requireNotNull(arguments.getString("localBackendInviteUri"))
            repository.handleAuthLink(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        }
        val session = withTimeout(15_000) { repository.session.filterNotNull().first() }
        assertEquals(memberId, session.userId)
        val projectId = repository.loadProjects().projects.first().id
        if (phase == "accept") {
            repository.setPassword("local-test-only-passphrase-2026")
            repeat(2) {
                repository.updateProfile("Local Test Member", "Synthetic test", listOf("Testing"), "private")
                repository.setProjectFollow(projectId, true)
            }
        }
        assertEquals("Local Test Member", repository.loadProfile().displayName)
        assertTrue(repository.loadFollowedProjectIds().contains(projectId))
        if (phase == "restore") {
            repeat(2) { repository.setProjectFollow(projectId, false) }
            assertFalse(repository.loadFollowedProjectIds().contains(projectId))
            repository.signOut()
            assertTrue(context.getSharedPreferences("supabase-auth-session", 0).all.isEmpty())
        }
    }
}
