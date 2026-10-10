package com.narvyn.suraksha
/** Presentation constraints supplement, never replace, server authorisation. */
internal object RoleDashboardPolicy {
 fun learningRole(role:String)=role in setOf("personal","learner")
 fun legacySections(role:String)=when(role){
  "admin"->setOf("workers","records","assignments","credentials","certification","team","settings","audit","rooms","verify","curriculum")
  "trainer"->setOf("workers","records","assignments","rooms","verify","curriculum")
  "examiner"->setOf("records","rooms","verify")
  "certifier"->setOf("credentials","certification","verify")
  "assessor"->setOf("records","rooms","credentials","certification","verify")
  "viewer"->setOf("workers","records","assignments","credentials","rooms","verify")
  else->emptySet()
 }
 fun localPath(path:String)=path.startsWith("/")&&!path.startsWith("//")&&!path.contains('\\')&&!path.contains('\n')&&!path.contains('\r')
}
