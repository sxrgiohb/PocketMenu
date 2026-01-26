plugins {
    alias(libs.plugins.android.application)

    //Google Services Gradle
    id("com.google.gms.google-services")
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

    // Versions
    val lifecycle_version = "2.10.0"
    val firebase_bom_version = "34.6.0"
    val navigation_version = "2.9.6"
    val firebase_ui_version = "9.1.1"
    val glide_version = "5.0.5"


    //Viewmodel
    implementation("androidx.lifecycle:lifecycle-viewmodel:${lifecycle_version}")
    //Livedata
    implementation("androidx.lifecycle:lifecycle-livedata:${lifecycle_version}")

    //Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:${firebase_bom_version}"))
    //Firebase Authentication
    implementation("com.google.firebase:firebase-auth")
    //Cloud Firestore
    implementation("com.google.firebase:firebase-firestore")
    //Cloud Storage
    implementation("com.google.firebase:firebase-storage")
    //Firebase Analytics
    implementation("com.google.firebase:firebase-analytics")

    //Jetpack Navigation
    implementation("androidx.navigation:navigation-fragment:${navigation_version}")
    implementation("androidx.navigation:navigation-ui:${navigation_version}")

    // FirebaseUI for Firebase Realtime Database
    implementation("com.firebaseui:firebase-ui-database:${firebase_ui_version}")
    // FirebaseUI for Firestore
    implementation("com.firebaseui:firebase-ui-firestore:${firebase_ui_version}")

    // Glide
    implementation("com.github.bumptech.glide:glide:${glide_version}")
    annotationProcessor("com.github.bumptech.glide:compiler:${glide_version}")


}