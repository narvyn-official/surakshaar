package com.narvyn.suraksha

/** Pure queue boundary, including upgrades from the old unscoped transfer format. */
internal object LearningSyncScope {
    fun requireSame(user:String,origin:String,worker:String,owner:String,pendingUser:String,pendingOrigin:String,pendingWorker:String,pendingOwner:String?) {
        require(owner.isNotBlank()) { "Choose an institution in your account before syncing." }
        require(user==pendingUser&&origin==pendingOrigin&&worker==pendingWorker) { "Queued records belong to a different account, server or learner." }
        require(!pendingOwner.isNullOrBlank()&&owner==pendingOwner) { "The pending transfer belongs to another or an older unscoped institution. Reset its transfer copy in Industrial records; your local learning is retained." }
    }
}
