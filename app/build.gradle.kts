plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.rapid.prominence.minty"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.rapid.prominence.minty"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        jvmTarget = "17"
    }
    // avoid DEPENDENCY_INFO_BLOCK for IzzyOnDroid
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation ("com.tencent:mmkv:1.3.14")
    implementation ("com.google.code.gson:gson:2.8.9")
    implementation ("io.supercharge:shimmerlayout:2.1.0")
    implementation("com.github.getActivity:XXPermissions:20.0")
    implementation("jp.wasabeef:glide-transformations:4.3.0")
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation ("com.blankj:utilcodex:1.31.1")
    implementation ("com.geyifeng.immersionbar:immersionbar:3.2.2")
    implementation ("com.google.android.flexbox:flexbox:3.0.0")

    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)

    implementation(libs.activity)

    implementation(libs.datastore)

    implementation(libs.viewmodel)


    implementation(libs.coil)


    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.apache.lang3)

    implementation(libs.hilt)
    kapt(libs.hilt.compiler)

    implementation(projects.corelib)

    implementation(projects.klib)

    implementation(libs.ktor.client.core)

    coreLibraryDesugaring(libs.desugaring)

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    implementation ("com.github.bumptech.glide:glide:4.15.1")// 使用最新稳定版本
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation(libs.timber)
    implementation ("com.github.zhpanvip:bannerviewpager:3.5.12")


}
