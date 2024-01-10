plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.22"
}

group = "com.kiryantsev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


kotlin {

    jvm(){
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                //cli utils
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
                //coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")

                //serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

                //ftx core
                implementation(project(":ftxcore"))
            }
        }

        explicitApi()

    }
}