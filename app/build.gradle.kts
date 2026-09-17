import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Versioning is driven by x.y.z git tags (single source of truth).
// - HEAD on exact tag  -> versionName "1.2.3", release builds allowed
// - N commits after tag -> versionName "1.2.3-N-g<sha>" (+ "-dirty" if tree is dirty)
// - No tag reachable / no git -> fallback "0.0.0-dev", versionCode 1
// versionCode keeps the legacy scheme major * 10000 + minor * 100 + patch
// so Play Store ordering is preserved across the migration.
data class GitVersion(val versionName: String, val versionCode: Int, val isExactTag: Boolean)

fun resolveGitVersion(): GitVersion {
    val fallback = GitVersion("0.0.0-dev", 1, false)
    val output = try {
        val process = ProcessBuilder(
            "git", "describe", "--tags", "--long", "--dirty",
            "--match", "[0-9]*.[0-9]*.[0-9]*"
        )
            .directory(rootDir)
            .redirectErrorStream(false)
            .start()
        val text = process.inputStream.bufferedReader().readText().trim()
        if (process.waitFor() != 0 || text.isEmpty()) return fallback
        text
    } catch (_: Exception) {
        return fallback
    }
    val parsed = Regex(
        """^(?<major>\d+)\.(?<minor>\d+)\.(?<patch>\d+)(?:-(?<distance>\d+)-g(?<sha>[0-9a-f]+))?(?<dirty>-dirty)?$"""
    ).matchEntire(output) ?: return fallback
    val major = parsed.groups["major"]!!.value.toInt()
    val minor = parsed.groups["minor"]!!.value.toInt()
    val patch = parsed.groups["patch"]!!.value.toInt()
    val distance = parsed.groups["distance"]?.value?.takeIf { it != "0" }
    val sha = parsed.groups["sha"]?.value
    val dirty = parsed.groups["dirty"] != null
    val versionName = buildString {
        append("$major.$minor.$patch")
        if (distance != null) append("-$distance-g$sha")
        if (dirty) append("-dirty")
    }
    return GitVersion(
        versionName = versionName,
        versionCode = major * 10000 + minor * 100 + patch,
        isExactTag = distance == null && !dirty
    )
}

val appVersion = resolveGitVersion()
logger.lifecycle("App version: ${appVersion.versionName} (${appVersion.versionCode})")

// Release APKs must come from an exact x.y.z tag so every release is traceable.
gradle.taskGraph.whenReady {
    if (hasTask(":app:assembleRelease") && !appVersion.isExactTag) {
        throw GradleException(
            "Release build requires HEAD to be exactly tagged x.y.z " +
                "(resolved '${appVersion.versionName}'). Tag it first: git tag <x.y.z>"
        )
    }
}

android {
    namespace = "com.ror.nms2go"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ror.nms2go"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersion.versionCode
        versionName = appVersion.versionName

        testInstrumentationRunner = "com.ror.nms2go.HiltTestRunner"
        // Visual delay for UI tests: -PvisualDelay=true or -PvisualDelayMs=2000 enables 2s pause after each action
        val visualDelayEnabled = (findProperty("visualDelay") as? String)?.toBoolean() == true
        val visualDelayMs = (findProperty("visualDelayMs") as? String)?.toLongOrNull() ?: 3000L
        testInstrumentationRunnerArguments["visualDelay"] = visualDelayEnabled.toString()
        testInstrumentationRunnerArguments["visualDelayMs"] = visualDelayMs.toString()
        buildConfigField("boolean", "VISUAL_TEST_DELAY", visualDelayEnabled.toString())
        buildConfigField("long", "VISUAL_TEST_DELAY_MS", "${visualDelayMs}L")
    }

    signingConfigs {
        create("release") {
            storeFile = keystoreProperties.getProperty("storeFile")?.let { rootProject.file(it) }
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Room 2.8.x ships kotlinx.serialization serializers compiled against >= 1.8.0.
// Force a consistent resolution so the KSP classpath does not downgrade it.
configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.jetbrains.kotlinx" &&
                requested.name.startsWith("kotlinx-serialization-")
            ) {
                useVersion("1.8.1")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)
    implementation(libs.jexcelapi)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.camera.mlkit.vision)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.zxing.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
}
