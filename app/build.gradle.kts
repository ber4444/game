@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
@file:Suppress("UnstableApiUsage")

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.file.DirectoryProperty
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.example.myapplication"
        compileSdk = 36
        minSdk = 24

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        withHostTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            isIncludeAndroidResources = true
        }

        withDeviceTestBuilder {}.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            emulatorControl {
                enable = true
            }
        }

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }

        val androidDeviceTest by getting {
            dependencies {
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.espresso.device)
                implementation(libs.androidx.compose.ui.test.junit4.android)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "game.app.generated.resources"
    publicResClass = true
}

tasks.configureEach {
    if (name.endsWith("ComposeResourcesToAndroidAssets")) {
        val outputDirectory = javaClass.methods
            .firstOrNull { it.name == "getOutputDirectory" && it.parameterCount == 0 }
            ?.invoke(this) as? DirectoryProperty

        if (outputDirectory != null && !outputDirectory.isPresent) {
            outputDirectory.set(
                layout.buildDirectory.dir("generated/compose/resourceGenerator/androidAssets/$name")
            )
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.example.myapplication.MainKt"

        nativeDistributions {
            packageName = "game"
            packageVersion = "1.0.0"
            targetFormats(TargetFormat.Deb)
        }
    }
}
