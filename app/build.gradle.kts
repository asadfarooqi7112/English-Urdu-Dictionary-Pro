plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.hfad.personaldictionary"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hfad.personaldictionary"
        minSdk = 24
        targetSdk = 35
        versionCode = 12
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    buildFeatures {
        viewBinding = true
    }

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.image.labeling.default.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.extensions)

    implementation(libs.ucrop)

    implementation ("com.google.mlkit:image-labeling:17.0.9")
    implementation ("com.google.mlkit:object-detection:17.0.2")
    implementation ("com.google.mlkit:object-detection-custom:17.0.2")

    implementation ("com.google.android.flexbox:flexbox:3.0.0")
}