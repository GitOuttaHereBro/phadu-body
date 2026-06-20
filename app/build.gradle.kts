plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "phadubody.vercel.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.kotlin.reflect)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("replaceFonts") {
    doLast {
        val allowedSizes = listOf(13, 15, 17, 22, 34, 48)
        fun getClosest(size: Float): Int {
            return allowedSizes.minByOrNull { Math.abs(it - size) } ?: 15
        }
        val uiDir = file("src/main/java/com/example")
        val regex = Regex("(fontSize\\s*=\\s*)([0-9\\.]+)(\\.sp)")
        val lineRegex = Regex("(lineHeight\\s*=\\s*)([0-9\\.]+)(\\.sp)")
        uiDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            var text = file.readText()
            var modified = false
            text = regex.replace(text) { match ->
                val size = match.groupValues[2].toFloatOrNull()
                if (size != null) {
                    modified = true
                    "${match.groupValues[1]}${getClosest(size)}${match.groupValues[3]}"
                } else match.value
            }
            if (modified) file.writeText(text)
        }
    }
}

tasks.register("fixGlass") {
    doLast {
        val uiDir = file("src/main/java/com/example/ui")
        uiDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            var text = file.readText()
            var modified = false
            listOf(
                "BorderStroke(1.dp, GlassBorderDark)" to "BorderStroke(0.5.dp, GlassBorderDark)",
                "BorderStroke(1.dp, com.example.ui.theme.GlassBorderDark)" to "BorderStroke(0.5.dp, com.example.ui.theme.GlassBorderDark)",
                ".border(1.dp, GlassBorderDark" to ".border(0.5.dp, GlassBorderDark",
                ".border(1.dp, com.example.ui.theme.GlassBorderDark" to ".border(0.5.dp, com.example.ui.theme.GlassBorderDark",
                "CardDefaults.cardColors(containerColor = Color(0x33FF3B30))" to "CardDefaults.cardColors(containerColor = com.example.ui.theme.GlassDark)", // Fix any hardcoded modals if applicable
            ).forEach { (old, new) ->
                if (text.contains(old)) {
                    text = text.replace(old, new)
                    modified = true
                }
            }
            if (modified) {
                file.writeText(text)
            }
        }
    }
}

tasks.register("fixColors") {
    doLast {
        val uiDir = file("src/main/java/com/example/ui")
        uiDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            var text = file.readText()
            var modified = false
            
            if (file.name != "BarbellVisualizer.kt" && file.name != "Color.kt" && file.name != "PRsScreen.kt") {
                 if (text.contains("AccentGreen")) {
                     text = text.replace("com.example.ui.theme.AccentGreen", "Color.White")
                     text = text.replace("AccentGreen", "Color.White")
                     modified = true
                 }
            }
            if (file.name == "PRsScreen.kt") {
                 // PR glow/scale effect specifically is allowed
                 // but any other green like tab indicator should be White
                 if (text.contains("AccentGreen")) {
                     text = text.replace("com.example.ui.theme.AccentGreen", "Color.White")
                     text = text.replace("AccentGreen", "Color.White")
                     modified = true
                 }
            }
            // fix red play button
            if (text.contains("tint = Color.Red") && text.contains("PlayArrow")) {
                 text = text.replace("tint = Color.Red", "tint = Color.White")
                 modified = true
            }
            // Fix "Body StatMetrics"
            if (text.contains("Body StatMetrics")) {
                 text = text.replace("Body StatMetrics", "Body Stats")
                 modified = true
            }
            // Fix "IRON LOGG OS"
            if (text.contains("IRON LOGG OS")) {
                 text = text.replace("IRON LOGG OS", "IRON LOG OS")
                 modified = true
            }
            // Fix "Precision Guided Athlete Hardware"
            if (text.contains("Precision Guided Athlete Hardware")) {
                 text = text.replace("Precision Guided Athlete Hardware", "")
                 modified = true
            }
            
            if (modified) {
                file.writeText(text)
            }
        }
    }
}
