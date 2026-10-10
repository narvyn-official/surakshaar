package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class SessionCookiePolicyTest {
    private val token="better-auth.session_token"
    private fun update(value:String?)=SessionCookiePolicy.Update(token,value,Long.MAX_VALUE)
    @Test fun delayedOldTokenRefreshCannotOverwritePasswordChangeToken(){
        val old=mapOf(token to "T1");val current=mapOf(token to "T2")
        assertTrue(SessionCookiePolicy.accepted(old,current,listOf(update("T1"))).isEmpty())
        assertFalse(SessionCookiePolicy.canClearUnauthorized(old,current))
    }
    @Test fun pendingAnonymousResponseCannotClearOrDeleteCompletedLogin(){
        val before=emptyMap<String,String>();val after=mapOf(token to "new-login")
        assertFalse(SessionCookiePolicy.canClearUnauthorized(before,after))
        assertTrue(SessionCookiePolicy.accepted(before,after,listOf(update(null))).isEmpty())
        assertTrue(SessionCookiePolicy.becameAuthenticated(before,after))
    }
    @Test fun legitimateRefreshAndRealExpiredSessionStillWork(){
        val current=mapOf(token to "same-account")
        assertEquals(listOf(update("refreshed")),SessionCookiePolicy.accepted(current,current,listOf(update("refreshed"))))
        assertTrue(SessionCookiePolicy.canClearUnauthorized(current,current))
        assertFalse(SessionCookiePolicy.becameAuthenticated(current,mapOf(token to "rotated")))
        assertEquals(listOf(update(null)),SessionCookiePolicy.accepted(current,current,listOf(update(null))))
    }
    @Test fun secureCookieNamesAndUnrelatedUpdatesRemainIsolated(){
        val secure="__Secure-better-auth.session_token"
        assertFalse(SessionCookiePolicy.canClearUnauthorized(mapOf(secure to "old"),mapOf(secure to "new")))
        val old=mapOf(token to "old","preferences" to "a");val current=mapOf(token to "new","preferences" to "a")
        val preference=SessionCookiePolicy.Update("preferences","b",42)
        assertEquals(listOf(preference),SessionCookiePolicy.accepted(old,current,listOf(update("old"),preference)))
    }
}
