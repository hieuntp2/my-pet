val junit4Version = property("dep.junit4").toString()

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aipet.brain.core.common"
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
    testImplementation("junit:junit:$junit4Version")
}
