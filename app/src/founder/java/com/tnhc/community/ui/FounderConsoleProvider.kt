package com.tnhc.community.ui

import androidx.compose.runtime.Composable
import com.tnhc.community.BuildConfig
import com.tnhc.community.data.DefaultFounderRepository
import com.tnhc.community.data.SupabaseCommunityRemoteDataSource
import com.tnhc.community.data.SupabaseFounderRemoteDataSource
import java.net.URI

class FounderConsoleProvider : FounderConsoleContent {
    @Composable
    override fun Content(remote: SupabaseCommunityRemoteDataSource) {
        val localEmailCapture = BuildConfig.DEBUG && runCatching {
            URI(BuildConfig.BACKEND_URL).host in setOf("127.0.0.1", "localhost", "10.0.2.2")
        }.getOrDefault(false)
        FounderConsoleScreen(
            DefaultFounderRepository(SupabaseFounderRemoteDataSource(remote.client)),
            localEmailCapture = localEmailCapture,
        )
    }
}
