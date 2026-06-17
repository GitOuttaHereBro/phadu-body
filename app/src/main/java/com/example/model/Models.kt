package com.example.model

data class Exercise(
    val id: String = "",
    val name: String = "",
    val muscleGroup: String = "",
    val unit: String = "kg",
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Template(
    val id: String = "",
    val name: String = "",
    val exercises: List<TemplateExercise> = emptyList()
)

data class TemplateExercise(
    val exerciseId: String = "",
    val exerciseName: String = "", // Added to simplify things
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val order: Int = 0,
    val videoUrl: String? = null
)

data class Workout(
    val id: String = "",
    val date: Long = System.currentTimeMillis(),
    val templateId: String? = null,
    val templateName: String? = null,
    val status: String = "in_progress",
    val durationMinutes: Int = 0,
    val loggedExercises: List<LoggedExercise> = emptyList(),
    val totalVolume: Double = 0.0
)

data class LoggedExercise(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val videoUrl: String? = null,
    val sets: List<WorkoutSet> = emptyList()
)

data class WorkoutSet(
    val setNumber: Int = 1,
    val weight: Double = 0.0,
    val reps: Int = 0,
    val isWarmup: Boolean = false,
    val completedAt: Long? = null
)

data class PersonalRecord(
    val exerciseId: String = "",
    val bestWeight: RecordDetail? = null,
    val bestVolume: RecordDetail? = null,
    val bestEstimated1RM: RecordDetail? = null
)

data class RecordDetail(
    val value: Double = 0.0,
    val reps: Int = 0,
    val date: Long = 0L,
    val workoutId: String = ""
)
