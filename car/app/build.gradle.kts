plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.car"
    compileSdk = 35


    defaultConfig {
        applicationId = "com.example.car"
        minSdk = 24
        targetSdk = 35
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


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    }
    buildFeatures {
        mlModelBinding=true
    }
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src/main/assets")
            }
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.tensorflow.lite.metadata)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("tech.gusavila92:java-android-websocket-client:1.2.2")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("org.java-websocket:Java-WebSocket:1.5.2")
    implementation ("org.tensorflow:tensorflow-lite:2.13.0")
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.3")
    implementation ("org.tensorflow:tensorflow-lite-gpu:2.13.0")
    implementation ("com.google.android.play:asset-delivery:2.3.0")
    implementation(project(":opencv"))


}