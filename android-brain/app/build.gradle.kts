val coreKtxVersion = property("dep.coreKtx").toString()
val activityComposeVersion = property("dep.activityCompose").toString()
val composeBomVersion = property("dep.composeBom").toString()
val roomVersion = property("dep.room").toString()
val cameraXVersion = property("dep.cameraX").toString()
val dataStoreVersion = property("dep.dataStore").toString()
val junitVersion = property("dep.junit4").toString()
val coroutinesTestVersion = property("dep.coroutinesTest").toString()
val mlKitFaceDetectionVersion = property("dep.mlKitFaceDetection").toString()
val androidJunitVersion = "1.1.5"
val espressoVersion = "3.5.1"

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

        // Keep release ABIs aligned with 16 KB page-size capable device targets.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
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
    implementation(project(":pixel-avatar"))

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
    implementation("androidx.datastore:datastore-preferences:$dataStoreVersion")

    implementation("com.google.mlkit:face-detection:$mlKitFaceDetectionVersion")

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesTestVersion")
    testImplementation("androidx.datastore:datastore-preferences-core:$dataStoreVersion")

    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:$androidJunitVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.register("connectedTeachSessionPersistenceDebugAndroidTest") {
    group = "verification"
    description = "Runs connected Android instrumentation validation for teach-session persistence flows (requires a connected emulator/device)."
    dependsOn("connectedDebugAndroidTest")
}
