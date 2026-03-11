val cameraXVersion = property("dep.cameraX").toString()
val mlKitFaceDetectionVersion = property("dep.mlKitFaceDetection").toString()
val tensorflowLiteVersion = property("dep.tensorflowLite").toString()

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aipet.brain.perception"
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
}

dependencies {
    implementation(project(":core-common"))
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("com.google.mlkit:face-detection:$mlKitFaceDetectionVersion")
    implementation("org.tensorflow:tensorflow-lite:$tensorflowLiteVersion")
}
