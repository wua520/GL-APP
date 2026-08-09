package com.fitness.training.network.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalWriteReferenceTest {

    private val gson = Gson()

    @Test
    fun trainingPlanReferenceUsesTypedSingleIdContract() {
        val reference = LocalWriteReference.trainingPlan(42L)
        val parsed = gson.fromJson(reference, LocalWriteReference::class.java)

        assertEquals("training_plan", parsed.type)
        assertEquals(listOf(42L), parsed.ids)
    }

    @Test
    fun dietRecordsReferencePreservesDraftOrder() {
        val reference = LocalWriteReference.dietRecords(listOf(8L, 3L, 11L))
        val parsed = gson.fromJson(reference, LocalWriteReference::class.java)

        assertEquals("diet_records", parsed.type)
        assertEquals(listOf(8L, 3L, 11L), parsed.ids)
    }

    @Test
    fun referencesAreJsonObjectsWithNonEmptyIds() {
        val reference = LocalWriteReference.dietRecords(listOf(1L))

        assertTrue(reference.trim().startsWith("{"))
        assertTrue(reference.trim().endsWith("}"))
        assertTrue(gson.fromJson(reference, LocalWriteReference::class.java).ids.isNotEmpty())
    }
}
