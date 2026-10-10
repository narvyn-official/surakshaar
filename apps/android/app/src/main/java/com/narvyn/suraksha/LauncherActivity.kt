package com.narvyn.suraksha
import android.app.Activity
import android.content.Intent
import android.os.Bundle
/** Signed-in accounts enter their role workspace; guests enter offline learning. */
class LauncherActivity:Activity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);startActivity(Intent(this,if(AdminClient(this).hasSession())RoleDashboardActivity::class.java else MainActivity::class.java));finish()}}
