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
            name = "", // blank -> should fall back to a numbered exercise
            muscleGroup = null, // null -> should infer from name or default
            warmupSets = "", // empty
            workingSets = null,
            substitution1 = null, // missing
            substitution2 = null // missing
        )

        val malformedDay = ProgramDay(
            dayName = "Sample Day",
            exercises = listOf(malformedExercise)
        )

        val malformedWeek = ProgramWeek(
            block = "Test Block",
            days = listOf(malformedDay)
        )

        val malformedProgram = Program(
            programName = "",
            author = "",
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
        assertEquals("1", sanitExercise.warmupSets)
        assertEquals("3", sanitExercise.workingSets)
        assertEquals("8-10", sanitExercise.repRange)

        // Verify substitution fallbacks are correctly generated and non-null
        assertNotNull(sanitExercise.substitution1)
        assertNotNull(sanitExercise.substitution2)
        assertEquals("Bodyweight Push-Up", sanitExercise.substitution1?.name)
        assertEquals("Bodyweight Squat", sanitExercise.substitution2?.name)
    }
}

