package com.example

import com.example.model.Program
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Test
import java.io.File

class ParsingTest {
    @Test
    fun testParseJeffNippard() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(Program::class.java)
        val json = File("src/main/assets/jeff_nippard.json").readText()
        try {
            val p = adapter.fromJson(json)
            println("Parsed successfully: ${p?.programName}")
        } catch (e: Exception) {
            println("ERROR:")
            println(e.message)
            println(e.toString())
            throw e
        }
    }
}
