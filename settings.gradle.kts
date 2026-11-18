rootProject.name = "PhotoCollageMaker"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                // cronet-api, pulled in by the GMA Next-Gen SDK (via ads-kmp).
                includeGroupAndSubgroups("org.chromium")
            }
        }
        mavenCentral()
        // Network SDKs behind the AdMob mediation adapters that aren't on Google Maven or Maven Central.
        maven {
            url = uri("https://artifact.bytedance.com/repository/pangle/")
            mavenContent { includeGroupAndSubgroups("com.pangle.global") }
        }
        maven {
            url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
            mavenContent { includeGroupAndSubgroups("com.mbridge.msdk") }
        }
        // The ads SDK from this Mac's Maven Local (`./gradlew publishToMavenLocal` in
        // AdmobComposeSdk_Nextgen), checked first so an unpublished SDK version can be tried.
        // Only the SDK's group is looked up here.
        mavenLocal {
            mavenContent { includeGroupAndSubgroups("com.ozi.admob.nextgen.compose") }
        }
        // Internal Maven repo for the OZI ads SDK (com.ozi.admob.nextgen.compose:
        // ads-kmp, ads, ads-core). Needs the office network / VPN.
        maven {
            url = uri("sftp://172.16.3.122:22/home/altaf/maven-repo-compose")
            credentials {
                username = "altaf"
                password = "Altaf@123"
            }
        }
    }
}

include(":androidApp")
include(":shared")