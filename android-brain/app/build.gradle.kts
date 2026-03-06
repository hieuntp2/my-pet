val coreKtxVersion = property("dep.coreKtx").toString()
val activityComposeVersion = property("dep.activityCompose").toString()
val composeBomVersion = property("dep.composeBom").toString()
val roomVersion = property("dep.room").toString()
val cameraXVersion = property("dep.cameraX").toString()
val junitVersion = property("dep.junit4").toString()

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aipet.brain.app"
    compileSdk = property("android.compileSdk").toString().toInt()

    defaultConfig {
        applicationId = "com.aipet.brain"
        minSdk = property("android.minSdk").toString().toInt()
        targetSdk = property("android.targetSdk").toString().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
    implementation(project(":core-common"))
    implementation(project(":ui-avatar"))
    implementation(project(":brain"))
    implementation(project(":memory"))
    implementation(project(":perception"))

    implementation("androidx.core:core-ktx:$coreKtxVersion")
    implementation("androidx.activity:activity-compose:$activityComposeVersion")

    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")

    implementation("androidx.room:room-runtime:$roomVersion")

    testImplementation("junit:junit:$junitVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
