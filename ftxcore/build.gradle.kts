plugins {
//    id("java")
//    id("kotlin")
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.22"
//    id("com.android.library")
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
//    jvm("desktop") {
//        compilations.all {
//            kotlinOptions.jvmTarget = "11"
//        }
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                //coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")

                //serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }

//        val coreMain by getting {
//
//            dependencies {
//                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")
//
//                //serialization
//                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
//            }
//        }

        explicitApi()

    }
}