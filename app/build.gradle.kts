import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun escaped(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "pl.tupsonik.niewtop"
    compileSdk = 37.2

    defaultConfig {
        applicationId = "pl.tupsonik.niewtop"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"

        buildConfigField("String", "SUPABASE_URL", "\"${escaped(localProperties.getProperty("SUPABASE_URL", ""))}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${escaped(localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY", ""))}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.8.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.ktor:ktor-client-android:3.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}