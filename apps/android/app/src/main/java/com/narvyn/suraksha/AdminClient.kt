package com.narvyn.suraksha

import android.content.Context
import android.content.pm.ApplicationInfo
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.HttpCookie
import java.net.URI
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Same-origin API session. Passwords are never persisted; cookies are encrypted by Android Keystore. */
class AdminClient(private val context:Context) {
    @Volatile private var completedStamp=""
    fun completedIdentityStamp()=completedStamp
    private val expectedRequest=ThreadLocal<String?>()
    fun <T> forIdentity(stamp:String,work:()->T):T{val previous=expectedRequest.get();expectedRequest.set(stamp);return try{work()}finally{expectedRequest.set(previous)}}
    private val prefs=context.getSharedPreferences("admin_session",Context.MODE_PRIVATE)
    var base:String
        get()=prefs.getString("server",null) ?: DEFAULT_ORIGIN
        private set(value){prefs.edit().putString("server",value).apply()}
    private fun key():SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("suraksha_admin_session",null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("suraksha_admin_session",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun configure(value:String) {
        val uri=URI(value.trim());val debug=context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE!=0
        require(uri.host!=null && uri.userInfo==null && uri.query==null && uri.fragment==null && uri.path.orEmpty() in listOf("","/") && (uri.scheme=="https" || debug&&uri.scheme=="http"&&uri.host in listOf("localhost","127.0.0.1"))) { "Enter your training centre’s HTTPS server address." }
        val clean=uri.toString().trimEnd('/');if(clean!=base)sessionGuard.replace{clearTokens();base=clean}
    }
    companion object { const val DEFAULT_ORIGIN="https://surakshaar.213-210-36-82.sslip.io"; private val sessionGuard=SessionRequestGuard(); private var sessionCookies:JSONObject?=null; private var sessionOrigin:String?=null }
    fun remember(value:Boolean){prefs.edit().putBoolean("remember",value).apply();if(!value)prefs.edit().remove("cookies").apply()}
    private fun cookies():JSONObject { return try {
        if(!prefs.getBoolean("remember",true))return if(sessionOrigin==base)JSONObject(sessionCookies?.toString()?:"{}")else JSONObject()
        val saved=prefs.getString("cookies",null)
        if(saved==null)JSONObject() else {
            val parts=saved.split('.');val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)))
            val envelope=JSONObject(String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),Charsets.UTF_8))
            SessionCookieEnvelope.read(envelope,base) ?: run { prefs.edit().remove("cookies").apply(); JSONObject() }
        }
    } catch(_:Exception){prefs.edit().remove("cookies").apply();JSONObject()}
    }
    private fun saveCookies(value:JSONObject) {
        if(!prefs.getBoolean("remember",true)){sessionCookies=JSONObject(value.toString());sessionOrigin=base;return}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key())
        val encrypted=cipher.doFinal(SessionCookieEnvelope.create(base,value).toString().toByteArray(Charsets.UTF_8))
        prefs.edit().putString("cookies",Base64.encodeToString(cipher.iv,Base64.NO_WRAP)+"."+Base64.encodeToString(encrypted,Base64.NO_WRAP)).apply()
    }
    fun hasSession():Boolean { val jar=cookies();val now=System.currentTimeMillis();return jar.keys().asSequence().any{it.endsWith("session_token")&&jar.getJSONObject(it).optLong("expires",0)>now} }
    fun identityStamp()="${sessionGuard.capture()}:$base"
    private fun clearTokens(){prefs.edit().remove("cookies").apply();sessionCookies=null;sessionOrigin=null}
    fun clear(){sessionGuard.replace{clearTokens()}}
    fun call(path:String,body:JSONObject?=null,method:String=if(body==null)"GET" else "POST",expectedIdentity:String?=expectedRequest.get()):JSONObject {
        require(base.isNotBlank()){ "Connect your training centre server first." }
        require(path.startsWith("/api/")&&!path.contains(".."))
        // An explicit new login replaces local identity even if its network
        // request fails. Responses from the previous account must not survive it.
        val captured=try{sessionGuard.snapshot(expectedIdentity?.substringBefore(':')?.toLongOrNull()){
            if(expectedIdentity!=null&&identityStamp()!=expectedIdentity)throw SessionChanged()
            if(method=="POST"&&(path=="/api/auth/sign-in/email"||path=="/api/auth/sign-up/email"))clear()
            Triple(base,sessionGuard.capture(),cookies())
        }}catch(_:SessionRequestGuard.Replaced){throw SessionChanged()}
        val origin=captured.first;val generation=captured.second
        val connection=URI(origin+path).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod=method;connection.instanceFollowRedirects=false;connection.connectTimeout=12000;connection.readTimeout=20000
            connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("Origin",origin)
            val jar=captured.third;val now=System.currentTimeMillis()
            val names=jar.keys().asSequence().toList();for(name in names)if(jar.getJSONObject(name).optLong("expires",Long.MAX_VALUE)<now)jar.remove(name)
            if(jar.length()>0)connection.setRequestProperty("Cookie",jar.keys().asSequence().joinToString("; "){"$it=${jar.getJSONObject(it).getString("value")}"})
            if(body!=null){connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json");connection.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}}
            val status=connection.responseCode
            val changedCookies=mutableListOf<HttpCookie>()
            for((name,values) in connection.headerFields)if(name.equals("Set-Cookie",true))for(raw in values)for(cookie in HttpCookie.parse(raw)) {
                changedCookies.add(cookie)
            }
            val stream=if(status in 200..299)connection.inputStream else connection.errorStream
            val bytes=stream?.use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(out.size()<=4_000_000){val count=input.read(buffer);if(count<0)break;out.write(buffer,0,count)};out.toByteArray()}?:byteArrayOf();require(bytes.size<=4_000_000){"Response is too large. Refine the records shown."}
            val data=try{JSONObject(String(bytes,Charsets.UTF_8))}catch(_:Exception){
                if(status in 200..299)throw IllegalStateException("The service returned an unreadable response. Check your server address and try again.")
                JSONObject()
            }
            if(!sessionGuard.isCurrent(generation)||origin!=base)throw SessionChanged()
            if(status==401){if(!sessionGuard.apply(generation){if(!SessionCookiePolicy.canClearUnauthorized(cookieValues(jar,now),cookieValues(cookies(),now)))throw SessionChanged();clear()})throw SessionChanged();if(path.startsWith("/api/auth/sign-in"))throw IllegalStateException("Check your email and password, then try again.");throw SignedOut()}
            if(!sessionGuard.apply(generation){
                // Merge only returned cookies. An unrelated response must not roll
                // back a newer token rotation or resurrect a cleared cookie.
                val fresh=cookies();val oldWorkspace=fresh.optJSONObject("suraksha_workspace")?.optString("value")
                val updates=changedCookies.map{cookie->SessionCookiePolicy.Update(cookie.name,if(cookie.hasExpired())null else cookie.value,if(cookie.maxAge<0)Long.MAX_VALUE else now+cookie.maxAge.coerceAtMost(31536000)*1000)}
                for(cookie in SessionCookiePolicy.accepted(cookieValues(jar,now),cookieValues(fresh,now),updates))if(cookie.value==null)fresh.remove(cookie.name)else fresh.put(cookie.name,JSONObject().put("value",cookie.value).put("expires",cookie.expires))
                if(changedCookies.isNotEmpty())saveCookies(fresh)
                if(SessionCookiePolicy.becameAuthenticated(cookieValues(jar,now),cookieValues(fresh,now))||oldWorkspace!=fresh.optJSONObject("suraksha_workspace")?.optString("value")||method=="POST"&&path=="/api/auth/sign-out")sessionGuard.replace{}
                completedStamp=identityStamp()
            })throw SessionChanged()
            if(status==429)throw IllegalStateException("Too many attempts. Wait a minute before trying again.")
            if(status==503&&path=="/api/auth/request-password-reset")throw IllegalStateException("Password recovery is unavailable. Contact your institution; your password has not changed.")
            if(status>=500)throw IllegalStateException("The account service is unavailable. Try again later or contact your training centre.")
            if(status !in 200..299)throw IllegalStateException(data.optString("message",data.optString("error","Could not complete the request ($status).")))
            return data
        } finally {connection.disconnect()}
    }
    private fun cookieValue(jar:JSONObject,name:String,now:Long)=jar.optJSONObject(name)?.takeIf{it.optLong("expires",0)>now}?.optString("value")
    private fun cookieValues(jar:JSONObject,now:Long)=jar.keys().asSequence().mapNotNull{name->cookieValue(jar,name,now)?.let{name to it}}.toMap()
    class SignedOut:Exception("Your session has ended. Sign in again.")
    class SessionChanged:Exception("The signed-in account changed. Open this screen again.")
}
