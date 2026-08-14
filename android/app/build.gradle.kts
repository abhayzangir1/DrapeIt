import java.net.URI
import org.gradle.api.GradleException

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

val offlineApiBaseUrl = "https://offline.drapeproof.invalid"
val configuredApiBaseUrl = (providers.gradleProperty("DRAPEIT_API_BASE_URL").orNull
  ?: providers.gradleProperty("DRAPEPROOF_API_BASE_URL").orNull)
  ?.trim()
  ?.trimEnd('/')
  ?.takeIf(String::isNotBlank)

fun apiOrigin(raw: String): URI? = runCatching { URI(raw) }.getOrNull()?.takeIf { uri ->
    uri.host != null &&
        uri.userInfo == null &&
        (uri.port == -1 || uri.port in 1..65_535) &&
        (uri.rawPath.isNullOrEmpty() || uri.rawPath == "/") &&
        uri.rawQuery == null &&
        uri.rawFragment == null
}

fun isDebugApiOrigin(raw: String): Boolean {
    val uri = apiOrigin(raw) ?: return false
    return uri.scheme.equals("https", ignoreCase = true) ||
        (uri.scheme.equals("http", ignoreCase = true) && uri.host == "10.0.2.2")
}

fun isReleaseApiOrigin(raw: String): Boolean {
    val uri = apiOrigin(raw) ?: return false
    val host = uri.host.lowercase().trimEnd('.')
    return uri.scheme.equals("https", ignoreCase = true) &&
        !host.endsWith(".invalid") &&
        host != "api.drapeproof.app" &&
        host !in setOf("localhost", "127.0.0.1", "10.0.2.2", "::1", "[::1]")
}

if (configuredApiBaseUrl != null && !isDebugApiOrigin(configuredApiBaseUrl)) {
    throw GradleException(
        "DRAPEPROOF_API_BASE_URL must be an HTTPS origin without credentials, path, query, or fragment " +
            "(http://10.0.2.2[:port] is allowed for emulator development).",
    )
}

val drapeProofApiBaseUrl = configuredApiBaseUrl ?: offlineApiBaseUrl
val cloudConfigured = configuredApiBaseUrl?.let(::isReleaseApiOrigin) == true ||
    configuredApiBaseUrl?.let { origin ->
        val uri = apiOrigin(origin)
        uri?.scheme.equals("http", ignoreCase = true) && uri?.host == "10.0.2.2"
    } == true

android {
    namespace = "com.drapeproof.mobile"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.drapeproof.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "API_BASE_URL", "\"$drapeProofApiBaseUrl\"")
        buildConfigField("boolean", "CLOUD_CONFIGURED", cloudConfigured.toString())
        buildConfigField("String", "PROTOCOL_VERSION", "\"1.0.0-alpha\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    if (configuredApiBaseUrl == null || !isReleaseApiOrigin(configuredApiBaseUrl)) {
        throw GradleException(
            "Release builds require DRAPEPROOF_API_BASE_URL to be a real HTTPS origin. " +
                "The offline .invalid sentinel and api.drapeproof.app placeholder are not releasable.",
        )
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Controlled camera capture and local face landmarks. Images stay on-device
  // unless the user explicitly starts a YouCam feature.
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.exifinterface)
  implementation(libs.mediapipe.tasks.vision)

  implementation(project(":core"))
}
