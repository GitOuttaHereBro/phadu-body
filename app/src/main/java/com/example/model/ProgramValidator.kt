package com.example.model

import android.util.Log

object ProgramValidator {

    /**
     * Infer the muscle group for an exercise based on its name keywords.
     */
    fun inferMuscleGroup(name: String): String {
        val n = name.lowercase().trim()
        return when {
            n.contains("bench") || n.contains("press") && (n.contains("incline") || n.contains("decline") || n.contains("chest") || n.contains("barbell") || n.contains("dumbbell") || n.contains("pec") || n.contains("fly") || n.contains("crossover")) || n.contains("pushup") || n.contains("dips") -> "Chest"
            n.contains("squat") || n.contains("leg") || n.contains("ham") || n.contains("quad") || n.contains("calf") || n.contains("calves") || n.contains("thigh") || n.contains("lunge") || n.contains("nordic") || n.contains("hack") || n.contains("extension") && n.contains("leg") || n.contains("rdl") || n.contains("romanian") -> "Legs"
            n.contains("pull") || n.contains("row") || (n.contains("lat") && !n.contains("lateral") && !n.contains("plate")) || n.contains("chin") || n.contains("deadlift") || n.contains("shrug") || n.contains("rear delt") -> "Back"
            n.contains("shoulder") || n.contains("overhead") || n.contains("military") || n.contains("lateral") || n.contains("raise") || n.contains("delt") || n.contains("arnold") -> "Shoulders"
            n.contains("curl") || n.contains("bicep") || n.contains("tricep") || n.contains("skull") || n.contains("extension") && (n.contains("triceps") || n.contains("overhead")) || n.contains("pushdown") -> "Arms"
            n.contains("crunch") || n.contains("situp") || n.contains("plank") || n.contains("abs") || n.contains("core") || n.contains("hanging") -> "Core"
            n.contains("cardio") || n.contains("treadmill") || n.contains("run") || n.contains("bike") || n.contains("walk") -> "Cardio"
            else -> "General"
        }
    }

    /**
     * Returns a pair of sensible fallback exercise names for substitutions based on the primary name and muscle group.
     */
    fun getDefaultSubstitutions(name: String, muscleGroup: String): Pair<String, String> {
        val n = name.lowercase()
        return when (muscleGroup) {
            "Chest" -> {
                if (n.contains("incline")) {
                    Pair("45° Incline DB Press", "45° Incline Machine Press")
                } else if (n.contains("fly") || n.contains("crossover")) {
                    Pair("Pec Deck Fly", "Low-to-High Cable Fly")
                } else {
                    Pair("Dumbbell Flat Bench Press", "Machine Chest Press")
                }
            }
            "Legs" -> {
                if (n.contains("squat")) {
                    Pair("Leg Press", "Goblet Squat")
                } else if (n.contains("curl")) {
                    Pair("Seated Leg Curl", "Lying Leg Curl")
                } else if (n.contains("rdl") || n.contains("deadlift")) {
                    Pair("Dumbbell Romanian Deadlift", "Snatch-Grip RDL")
                } else {
                    Pair("Bulgarian Split Squat", "Leg Press")
                }
            }
            "Back" -> {
                if (n.contains("pull") || n.contains("chin")) {
                    Pair("Neutral-Grip Pull-Up", "Wide-Grip Lat Pulldown")
                } else if (n.contains("row")) {
                    Pair("Chest-Supported Dumbbell Row", "Single-Arm Dumbbell Row")
                } else {
                    Pair("Seated Cable Row", "Barbell Row")
                }
            }
            "Shoulders" -> {
                if (n.contains("lateral") || n.contains("raise")) {
                    Pair("Cable Lateral Raise", "Chest-Supported Dumbbell Lateral Raise")
                } else if (n.contains("rear")) {
                    Pair("Reverse Pec Deck", "Face Pull")
                } else {
                    Pair("Seated Dumbbell Shoulder Press", "Smith Machine Shoulder Press")
                }
            }
            "Arms" -> {
                if (n.contains("curl") || n.contains("bicep")) {
                    Pair("Incline Dumbbell Curl", "EZ-Bar Preacher Curl")
                } else {
                    Pair("Overhead Cable Triceps Extension", "Dumbbell Triceps Kickback")
                }
            }
            "Core" -> {
                Pair("Hanging Leg Raise", "Decline Weighted Crunch")
            }
            else -> {
                Pair("Bodyweight Push-Up", "Bodyweight Squat")
            }
        }
    }

    /**
     * Propagate missing weeks and sanitize properties for v4.0 schema.
     */
    fun validateAndSanitize(program: Program?): Program {
        var baseProgram = program ?: Program(program = ProgramInfo(name = "Bodybuilding Transformation", author = "Jeff Nippard"))
        
        val sanitizedWeeks = baseProgram.weeks.mapValues { (weekKey, weekObj) ->
            val sanitizedDays = weekObj.days.map { dayObj ->
                val sanitizedExercises = dayObj.exercises.mapIndexed { index, exObj ->
                    val resolvedName = exObj.name.takeIf { it.isNotBlank() } ?: "Exercise ${index + 1}"
                    val resolvedGroup = exObj.muscleGroup?.takeIf { it.isNotBlank() } ?: inferMuscleGroup(resolvedName)

                    val prescriptions = exObj.prescription ?: ExercisePrescription()
                    val tech = exObj.technique ?: ExerciseTechnique()
                    val notes = exObj.notes ?: ExerciseNotes()
                    
                    val alts = exObj.alternatives ?: run {
                        if (exObj.flatSub1 != null || exObj.flatSub2 != null) {
                            ExerciseAlternatives(
                                substitution1 = exObj.flatSub1,
                                substitution2 = exObj.flatSub2
                            )
                        } else {
                            val (s1Name, s2Name) = getDefaultSubstitutions(resolvedName, resolvedGroup)
                            ExerciseAlternatives(
                                substitution1 = ProgramExercise(name = s1Name, muscleGroup = resolvedGroup),
                                substitution2 = ProgramExercise(name = s2Name, muscleGroup = resolvedGroup)
                            )
                        }
                    }

                    exObj.copy(
                        name = resolvedName,
                        muscleGroup = resolvedGroup,
                        prescription = prescriptions,
                        technique = tech,
                        notes = notes,
                        alternatives = alts
                    )
                }
                dayObj.copy(exercises = sanitizedExercises)
            }
            weekObj.copy(days = sanitizedDays)
        }

        return baseProgram.copy(
            weeks = sanitizedWeeks
        )
    }

    fun validateStrict(program: Program?): List<String> {
        val errors = mutableListOf<String>()
        if (program == null) {
            errors.add("Program JSON root is empty.")
            return errors
        }

        if (program.weeks.isEmpty()) {
            errors.add("Missing expected key: 'weeks' map is empty.")
        }

        program.weeks.forEach { (weekKey, weekObj) ->
            if (weekObj.days.isEmpty()) {
                errors.add("[$weekKey] Missing expected key: 'days' list is empty.")
            } else {
                weekObj.days.forEachIndexed { dayIdx, dayObj ->
                    if (!dayObj.isRestDay) {
                        if (dayObj.exercises.isEmpty()) {
                            errors.add("[$weekKey, Day $dayIdx] Non-rest day contains zero exercises.")
                        } else {
                            dayObj.exercises.forEachIndexed { exIdx, exObj ->
                                if (exObj.name.isBlank()) {
                                    errors.add("[$weekKey, Day $dayIdx, Exercise $exIdx] Missing name.")
                                }
                            }
                        }
                    }
                }
            }
        }
        return errors
    }
}
