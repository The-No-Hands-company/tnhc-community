import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tnhc.community"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "com.tnhc.community"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = rootProject.file("VERSION").readText().trim()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "FOUNDER_BUILD", "false")
        buildConfigField("String", "TARGET_VERSION", "\"0.0.3\"")
        val localConfiguration = Properties()
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(localConfiguration::load)
        val backendUrl = providers.environmentVariable("TNHC_BACKEND_URL")
            .orElse(providers.gradleProperty("tnhcBackendUrl"))
            .orElse(localConfiguration.getProperty("TNHC_BACKEND_URL", ""))
            .get()
        val publishableKey = providers.environmentVariable("TNHC_PUBLISHABLE_KEY")
            .orElse(providers.gradleProperty("tnhcPublishableKey"))
            .orElse(localConfiguration.getProperty("TNHC_PUBLISHABLE_KEY", ""))
            .get()
        fun quoted(value: String) = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        buildConfigField("String", "BACKEND_URL", quoted(backendUrl))
        buildConfigField("String", "PUBLISHABLE_KEY", quoted(publishableKey))
    }
    flavorDimensions += "audience"
    productFlavors {
        create("member") {
            dimension = "audience"
        }
        create("founder") {
            dimension = "audience"
            applicationIdSuffix = ".founder"
            buildConfigField("boolean", "FOUNDER_BUILD", "true")
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    applicationVariants.all {
        if (buildType.name == "release") {
            val url = defaultConfig.buildConfigFields["BACKEND_URL"]?.value?.trim('"').orEmpty()
            val key = defaultConfig.buildConfigFields["PUBLISHABLE_KEY"]?.value?.trim('"').orEmpty()
            val validateBackend = tasks.register("validate${name.replaceFirstChar(Char::uppercase)}BackendConfig") {
                doLast {
                    if (url.isBlank() || !url.startsWith("https://") || key.isBlank()) {
                        throw GradleException("Release builds require TNHC_BACKEND_URL (HTTPS) and TNHC_PUBLISHABLE_KEY")
                    }
                }
            }
            tasks.named("pre${name.replaceFirstChar(Char::uppercase)}Build").configure {
                dependsOn(validateBackend)
            }
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { abortOnError = true; warningsAsErrors = false }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

tasks.register("verifyBothDebugApps") {
    group = "verification"
    description = "Tests, lints, compiles UI tests for, and builds both Community debug apps."
    dependsOn(
        "testMemberDebugUnitTest",
        "testFounderDebugUnitTest",
        "lintMemberDebug",
        "lintFounderDebug",
        "compileMemberDebugAndroidTestKotlin",
        "compileFounderDebugAndroidTestKotlin",
        "assembleMemberDebug",
        "assembleFounderDebug",
    )
}

tasks.register("installBothDebugApps") {
    group = "install"
    description = "Installs both Member and Founder debug apps on a connected Android device."
    dependsOn("installMemberDebug", "installFounderDebug")
}

dependencies {
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.functions)
    implementation(libs.ktor.client.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.espresso.core)
}
