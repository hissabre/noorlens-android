plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.basirah.noorlens"
    compileSdk = 34
    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "app.basirah.noorlens"
        minSdk = 29
        targetSdk = 32
        versionCode = 1
        versionName = "0.1"
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild {
            cmake {
                cppFlags += "-O3"
                arguments += listOf("-DANDROID_STL=c++_static")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // APK installable en sideload : signé avec la clé de debug
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // le modèle Whisper est lu tel quel : ne pas le compresser
    androidResources { noCompress += listOf("bin") }
    packaging { jniLibs { useLegacyPackaging = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
