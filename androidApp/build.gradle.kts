import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    // Reads google-services.json (Firebase Remote Config + Analytics used by ads-kmp).
    alias(libs.plugins.googleServices)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)

    // Push notifications (FCM). The BOM matches the Firebase that ads-kmp already pulls in.
    implementation(project.dependencies.platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // AdMob mediation adapters. Each network is switched on in the AdMob console (mediation
    // groups); an adapter with no ad source there stays idle.
    implementation(libs.mediation.meta)
    implementation(libs.mediation.applovin)
    implementation(libs.mediation.unity)
    implementation(libs.mediation.mintegral)
    implementation(libs.mediation.liftoff)
    implementation(libs.mediation.pangle)
    implementation(libs.mediation.dtexchange)
    implementation(libs.mediation.moloco)
    implementation(libs.mediation.inmobi)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

// The adapters depend on the legacy Google Mobile Ads SDK, which can't be in the same APK as the
// GMA Next-Gen SDK (from ads-kmp). Next-Gen includes every class the adapters need.
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}

android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.piccollagemaker.photoeditor"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // AdMob app ID per build type, read by shared code as the `admob_app_id` string resource
    // (Platform.adMobAppId). Debug uses Google's test app ID, so development never serves live ads.
    buildTypes {
        debug {
            resValue("string", "admob_app_id", "ca-app-pub-3940256099942544~3347511713")
        }
        release {
            // TODO: replace with the Android app's own AdMob app ID (this one is shared with iOS for now).
            resValue("string", "admob_app_id", "ca-app-pub-9936075969508455~3732053299")
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
    buildFeatures {
        // For the per-build-type admob_app_id below.
        resValues = true
        compose = true
    }
}