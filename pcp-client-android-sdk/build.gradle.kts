plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.sonar)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.payone.pcp_client_android_sdk"
    compileSdk = 37

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
    testOptions.unitTests.isIncludeAndroidResources = true
}

kotlin.compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)

tasks.withType<Test>().configureEach {
    // Each Android platform has its own Robolectric sandbox/resources.
    maxHeapSize = "2g"
    systemProperty("robolectric.sandboxCacheSize", "2")
    // Android 16+ shared-memory emulation accesses FileDescriptor internals.
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
}

dependencies {
    api(libs.google.wallet)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.espresso.core)
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates("io.github.payone-gmbh", "pcp-client-android-sdk", "1.4.0")

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
