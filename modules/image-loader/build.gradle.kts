plugins {
    id("catboard.platforms")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.coil.core)
            implementation(libs.coil.network.ktor)
            implementation(libs.ktor.client.core)
            implementation(libs.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.coroutines.test)
        }
    }
}
