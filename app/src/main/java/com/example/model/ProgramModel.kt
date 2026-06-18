package com.example.model
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Program(
    val programName: String = "",
    val author: String = "",
    val weeks: Map<String, ProgramWeek> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class ProgramWeek(
    val block: String = "Block",
    val days: List<ProgramDay> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ProgramDay(
    val dayName: String = "",
    val isRestDay: Boolean = false,
    val exercises: List<ProgramExercise> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ProgramExercise(
    val name: String = "",
    val demoLink: String? = null,
    val link: String? = null,
    val warmupSets: String? = null,
    val workingSets: String? = null,
    val repRange: String? = null,
    val reps: String? = null,
    val earlySetRPE: String? = null,
    val lastSetRPE: Any? = null,
    val rest: String? = null,
    val lastSetTechnique: String? = "N/A",
    val muscleGroup: String? = null,
    val notes: String? = null,
    val substitution1: ProgramExercise? = null,
    val substitution2: ProgramExercise? = null
) {
    val videoUrl: String? get() = demoLink ?: link
    val lastSetRPEStr: String get() = lastSetRPE?.toString() ?: "N/A"
}
