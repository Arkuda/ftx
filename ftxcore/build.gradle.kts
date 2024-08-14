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

    mingwX64()
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()

    jvm{
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                //coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")

                // serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

                //fs
                implementation("com.squareup.okio:okio:3.9.0")

                //sockets
                val ktor_version = "2.3.12"
                implementation("io.ktor:ktor-network:$ktor_version")
                implementation("io.ktor:ktor-network-tls:$ktor_version")
            }
        }

        explicitApi()

    }
}