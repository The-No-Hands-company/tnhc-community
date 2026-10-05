package com.tnhc.community.ui

import kotlinx.coroutines.CancellationException

sealed interface FounderAccess {
    data object Loading : FounderAccess
    data object Denied : FounderAccess
    data object Allowed : FounderAccess
}

suspend fun resolveFounderAccess(
    founderBuild: Boolean,
    signedIn: Boolean,
    serverRoleCheck: suspend () -> Boolean,
): FounderAccess {
    if (!founderBuild || !signedIn) return FounderAccess.Denied
    return try {
        if (serverRoleCheck()) FounderAccess.Allowed else FounderAccess.Denied
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        FounderAccess.Denied
    }
}
