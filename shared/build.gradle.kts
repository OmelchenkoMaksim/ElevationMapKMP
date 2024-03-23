plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    val onPhone = listOf(
        iosArm64(),
        iosX64()
    )
    val onSimulator = listOf(
        iosSimulatorArm64()
    )
    val iosTargets = onPhone + onSimulator
    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = "ElevationMapShared"
        }
    }
    cocoapods {
        summary = "ElevationMap Shared Module for interacting with Google Maps and Elevation APIs"
        homepage = "https://github.com/Omelchenko/ElevationMap"
        version = "1.0"
        ios.deploymentTarget = "13.0"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "ElevationMapShared"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.google.maps.services)
            implementation(libs.play.services.maps)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
    task("testClasses")
}

android {
    namespace = "com.example.elevationmap"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}
