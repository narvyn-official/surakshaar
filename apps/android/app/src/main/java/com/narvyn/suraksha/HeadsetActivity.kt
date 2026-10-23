package com.narvyn.suraksha

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Compatibility route for older internal links. Opens camera-AR consent, never a camera. */
class HeadsetActivity:Activity(){
    override fun onCreate(saved:Bundle?){
        super.onCreate(saved)
        val next=Intent(this,HandsArActivity::class.java).putExtra("headset",true)
        intent.getStringExtra("scenarioId")?.let{next.putExtra("scenarioId",it)}
        startActivity(next);finish()
    }
}
