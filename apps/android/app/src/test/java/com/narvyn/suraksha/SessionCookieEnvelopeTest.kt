package com.narvyn.suraksha

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SessionCookieEnvelopeTest {
    private val local="http://localhost:5173"
    private val hosted="https://surakshaar.213-210-36-82.sslip.io"
    private fun jar()=JSONObject().put("better-auth.session_token",JSONObject().put("value","disposable-test-token").put("expires",Long.MAX_VALUE))
    @Test fun legacyUnboundJarIsNeverMigratedAcrossDefaultOriginChange(){
        assertNull(SessionCookieEnvelope.read(jar(),hosted))
        assertNull(SessionCookieEnvelope.read(jar(),local))
        assertNull(SessionCookieEnvelope.read(JSONObject().put("version",1).put("cookies",jar()),hosted))
    }
    @Test fun onlyExactBoundOriginCanReadPersistedSession(){
        val envelope=SessionCookieEnvelope.create(local,jar())
        assertNull(SessionCookieEnvelope.read(envelope,hosted))
        assertNull(SessionCookieEnvelope.read(envelope,"https://localhost:5173"))
        assertNull(SessionCookieEnvelope.read(envelope,"http://localhost:5174"))
        assertEquals("disposable-test-token",SessionCookieEnvelope.read(envelope,local)!!.getJSONObject("better-auth.session_token").getString("value"))
    }
    @Test fun malformedAndUnknownVersionsFailClosedAndJarIsCopied(){
        val envelope=SessionCookieEnvelope.create(hosted,jar())
        SessionCookieEnvelope.read(envelope,hosted)!!.remove("better-auth.session_token")
        assertEquals(1,SessionCookieEnvelope.read(envelope,hosted)!!.length())
        assertNull(SessionCookieEnvelope.read(envelope.put("version",2),hosted))
        assertNull(SessionCookieEnvelope.read(JSONObject().put("version",1).put("origin",hosted).put("cookies","bad"),hosted))
    }
}
