val roomVersion = property("dep.room").toString()
val junitVersion = property("dep.junit4").toString()
val coroutinesTestVersion = property("dep.coroutinesTest").toString()

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.aipet.brain.memory"
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":brain"))

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesTestVersion")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
}
