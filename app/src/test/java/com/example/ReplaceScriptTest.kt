package com.example

import org.junit.Test
import java.io.File

class ReplaceScriptTest {
    @Test
    fun replaceStyles() {
        val uiDir = File("src/main/java/com/example/ui")
        uiDir.walk().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            var content = file.readText()
            var modified = false
            
            if (content.contains("import androidx.compose.material.icons.filled.")) {
                content = content.replace("import androidx.compose.material.icons.filled.", "import androidx.compose.material.icons.outlined.")
                modified = true
            }

            if (content.contains("Icons.Filled.")) {
                content = content.replace("Icons.Filled.", "Icons.Outlined.")
                modified = true
            }
            if (content.contains("Icons.Default.")) {
                content = content.replace("Icons.Default.", "Icons.Outlined.")
                modified = true
            }
            if (content.contains("Icons.AutoMirrored.Filled.")) {
                content = content.replace("Icons.AutoMirrored.Filled.", "Icons.AutoMirrored.Outlined.")
                modified = true
            }
            
            if (modified) {
                file.writeText(content)
            }
        }
    }
}
