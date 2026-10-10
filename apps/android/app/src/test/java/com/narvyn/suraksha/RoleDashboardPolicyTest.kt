package com.narvyn.suraksha
import org.junit.Assert.*
import org.junit.Test
class RoleDashboardPolicyTest {
 @Test fun operationalRolesNeverDefaultToLearning(){for(role in listOf("platform","admin","trainer","examiner","certifier","assessor","auditor","author","reviewer","viewer","supporter"))assertFalse(role,RoleDashboardPolicy.learningRole(role));assertTrue(RoleDashboardPolicy.learningRole("learner"));assertTrue(RoleDashboardPolicy.learningRole("personal"))}
 @Test fun auditorAndContentHaveNoLegacyLearnerRecords(){for(role in listOf("platform","auditor","author","reviewer"))assertTrue(RoleDashboardPolicy.legacySections(role).isEmpty());assertFalse("team" in RoleDashboardPolicy.legacySections("examiner"));assertTrue("team" in RoleDashboardPolicy.legacySections("admin"));assertFalse("records" in RoleDashboardPolicy.legacySections("certifier"))}
 @Test fun browserDestinationsStayRelative(){for(path in listOf("https://evil.test","//evil.test","/\\evil","/account\n"))assertFalse(RoleDashboardPolicy.localPath(path));assertTrue(RoleDashboardPolicy.localPath("/awards?learner=1"))}
}
