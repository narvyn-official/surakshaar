package com.narvyn.suraksha

import org.junit.Test
import org.junit.Assert.*
class LearningSyncScopeTest {
    private fun check(user:String="u",origin:String="https://service",worker:String="w",owner:String?="a")=LearningSyncScope.requireSame("u","https://service","w","a",user,origin,worker,owner)
    @Test fun sameInstitutionRetriesRemainValid(){check()}
    @Test fun institutionSwitchCannotRetargetPendingCopy(){assertThrows(IllegalArgumentException::class.java){check(owner="b")}}
    @Test fun upgradeCannotGuessInstitutionForLegacyQueue(){assertThrows(IllegalArgumentException::class.java){check(owner=null)};assertThrows(IllegalArgumentException::class.java){check(owner="")}}
    @Test fun queueRemainsAccountOriginAndProfileBound(){assertThrows(IllegalArgumentException::class.java){check(user="other")};assertThrows(IllegalArgumentException::class.java){check(origin="https://other")};assertThrows(IllegalArgumentException::class.java){check(worker="other")}}
}
