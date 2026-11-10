import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    iosArm64()
    iosSimulatorArm64()
    android {
        namespace = "com.akitorahayashi.catboard.${project.name.replace("-", "") }"
        compileSdk = catalog.findVersion("android-compile").get().requiredVersion.toInt()
        minSdk = catalog.findVersion("android-min").get().requiredVersion.toInt()
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        withHostTest {}
    }
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
