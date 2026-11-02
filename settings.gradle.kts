pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://repo.spongepowered.org/maven") }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "fabric-loom") {
                useModule("net.fabricmc:fabric-loom:1.19.1")
            }
        }
    }
}

rootProject.name = "working-consoles-remake"

include("wcore")
include("gbsp")
include("ps2")
include("sega")
include("arcade")
include("nintendo")
include("retro")
include("stream")
include("disc")
include("classic")
include("arcadeplus")
include("arcadebuild")
include("gamecube")   // Dustin's Arcade 1.1: the rebuilt, interactive Fresno-style nickel arcade
