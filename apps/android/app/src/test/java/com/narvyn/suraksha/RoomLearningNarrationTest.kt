package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class RoomLearningNarrationTest {
    @Test fun spokenTaskDoesNotReadUnsafeDistractorsAsDirections() {
        val page=RoomLearningDialog.Page("Inspect the scene","Keep a clear exit.","What should you do?",0,
            listOf(RoomLearningDialog.Choice("Block the exit","Keep exits clear.",false)))
        val speech=RoomLearningDialog.narration(page,false)
        assertFalse(speech.contains("Block the exit"))
        assertTrue(speech.contains("Keep a clear exit."))
        assertTrue(speech.endsWith("Choose an option on screen."))
    }
    @Test fun feedbackSpeaksItsExplanationWithoutRepeatingTheRejectedCommand() {
        val page=RoomLearningDialog.Page("Pull it out","Leave an embedded object in place.",tint=0,speakTitle=false)
        assertEquals("Leave an embedded object in place.",RoomLearningDialog.narration(page,false))
    }
    @Test fun hindiTaskRetainsAuthoredInstructionsAndSelectedLanguage() {
        val page=RoomLearningDialog.Page("पहले जाँचें","बाहर निकलने का रास्ता खुला रखें।",tint=0,
            choices=listOf(RoomLearningDialog.Choice("रास्ता रोकें","रास्ता खुला रखें।",false)))
        val speech=RoomLearningDialog.narration(page,true)
        assertFalse(speech.contains("रास्ता रोकें"));assertTrue(speech.endsWith("स्क्रीन पर एक विकल्प चुनें।"))
        assertTrue(speech.contains("बाहर निकलने का रास्ता खुला रखें।"))
    }
}
