import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "io.github.sandrogekeler.glotze"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.sandrogekeler.glotze"
        // Android 9, the oldest Android TV release still common on sets in use.
        // Raise it once the target TV's version is known (ROADMAP.md).
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Signed with the local debug key so `installRelease` works and speed is measured on
            // the build people use. GlotzeOS is sideloaded onto one TV; a release key replaces
            // this if it is ever published. A different machine's debug key means uninstalling first.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric reads the merged manifest and resources (src/testDebug).
        unitTests.isIncludeAndroidResources = true
        // CI has no test report to open: print the assertion message and stack trace.
        unitTests.all { it.testLogging { exceptionFormat = TestExceptionFormat.FULL } }
    }

    lint {
        warningsAsErrors = true
        // Version bumps are Dependabot's job, not a reason to fail the build.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.material)
    // Installs the Baseline Profile on sideloaded builds, where no app store does it.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":benchmark"))
    debugImplementation(libs.androidx.compose.ui.tooling)
    // The empty activity createComposeRule() hosts tests in; debug only, like the tests that need it.
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
