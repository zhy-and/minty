plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id ("com.google.gms.google-services")
    id ("com.google.firebase.crashlytics")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.rapid.prominence.minty"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.rapid.prominence.minty"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "1.0.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
//            isShrinkResources = true
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
    implementation ("com.google.android.ump:user-messaging-platform:2.2.0")
    implementation("com.google.android.gms:play-services-ads:25.0.0")
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("org.greenrobot:eventbus:3.3.1")
    implementation ("com.android.installreferrer:installreferrer:2.2")
    implementation (platform("com.google.firebase:firebase-bom:32.1.0"))
    implementation ("com.appsflyer:af-android-sdk:6.16.2")
    implementation ("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-crashlytics")
    implementation ("com.facebook.android:facebook-android-sdk:latest.release")
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

    implementation  ("com.google.firebase:firebase-messaging")

    implementation("com.google.ads.mediation:facebook:6.21.0.1")
    implementation("com.google.ads.mediation:mintegral:17.0.91.0")
    implementation("com.google.ads.mediation:pangle:7.9.1.1.0")
    implementation("com.unity3d.ads:unity-ads:4.16.5")
    implementation("com.google.ads.mediation:unity:4.16.6.0")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation(libs.ktor.serialization.json)

}
