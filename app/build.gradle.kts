plugins {
    id("com.android.application")
}

android {
    namespace = "pl.tupsonik.deadzone"
    compileSdk = 35

    defaultConfig {
        applicationId = "pl.tupsonik.deadzone"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
    }
}


dependencies {
    testImplementation("junit:junit:4.13.2")
}
