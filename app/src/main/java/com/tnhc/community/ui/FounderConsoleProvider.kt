package com.tnhc.community.ui

import androidx.compose.runtime.Composable
import com.tnhc.community.data.SupabaseCommunityRemoteDataSource

/** Flavor boundary: the member APK has this contract but no Founder implementation. */
interface FounderConsoleContent {
    @Composable
    fun Content(remote: SupabaseCommunityRemoteDataSource)
}
