plugins {
    alias(libs.plugins.android.application)

    //Google Services Gradle
    id("com.google.gms.google-services") version "4.4.4" apply false
}

android {
    namespace = "com.example.pocketmenu"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.pocketmenu"
        minSdk = 27
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //Variable
    val lifecycle_version = "2.9.0"

    //Viewmodel
    implementation("androidx.lifecycle:lifecycle-viewmodel:${lifecycle_version}")

    //Livedata
    implementation("androidx.lifecycle:lifecycle-livedata:${lifecycle_version}")

    //Firebase BOM
    implementation ("com.google.firebase:firebase-bom:34.6.0")

    //Firebase Authentication
    implementation ("com.google.firebase:firebase-auth")

    //Cloud Firestore
    implementation ("com.google.firebase:firebase-firestore")

    //Cloud Storage
    implementation ("com.google.firebase:firebase-storage")

}