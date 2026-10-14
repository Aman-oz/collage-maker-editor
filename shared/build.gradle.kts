import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

/**
 * Generates `BgRemoverConfig` (the AI background remover's endpoint and API key) from
 * `local.properties`, the multiplatform stand-in for Android's `BuildConfig`: the key stays out of
 * version control and one generated file serves both Android and iOS. Missing keys generate empty
 * strings, which the app reports as "not configured" instead of failing the build.
 */
abstract class GenerateBgRemoverConfig : DefaultTask() {
    @get:Input abstract val baseUrl: Property<String>
    @get:Input abstract val apiKeyHeader: Property<String>
    @get:Input abstract val apiKey: Property<String>
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        fun literal(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""
        val file = outputDir.file("org/example/project/data/bgremover/BgRemoverConfig.kt").get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |// Generated from local.properties by :shared:generateBgRemoverConfig. Do not edit.
            |package org.example.project.data.bgremover
            |
            |internal object BgRemoverConfig {
            |    const val BASE_URL: String = ${literal(baseUrl.get())}
            |    const val API_KEY_HEADER: String = ${literal(apiKeyHeader.get())}
            |    const val API_KEY: String = ${literal(apiKey.get())}
            |}
            |""".trimMargin(),
        )
    }
}

/**
 * Generates `CentralDataConfig` (the central-data server behind the collage, template and frame
 * catalogs and Home's template/frame rows: its endpoint, instance, category ids and `X-API-Key`) from `local.properties`, the same
 * way as [GenerateBgRemoverConfig]. A missing key generates "": collages then fall back to the
 * bundled `collages.json`, and Templates / Frames report the error.
 */
abstract class GenerateCentralDataConfig : DefaultTask() {
    @get:Input abstract val baseUrl: Property<String>
    @get:Input abstract val instanceId: Property<String>
    @get:Input abstract val collagesCategoryId: Property<String>
    @get:Input abstract val templatesCategoryId: Property<String>
    @get:Input abstract val framesCategoryId: Property<String>
    @get:Input abstract val homeCategoryId: Property<String>
    @get:Input abstract val apiKey: Property<String>
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        fun literal(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""
        val file = outputDir.file("org/example/project/data/centraldata/CentralDataConfig.kt").get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |// Generated from local.properties by :shared:generateCentralDataConfig. Do not edit.
            |package org.example.project.data.centraldata
            |
            |internal object CentralDataConfig {
            |    const val BASE_URL: String = ${literal(baseUrl.get())}
            |    const val INSTANCE_ID: String = ${literal(instanceId.get())}
            |    const val COLLAGES_CATEGORY_ID: String = ${literal(collagesCategoryId.get())}
            |    const val TEMPLATES_CATEGORY_ID: String = ${literal(templatesCategoryId.get())}
            |    const val FRAMES_CATEGORY_ID: String = ${literal(framesCategoryId.get())}
            |    const val HOME_CATEGORY_ID: String = ${literal(homeCategoryId.get())}
            |    const val API_KEY: String = ${literal(apiKey.get())}
            |}
            |""".trimMargin(),
        )
    }
}

val localProperties: Provider<Properties> = providers
    .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
    .asText
    .map { text -> Properties().apply { load(text.reader()) } }
    .orElse(Properties())

/** The first of [keys] that `local.properties` sets to a non-blank value, or "" if none does. */
fun localProperty(vararg keys: String): Provider<String> = localProperties.map { properties ->
    keys.firstNotNullOfOrNull { key -> properties.getProperty(key)?.trim()?.takeIf(String::isNotEmpty) }.orEmpty()
}

val generateBgRemoverConfig = tasks.register<GenerateBgRemoverConfig>("generateBgRemoverConfig") {
    // The LAS app's key names (BASE_URL, KEY, X_API_KEY) also work, so its local.properties
    // entries can be copied over as they are.
    baseUrl = localProperty("BG_REMOVER_BASE_URL", "BASE_URL")
    apiKeyHeader = localProperty("BG_REMOVER_API_KEY_HEADER", "KEY")
    apiKey = localProperty("BG_REMOVER_API_KEY", "X_API_KEY")
    outputDir = layout.buildDirectory.dir("generated/bgRemoverConfig/kotlin")
}

val generateCentralDataConfig = tasks.register<GenerateCentralDataConfig>("generateCentralDataConfig") {
    // Only the key is secret; the endpoint and ids default to the production instance.
    fun withDefault(key: String, default: String) = localProperty(key).map { it.ifEmpty { default } }
    baseUrl = withDefault("CENTRAL_DATA_BASE_URL", "https://central-data.thetatechnologies.co/")
    instanceId = withDefault("CENTRAL_DATA_INSTANCE_ID", "6abb951fe25e70de3bfd8169")
    collagesCategoryId = withDefault("CENTRAL_DATA_COLLAGES_CATEGORY_ID", "6abb9538e25e70de3bfd816b")
    templatesCategoryId = withDefault("CENTRAL_DATA_TEMPLATES_CATEGORY_ID", "6ac5e1b1d78628e67e4bb760")
    framesCategoryId = withDefault("CENTRAL_DATA_FRAMES_CATEGORY_ID", "6ac5e234d78628e67e4bb7e7")
    homeCategoryId = withDefault("CENTRAL_DATA_HOME_CATEGORY_ID", "6acf5d131495d93ad6f8d49b")
    apiKey = localProperty("CENTRAL_DATA_API_KEY", "COLLAGES_API_KEY")
    outputDir = layout.buildDirectory.dir("generated/centralDataConfig/kotlin")
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    android {
       namespace = "org.example.project.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            // Ktor engine backing the shared HttpClient on Android.
            implementation(libs.ktor.client.okhttp)
            // Google Play Billing, behind the shared AppBillingWrapper.
            implementation(libs.google.billing)
        }
        iosMain.dependencies {
            // Ktor engine backing the shared HttpClient on iOS.
            implementation(libs.ktor.client.darwin)
        }
        commonMain {
            kotlin.srcDir(generateBgRemoverConfig.map { it.outputDir })
            kotlin.srcDir(generateCentralDataConfig.map { it.outputDir })
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Navigation 3
            implementation(libs.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.androidx.savedstate)
            implementation(libs.kotlinx.serialization.core)

            // Dependency injection
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Cross-platform gallery picker
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)

            // Liquid Glass effect (reusable GlassBottomNav module)
            implementation(libs.liquid)

            // Lottie playback for the onboarding animations
            implementation(libs.compottie)

            implementation(libs.kotlinx.coroutines.core)

            // Networking — fetch collage layouts (and their frame/thumbnail images) from the server.
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinxJson)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}