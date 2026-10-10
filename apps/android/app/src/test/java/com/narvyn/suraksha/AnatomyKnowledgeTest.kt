package com.narvyn.suraksha

import java.io.File
import java.net.URI
import java.util.zip.ZipFile
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AnatomyKnowledgeTest {
    private fun contentFile(path: String): File = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
        .map { File(it, "content/$path") }.firstOrNull { it.isFile }
        ?: error("Missing bundled content/$path")

    private fun partIds(json: String): Set<String> {
        val parts = JSONObject(json).getJSONArray("parts")
        return (0 until parts.length()).map { parts.getJSONObject(it).getString("id") }.toSet()
    }

    @Test fun everyBalancedAndDetailedMeshHasAnExactDescription() {
        val balanced = partIds(contentFile("anatomy/manifest.json").readText())
        val detailed = ZipFile(contentFile("anatomy/optional/body-detail.zip")).use { archive ->
            partIds(archive.getInputStream(archive.getEntry("manifest.json")).bufferedReader().use { it.readText() })
        }
        assertEquals("No missing or stale description IDs for balanced anatomy", balanced, AnatomyKnowledge.knownIds)
        assertEquals("Detailed quality must describe the same actual meshes", detailed, AnatomyKnowledge.knownIds)
    }

    @Test fun heartInformationDescribesTheHeartRatherThanAFirstAidTask() {
        val heart = AnatomyKnowledge.info("FMA7088")!!
        assertEquals("Heart", heart.title)
        assertTrue(heart.description.contains("four chambers"))
        assertTrue(heart.location.contains("Between the lungs"))
        assertTrue(heart.function.contains("Pumps blood"))
        assertFalse(heart.introText.contains("compression", ignoreCase = true))
        assertFalse(heart.isGroup)
        assertNull(heart.learningNote)
    }

    @Test fun pairedStructuresRetainThePersonsLateralityAndIndividualLungAnatomy() {
        val right = AnatomyKnowledge.info("FMA7309")!!
        val left = AnatomyKnowledge.info("FMA7310")!!
        assertEquals("Right lung", right.title)
        assertEquals("Left lung", left.title)
        assertTrue(right.description.contains("three lobes"))
        assertTrue(left.description.contains("two lobes"))
        assertTrue(AnatomyKnowledge.info("FMA23130")!!.title.startsWith("Right "))
        assertTrue(AnatomyKnowledge.info("FMA23131")!!.title.startsWith("Left "))
        assertTrue(AnatomyKnowledge.info("FMA7204", "hi")!!.title.startsWith("दायाँ"))
        assertTrue(AnatomyKnowledge.info("FMA7205", "hi")!!.title.startsWith("बायाँ"))
    }

    @Test fun closelyNamedStructuresKeepTheirDifferentLocationsAndFunctions() {
        val spine = AnatomyKnowledge.info("FMA13478")!!
        val cord = AnatomyKnowledge.info("FMA7647")!!
        assertTrue(spine.description.contains("vertebrae"))
        assertTrue(cord.description.contains("nervous tissue"))
        assertTrue(cord.location.contains("distinct from the bones"))
        val shortBiceps = AnatomyKnowledge.info("FMA45891")!!
        val longBiceps = AnatomyKnowledge.info("FMA45888")!!
        assertTrue(shortBiceps.description.contains("not the hip"))
        assertFalse(shortBiceps.function.contains("extends the hip"))
        assertTrue(longBiceps.function.contains("extends the hip"))
        assertTrue(AnatomyKnowledge.info("FMA34680")!!.function.contains("forward"))
        assertTrue(AnatomyKnowledge.info("FMA34684")!!.function.contains("backward"))
    }

    @Test fun sourceRemaindersAreHonestlyDescribedAsGroups() {
        val remainders = listOf("FMA23881", "FMA9616", "FMA9617", "FMA9619", "FMA9621", "FMA9622", "FMA19086", "FMA22594")
        remainders.forEach { id ->
            val english = AnatomyKnowledge.info(id)!!
            val hindi = AnatomyKnowledge.info(id, "hi")!!
            assertTrue(id, english.isGroup)
            assertTrue(id, english.title.contains("grouped"))
            assertTrue(id, english.description.contains("individual"))
            assertTrue(id, hindi.isGroup)
            assertTrue(id, hindi.title.contains("समूह"))
        }
    }

    @Test fun allDescriptionsAreConciseAndNarrationMatchesTheSelectedPart() {
        val approvedHosts = setOf("openstax.org", "www.ncbi.nlm.nih.gov", "www.nhlbi.nih.gov", "www.niddk.nih.gov", "www.ninds.nih.gov", "dbarchive.biosciencedbc.jp")
        AnatomyKnowledge.knownIds.forEach { id ->
            for (language in listOf("en", "hi")) {
                val info = AnatomyKnowledge.info(id, language)!!
                assertEquals(language, info.language)
                assertTrue("$id $language", info.description.length in 15..140)
                assertTrue(info.location.isNotBlank())
                assertTrue(info.function.isNotBlank())
                assertTrue(info.introText.startsWith(info.title))
                assertTrue(info.introText.contains(info.description))
                assertTrue(info.introText.contains(info.location))
                assertTrue(info.introText.contains(info.function))
                assertFalse(info.introText.contains(".."))
                assertFalse(info.introText.contains("source-labelled adult anatomy"))
                assertTrue(info.sourceUrls.isNotEmpty())
                info.sourceUrls.forEach { source ->
                    val uri = URI(source)
                    assertEquals("https", uri.scheme)
                    assertTrue(uri.host, uri.host in approvedHosts)
                }
                if (language == "hi") assertTrue(info.description.any { it in '\u0900'..'\u097f' })
            }
        }
    }

    @Test fun unsupportedLanguagesAreIdentifiedAsEnglishFallbackWithoutGuessingPartNames() {
        assertEquals("hi", AnatomyKnowledge.info("FMA7088", "hi-IN")!!.language)
        assertEquals("hi", AnatomyKnowledge.info("FMA7088", "HI_in")!!.language)
        assertEquals("en", AnatomyKnowledge.info("FMA7088", "sat")!!.language)
        assertEquals("en", AnatomyKnowledge.info("FMA7088", "unknown")!!.language)
        assertNull(AnatomyKnowledge.info("heart"))
        assertNull(AnatomyKnowledge.info("FMA999999"))
        assertNull(AnatomyKnowledge.info("FMA7088 "))
        assertNull(AnatomyKnowledge.info(""))
    }
}
