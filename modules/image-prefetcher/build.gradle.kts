import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("catboard.platforms")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":modules:image-loader"))
            api(project(":modules:image-screener"))
            api(project(":modules:image-store"))
            api(libs.coroutines.core)
            api(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(libs.coroutines.test)
        }
    }
    dependencies.add("kspAndroid", libs.room.compiler)
    targets.withType<KotlinNativeTarget>().configureEach {
        project.dependencies.add("ksp${name.replaceFirstChar { it.uppercaseChar() }}", libs.room.compiler)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}
