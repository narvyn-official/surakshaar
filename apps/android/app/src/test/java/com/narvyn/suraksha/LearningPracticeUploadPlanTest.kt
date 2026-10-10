package com.narvyn.suraksha

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class LearningPracticeUploadPlanTest {
    private val worker = "8585be99-de62-4414-b5dc-5b934a7135d3"
    private fun record(text: String = "") = JSONObject().put("id", UUID.randomUUID().toString())
        .put("workerId", worker).put("updatedAt", 1234L).put("reflection", text)
    private fun receipt(batch: JSONObject) = JSONObject().put("schemaVersion", 1)
        .put("evidenceType", "formative-decision-practice").put("certifiable", false)
        .put("accepted", JSONArray(batch.getJSONArray("attempts").objects().map {
            JSONObject().put("id", it.getString("id")).put("updatedAt", it.getLong("updatedAt"))
                .put("digest", "a".repeat(64))
        }))

    @Test fun countLimitPreservesEveryRecordInOrderWithoutEmptyRequests() {
        val records = List(41) { record() }
        val batches = LearningPracticeUploadPlan.batches(worker, records)
        assertEquals(listOf(20, 20, 1), batches.map { it.getJSONArray("attempts").length() })
        assertEquals(records.map { it.getString("id") }, batches.flatMap {
            it.getJSONArray("attempts").objects().map { row -> row.getString("id") }
        })
        assertTrue(LearningPracticeUploadPlan.batches(worker, emptyList()).isEmpty())
    }

    @Test fun unicodePayloadUsesUtf8BytesRatherThanCharacterCount() {
        val records = List(7) { record("अ".repeat(50_000)) }
        assertTrue(records.first().toString().length < 60_000)
        assertTrue(LearningPracticeUploadPlan.bytes(records.first()) > 150_000)
        val batches = LearningPracticeUploadPlan.batches(worker, records)
        assertTrue(batches.size > 1)
        assertEquals(7, batches.sumOf { it.getJSONArray("attempts").length() })
        assertTrue(batches.all { LearningPracticeUploadPlan.bytes(it) <= LearningPracticeUploadPlan.MAX_BODY_BYTES })
    }

    @Test fun planSnapshotsAreIndependentOfSubsequentLocalEdits() {
        val original = record("Before").put("answers", JSONArray().put(JSONObject().put("choice", "original")))
        val batch = LearningPracticeUploadPlan.batches(worker, listOf(original)).single()
        original.put("reflection", "After").getJSONArray("answers").getJSONObject(0).put("choice", "changed")
        val snapshot = batch.getJSONArray("attempts").getJSONObject(0)
        assertEquals("Before", snapshot.getString("reflection"))
        assertEquals("original", snapshot.getJSONArray("answers").getJSONObject(0).getString("choice"))
    }

    @Test fun rejectsMixedProfileDuplicateIdentityAndOversizeRecords() {
        assertThrows(IllegalArgumentException::class.java) {
            LearningPracticeUploadPlan.batches(worker, listOf(record().put("workerId", UUID.randomUUID().toString())))
        }
        val one = record()
        assertThrows(IllegalArgumentException::class.java) { LearningPracticeUploadPlan.batches(worker, listOf(one, one)) }
        assertThrows(IllegalArgumentException::class.java) { LearningPracticeUploadPlan.batches(worker, listOf(record("अ".repeat(70_000)))) }
    }

    @Test fun receiptMustConfirmExactlyTheSentRecordsAndVersions() {
        val batch = LearningPracticeUploadPlan.batches(worker, listOf(record(), record())).single()
        LearningPracticeUploadPlan.verifyReply(receipt(batch), batch)
        val missing = receipt(batch).apply { getJSONArray("accepted").remove(0) }
        val duplicate = receipt(batch).apply { getJSONArray("accepted").put(1, getJSONArray("accepted").getJSONObject(0)) }
        val other = receipt(batch).apply { getJSONArray("accepted").getJSONObject(0).put("id", UUID.randomUUID().toString()) }
        val old = receipt(batch).apply { getJSONArray("accepted").getJSONObject(0).put("updatedAt", 1233L) }
        for (invalid in listOf(missing, duplicate, other, old)) {
            assertThrows(IllegalArgumentException::class.java) { LearningPracticeUploadPlan.verifyReply(invalid, batch) }
        }
    }

    @Test fun receiptCannotUpgradeEvidenceOrOmitIntegrityData() {
        val batch = LearningPracticeUploadPlan.batches(worker, listOf(record())).single()
        val certifiable = receipt(batch).put("certifiable", true)
        val grade = receipt(batch).put("evidenceType", "practical-competence")
        val invalidDigest = receipt(batch).apply { getJSONArray("accepted").getJSONObject(0).put("digest", "accepted") }
        for (invalid in listOf(certifiable, grade, invalidDigest)) {
            assertThrows(IllegalArgumentException::class.java) { LearningPracticeUploadPlan.verifyReply(invalid, batch) }
        }
    }
}
