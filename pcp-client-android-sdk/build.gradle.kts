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

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates("io.github.payone-gmbh", "pcp-client-android-sdk", "0.0.1")

    pom {
        name.set("PCP-CLIENT-SDK-ANDROID")
        description.set("The PAYONE Client Android SDK")
        inceptionYear.set("2024")
        url.set("https://github.com/PAYONE-GmbH/PCP-client-android-SDK")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit/")
            }
        }

        developers {
            developer {
                name.set("PAYONE Intergrations")
                email.set("integrations@payone.com")
                url.set("https://github.com/PAYONE-GmbH/")
            }
        }

        scm {
            url.set("https://github.com/PAYONE-GmbH/PCP-client-android-SDK")
            connection.set("scm:git:git://github.com/PAYONE-GmbH/PCP-client-android-SDK.git")
            developerConnection.set("scm:git:ssh://git@github.com/PAYONE-GmbH/PCP-client-android-SDK.git")
        }
    }
}
