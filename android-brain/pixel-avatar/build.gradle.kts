val composeBomVersion = property("dep.composeBom").toString()
val junitVersion = property("dep.junit4").toString()
val coroutinesTestVersion = property("dep.coroutinesTest").toString()

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aipet.brain.pixel.avatar"
    compileSdk = property("android.compileSdk").toString().toInt()

    defaultConfig {
        minSdk = property("android.minSdk").toString().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(property("android.jvmTarget").toString())
        targetCompatibility = JavaVersion.toVersion(property("android.jvmTarget").toString())
    }
    kotlinOptions {
        jvmTarget = property("android.jvmTarget").toString()
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = property("android.composeCompiler").toString()
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesTestVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
