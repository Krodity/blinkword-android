plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "uk.krodity.blinkword"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "uk.krodity.blinkword"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        base.archivesName = "blinkword-$versionName"

        // sherpa-onnx ships native libraries for four ABIs. Both target devices
        // are arm64, and carrying the rest would add ~40MB to the APK.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        // Pinned rather than left to AGP's default. AGP resolves the debug
        // keystore relative to the Android config directory, and that moved
        // to the XDG location -- so the same source tree could start
        // producing APKs signed with a different key than the ones already
        // on a device. Naming the file makes the signature a property of
        // the project instead of the environment.
        // Machines without that file keep AGP's default debug keystore.
        val pinnedDebugKeystore = File(System.getProperty("user.home"), ".config/.android/debug.keystore")
        if (pinnedDebugKeystore.exists()) {
            getByName("debug") {
                storeFile = pinnedDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.jsoup)
    implementation(libs.pdfbox.android)

    // Offline neural text-to-speech. No Maven Central release from upstream,
    // so the official prebuilt AAR is vendored in app/libs.
    implementation(files("libs/sherpa-onnx-1.13.8.aar"))
    // Voice models ship as .tar.bz2 and the JDK can't read bzip2.
    implementation(libs.commons.compress)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
