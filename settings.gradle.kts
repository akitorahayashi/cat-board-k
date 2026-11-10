pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "cat-board-k"
include(":modules:app")
include(":modules:cat-api")
include(":modules:image-store")
include(":modules:image-loader")
include(":modules:image-screener")
include(":modules:image-prefetcher")
