plugins {
    id("catboard.platforms")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":modules:image-loader"))
    }
}
