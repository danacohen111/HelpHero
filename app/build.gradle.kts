plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    kotlin("kapt")
}

android {
    namespace = "com.example.helphero"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.helphero"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CLOUD_NAME", "\"${project.properties["CLOUD_NAME"] ?: ""}\"")
        buildConfigField("String", "API_KEY", "\"${project.properties["API_KEY"] ?: ""}\"")
        buildConfigField("String", "API_SECRET", "\"${project.properties["API_SECRET"] ?: ""}\"")
        buildConfigField("String", "CLOUDINARY_BASE_URL", "\"${project.properties["CLOUDINARY_BASE_URL"] ?: ""}\"")
        buildConfigField("String", "NOMINATIM_URL", "\"${project.properties["NOMINATIM_URL"] ?: ""}\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.filament.android)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("jp.wasabeef:picasso-transformations:2.4.0")
    testImplementation(libs.junit)
    implementation("androidx.multidex:multidex:2.0.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:3.11.2")
    testImplementation("org.mockito:mockito-inline:3.11.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.3")
    implementation("com.google.firebase:firebase-auth-ktx:22.1.1")
    implementation("com.google.firebase:firebase-firestore-ktx:24.5.0")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.1.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.cloudinary:cloudinary-android:3.0.2")
    implementation("androidx.room:room-runtime:2.5.0")
    implementation ("com.google.code.gson:gson:2.8.9")
    implementation("com.google.android.gms:play-services-auth:20.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    kapt("androidx.room:room-compiler:2.5.0")

}