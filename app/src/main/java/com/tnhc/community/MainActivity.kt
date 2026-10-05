package com.tnhc.community

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tnhc.community.ui.CommunityApp
import com.tnhc.community.ui.TnhcTheme
import com.tnhc.community.data.BackendConfig
import com.tnhc.community.data.DefaultCommunityRepository
import com.tnhc.community.data.SupabaseCommunityRemoteDataSource
import com.tnhc.community.ui.FounderConsoleContent
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val backendConfig = BackendConfig.fromBuildConfig()
        val remote = if (backendConfig.isConfigured) SupabaseCommunityRemoteDataSource(applicationContext, backendConfig) else null
        val communityRepository = remote?.let(::DefaultCommunityRepository)
        val founderProvider = if (BuildConfig.FOUNDER_BUILD && remote != null) {
            try {
                Class.forName("com.tnhc.community.ui.FounderConsoleProvider")
                    .getDeclaredConstructor().newInstance() as FounderConsoleContent
            } catch (_: Exception) {
                null
            }
        } else null
        val founderConsoleContent: @Composable () -> Unit = {
            if (founderProvider != null && remote != null) founderProvider.Content(remote)
        }
        communityRepository?.handleAuthLink(intent)
        setContent {
            TnhcTheme {
                CommunityApp(
                    communityRepository = communityRepository,
                    founderConsoleContent = founderConsoleContent,
                    founderBuild = BuildConfig.FOUNDER_BUILD,
                )
            }
        }
    }
}
