import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlinx.kover")
}

// The signing key is not in the repository. CI reads it from the secrets, a local build from
// FRESKIEL_KEYSTORE_PATH and the passwords in ~/.gradle/gradle.properties.
val signingKeystorePath =
    providers.gradleProperty("freskiel.keystorePath").orNull ?: System.getenv("FRESKIEL_KEYSTORE_PATH")
val signingStorePassword =
    providers.gradleProperty("freskiel.keystorePassword").orNull ?: System.getenv("FRESKIEL_KEYSTORE_PASSWORD")
val signingKeyAlias =
    providers.gradleProperty("freskiel.keyAlias").orNull ?: System.getenv("FRESKIEL_KEY_ALIAS")
val signingKeyPassword =
    providers.gradleProperty("freskiel.keyPassword").orNull ?: System.getenv("FRESKIEL_KEY_PASSWORD")
val hasSigningKey =
    listOf(signingKeystorePath, signingStorePassword, signingKeyAlias, signingKeyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "dev.scdeal.freskiel"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "dev.scdeal.freskiel"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasSigningKey) {
            create("freskiel") {
                storeFile = file(signingKeystorePath!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // One key for every build, so an update installs over the previous one.
            signingConfig = signingConfigs.findByName("freskiel") ?: signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.findByName("freskiel")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    kotlin {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
    }

    // Declare the toolchain rather than inheriting the daemon's JVM. Without
    // it, a Gradle daemon on an older JDK fails with "invalid source release:
    // 25" only once it reaches :app, long after the real work is done.
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = true
        disable += "GradleDependency"
        // Keep the fully supported Gradle 9.3.1 matrix instead of lint's newer 9.6.1 suggestion.
        informational += "AndroidGradlePluginVersion"
        baseline = file("lint-baseline.xml")
    }
}

tasks.named("check") {
    dependsOn("lintDebug")
}

dependencies {
    implementation(project(":core-rpc"))
    implementation(project(":core-net"))
    implementation(project(":core-sessions"))

    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("com.github.jeziellago:compose-markdown:0.7.2")
    implementation("io.github.java-diff-utils:java-diff-utils:4.17")
    implementation("io.noties:prism4j:2.0.0") {
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")

    androidTestImplementation(platform("androidx.compose:compose-bom:2026.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
}
