plugins {
//    id("java")
    id("kotlin")
    kotlin("plugin.serialization") version "1.9.22"
}

group = "com.kiryantsev.ftx.ftxcore"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {


    //coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")

    //serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

//    testImplementation(platform("org.junit:junit-bom:5.9.1"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
//    useJUnitPlatform()
}