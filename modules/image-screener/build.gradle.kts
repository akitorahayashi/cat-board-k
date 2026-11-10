plugins {
    id("catboard.ios")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":modules:image-loader"))
    }
}
