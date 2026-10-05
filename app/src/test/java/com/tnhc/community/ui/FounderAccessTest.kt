package com.tnhc.community.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FounderAccessTest {
    @Test fun memberBuildNeverRequestsFounderRole() = runBlocking {
        val access = resolveFounderAccess(founderBuild = false, signedIn = true) {
            error("Member build must not query Founder access")
        }
        assertEquals(FounderAccess.Denied, access)
    }

    @Test fun signedOutFounderBuildDoesNotRequestFounderRole() = runBlocking {
        val access = resolveFounderAccess(founderBuild = true, signedIn = false) {
            error("Signed-out app must not query Founder access")
        }
        assertEquals(FounderAccess.Denied, access)
    }

    @Test fun serverConfirmedFounderGetsAccess() = runBlocking {
        assertEquals(FounderAccess.Allowed, resolveFounderAccess(true, true) { true })
    }

    @Test fun regularAccountInFounderBuildIsDenied() = runBlocking {
        assertEquals(FounderAccess.Denied, resolveFounderAccess(true, true) { false })
    }

    @Test fun roleRequestFailureFailsClosed() = runBlocking {
        assertEquals(FounderAccess.Denied, resolveFounderAccess(true, true) { error("offline") })
    }

    @Test fun cancellationIsNotConvertedIntoDenial() {
        assertThrows(CancellationException::class.java) {
            runBlocking { resolveFounderAccess(true, true) { throw CancellationException("cancelled") } }
        }
    }
}
