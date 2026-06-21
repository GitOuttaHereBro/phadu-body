package com.example

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying ProgramValidator's correctness under varied scenarios.
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testMuscleGroupInference() {
        assertEquals("Chest", ProgramValidator.inferMuscleGroup("Flat Bench Press"))
        assertEquals("Legs", ProgramValidator.inferMuscleGroup("Warm-up Back Squat"))
        assertEquals("Back", ProgramValidator.inferMuscleGroup("Wide-grip Lat Pulldown"))
        assertEquals("Shoulders", ProgramValidator.inferMuscleGroup("Dumbbell Lateral Raise"))
        assertEquals("Arms", ProgramValidator.inferMuscleGroup("Incline Dumbbell Bicep Curl"))
        assertEquals("Core", ProgramValidator.inferMuscleGroup("Weighted Decline Crunch"))
        assertEquals("General", ProgramValidator.inferMuscleGroup("Unknown Mystical Lift"))
    }

    @Test
    fun testProgramValidator_sanitizesMissingFields() {
        // Create an incomplete/malformed program exercise structure
        val malformedExercise = ProgramExercise(
            name = "",
            muscleGroup = null,
            directWarmupSets = "",
            directWorkingSets = null,
            flatSub1 = null,
            flatSub2 = null
        )

        val malformedDay = ProgramDay(
            directDayName = "Sample Day",
            exercises = listOf(malformedExercise)
        )

        val malformedWeek = ProgramWeek(
            block = "Test Block",
            days = listOf(malformedDay)
        )

        val malformedProgram = Program(
            directProgramName = "",
            directAuthor = "",
            weeks = mapOf("week1" to malformedWeek)
        )

        // Run validation
        val sanitized = ProgramValidator.validateAndSanitize(malformedProgram)

        // Verify program name and author fallbacks
        assertEquals("The Bodybuilding Transformation System - Intermediate-Advanced", sanitized.programName)
        assertEquals("Jeff Nippard", sanitized.author)

        // Verify exercise fields fallbacks
        val sanitWeek = sanitized.weeks["week1"]
        assertNotNull(sanitWeek)
        val sanitDay = sanitWeek!!.days.first()
        val sanitExercise = sanitDay.exercises.first()

        assertEquals("Exercise 1", sanitExercise.name)
        assertEquals("General", sanitExercise.muscleGroup)
        assertEquals("1", sanitExercise.directWarmupSets?.toString() ?: sanitExercise.prescription?.warmup?.setsCount)
        assertEquals("3", sanitExercise.directWorkingSets?.toString() ?: sanitExercise.prescription?.workingSetsInt?.toString())
        assertEquals("8-10", sanitExercise.directReps?.toString() ?: sanitExercise.prescription?.repRange)

        // Verify substitution fallbacks are correctly generated and non-null
        val hasAlt1 = sanitExercise.alternatives?.substitution1 != null || sanitExercise.flatSub1 != null
        val hasAlt2 = sanitExercise.alternatives?.substitution2 != null || sanitExercise.flatSub2 != null
        assertTrue(hasAlt1)
        assertTrue(hasAlt2)
        assertEquals("Bodyweight Push-Up", sanitExercise.alternatives?.substitution1?.name ?: sanitExercise.flatSub1?.name)
        assertEquals("Bodyweight Squat", sanitExercise.alternatives?.substitution2?.name ?: sanitExercise.flatSub2?.name)
    }
}

