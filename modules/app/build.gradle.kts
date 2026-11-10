import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("catboard.ios")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
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
        commonMain.dependencies {
            api(project(":modules:image-screener"))
            api(project(":modules:image-loader"))
            implementation(project(":modules:cat-api"))
            implementation(project(":modules:image-store"))
            implementation(project(":modules:image-prefetcher"))
            implementation(libs.coil.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.coroutines.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.coroutines.test)
        }
    }
}
