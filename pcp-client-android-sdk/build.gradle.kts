import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.sonar)
    kotlin("plugin.serialization") version "1.9.0"
    id("com.vanniktech.maven.publish") version "0.28.0"
    id("com.gradleup.nmcp") version "0.0.7"
}

android {
    namespace = "com.payone.pcp_client_android_sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation("androidx.fragment:fragment-testing:1.5.7")
}

nmcp {
    publishAllPublications {
        val keyUsername = "SONATYPE_USERNAME"
        val keyPassword = "SONATYPE_PASSWORD"
        username = findProperty(keyUsername)?.toString() ?: System.getenv(keyUsername)
        password = findProperty(keyPassword)?.toString() ?: System.getenv(keyPassword)

        publicationType = "USER_MANAGED"
    }
}
