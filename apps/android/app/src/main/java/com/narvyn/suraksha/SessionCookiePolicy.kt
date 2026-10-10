package com.narvyn.suraksha

/** Compare-and-set cookie updates preserve a newer concurrent rotation. */
internal object SessionCookiePolicy {
    data class Update(val name:String,val value:String?,val expires:Long)
    fun accepted(request:Map<String,String>,current:Map<String,String>,updates:List<Update>):List<Update> =
        updates.filter{request[it.name]==current[it.name]}
    fun tokens(cookies:Map<String,String>)=cookies.filterKeys{it.endsWith("session_token")}
    fun canClearUnauthorized(request:Map<String,String>,current:Map<String,String>)=tokens(request)==tokens(current)
    fun becameAuthenticated(request:Map<String,String>,current:Map<String,String>)=tokens(request).isEmpty()&&tokens(current).isNotEmpty()
}
