val coroutinesVersion = property("dep.coroutines").toString()
val junitVersion = property("dep.junit4").toString()
val coroutinesTestVersion = property("dep.coroutinesTest").toString()

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aipet.brain.brain"
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

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesTestVersion")
}
