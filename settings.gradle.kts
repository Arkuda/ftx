//plugins {
//    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
//}
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "ftx"

include(":android")
include(":desktop")
include(":commonui")
include(":ftxcore")
//include(":ftxcore")
include("ftxcli")
