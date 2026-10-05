package com.tnhc.community.data

import com.tnhc.community.BuildConfig

data class BackendConfig(val url: String, val publishableKey: String) {
    val isConfigured: Boolean get() = url.isNotBlank() && publishableKey.isNotBlank()

    fun validateForRelease() {
        require(url.startsWith("https://")) { "Production backend URL must use HTTPS" }
        require(publishableKey.isNotBlank()) { "Supabase publishable key is missing" }
    }

    companion object {
        fun fromBuildConfig() = BackendConfig(BuildConfig.BACKEND_URL.trim(), BuildConfig.PUBLISHABLE_KEY.trim())
    }
}
