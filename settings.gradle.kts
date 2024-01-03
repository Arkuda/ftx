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
include("ftxcore")
include("ftxcore")
