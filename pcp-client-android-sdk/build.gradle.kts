plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.sonar)
    kotlin("plugin.serialization") version "1.9.0"
    id("com.vanniktech.maven.publish") version "0.29.0"
    id("maven-publish")
    id("signing")
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

publishing {
    publications {
        afterEvaluate {
            create<MavenPublication>("mavenAndroid") {
                from(components["release"])

                groupId = "com.payone.pcp_client_android_sdk"
                artifactId = "pcp-client-android-sdk"
                version = "0.0.1"

                pom {
                    packaging = "aar"
                    name.set("PCP Client Android SDK")
                    description.set("Welcome to the PAYONE Commerce Platform Client Android SDK for the PAYONE Commerce Platform. This SDK provides everything a client needs to easily complete payments using Credit or Debit Card, PAYONE Buy Now Pay Later (BNPL).")
                    url.set("https://github.com/PAYONE-GmbH/PCP-client-android-SDK")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/license/mit/")
                        }
                    }

                    developers {
                        developer {
                            name.set("Djordje Nikolic")
                            email.set("d.nikolic@NanoGiants.de")
                        }
                    }

                    scm {
                        connection.set("scm:git:https://github.com/PAYONE-GmbH/PCP-client-android-SDK.git")
                        developerConnection.set("scm:git:https://github.com/PAYONE-GmbH/PCP-client-android-SDK.git")
                        url.set("https://github.com/PAYONE-GmbH/PCP-client-android-SDK")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "MavenCentral"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("mavenCentralUsername") as String? ?: System.getenv("ORG_GRADLE_PROJECT_mavenCentralUsername")
                password = project.findProperty("mavenCentralPassword") as String? ?: System.getenv("ORG_GRADLE_PROJECT_mavenCentralPassword")
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        project.findProperty("signingInMemoryKeyId") as String? ?: System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyId"),
        project.findProperty("signingInMemoryKey") as String? ?: System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey"),
        project.findProperty("signingInMemoryKeyPassword") as String? ?: System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword")
    )
    afterEvaluate {
        sign(publishing.publications["mavenAndroid"])
    }
}




