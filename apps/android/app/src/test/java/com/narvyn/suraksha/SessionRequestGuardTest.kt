package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch

class SessionRequestGuardTest {
    @Test fun lateUnauthorizedResponseCannotClearReplacementAccount(){
        val guard=SessionRequestGuard();var cookie="account-a";val old=guard.capture()
        guard.replace{cookie="account-b"}
        assertFalse(guard.apply(old){guard.replace{cookie=""}})
        assertEquals("account-b",cookie)
    }
    @Test fun cookieRefreshKeepsInFlightSameAccountUploadsCurrent(){
        val guard=SessionRequestGuard();val upload=guard.capture();var cookie="original"
        assertTrue(guard.apply(upload){cookie="rotated-same-account"})
        assertTrue(guard.isCurrent(upload));assertEquals("rotated-same-account",cookie)
    }
    @Test fun olderLoginResponseCannotOverwriteNewLogin(){
        val guard=SessionRequestGuard();guard.replace{};val firstLogin=guard.capture()
        guard.replace{};val secondLogin=guard.capture();var identity=""
        assertTrue(guard.apply(secondLogin){identity="second"})
        assertFalse(guard.apply(firstLogin){identity="first"});assertEquals("second",identity)
    }
    @Test fun responseApplicationAndAccountReplacementAreSerialized(){
        val guard=SessionRequestGuard();val epoch=guard.capture();val entered=CountDownLatch(1);val release=CountDownLatch(1);var identity="old"
        val response=Thread{guard.apply(epoch){entered.countDown();release.await();identity="response"}}.apply{start()}
        entered.await();val replacement=Thread{guard.replace{identity="new"}}.apply{start()}
        release.countDown();response.join();replacement.join()
        assertEquals("new",identity);assertFalse(guard.isCurrent(epoch))
    }
    @Test fun originAndCookieSnapshotCannotStraddleServerReplacement(){
        val guard=SessionRequestGuard();var origin="server-a";var cookie="token-a"
        val entered=CountDownLatch(1);val release=CountDownLatch(1);var captured:Pair<String,String>?=null
        val request=Thread{captured=guard.snapshot{val url=origin;entered.countDown();release.await();url to cookie}}.apply{start()}
        entered.await();val replacement=Thread{guard.replace{origin="server-b";cookie="token-b"}}.apply{start()}
        release.countDown();request.join();replacement.join()
        assertEquals("server-a" to "token-a",captured);assertEquals("server-b",origin)
    }
    @Test fun replacementBetweenPreviewAndDispatchCannotSendWithNewAccount(){
        val guard=SessionRequestGuard();val preview=guard.capture();var dispatched=false
        val ready=CountDownLatch(1);val proceed=CountDownLatch(1);var rejected=false
        val upload=Thread{ready.countDown();proceed.await();try{guard.snapshot(preview){dispatched=true}}catch(_:SessionRequestGuard.Replaced){rejected=true}}.apply{start()}
        ready.await();guard.replace{};proceed.countDown();upload.join()
        assertTrue(rejected);assertFalse(dispatched)
    }
    @Test fun loginCompletionInvalidatesAnonymousRequestsWhileTokenRotationDoesNot(){
        val guard=SessionRequestGuard();guard.replace{};val anonymous=guard.capture()
        guard.apply(anonymous){guard.replace{}}
        assertFalse(guard.apply(anonymous){fail("Anonymous response survived completed login")})
        val signedIn=guard.capture();assertTrue(guard.apply(signedIn){});assertTrue(guard.isCurrent(signedIn))
    }
}
