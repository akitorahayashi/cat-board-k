import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("catboard.platforms")
}

kotlin {
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "CatBoard"
            isStatic = true
            binaryOption("bundleId", "com.akitorahayashi.catboard")
            export(project(":modules:image-screener"))
            export(project(":modules:image-loader"))
        }
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.coroutines.android)
        }
        commonMain.dependencies {
            api(project(":modules:image-screener"))
            api(project(":modules:image-loader"))
            implementation(project(":modules:cat-api"))
            api(project(":modules:image-store"))
            api(project(":modules:image-prefetcher"))
            api(libs.lifecycle.viewmodel)
            api(libs.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.coroutines.test)
        }
    }
}
