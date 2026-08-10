import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "co.bssply.bsa"
    compileSdk = 36

    defaultConfig {
        applicationId = "co.bssply.bsa"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "0.4.2-alpha"
        manifestPlaceholders["carAppCategory"] = "androidx.car.app.category.POI"
        buildConfigField("String", "CAR_APP_MODE", "\"poi\"")
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    flavorDimensions += "carMode"
    productFlavors {
        create("poi") {
            dimension = "carMode"
            // Distinct ID allows all four A/B variants to coexist on one phone.
            applicationIdSuffix = ".poi"
            versionNameSuffix = "-poi"
            manifestPlaceholders["carAppCategory"] = "androidx.car.app.category.POI"
            buildConfigField("String", "CAR_APP_MODE", "\"poi\"")
        }
        create("nav") {
            dimension = "carMode"
            applicationIdSuffix = ".nav"
            versionNameSuffix = "-nav"
            manifestPlaceholders["carAppCategory"] = "androidx.car.app.category.NAVIGATION"
            buildConfigField("String", "CAR_APP_MODE", "\"navigation\"")
        }
    }

    buildTypes {
        debug {
            // Keep debug and locally release-signed builds installed simultaneously.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("org.maplibre.gl:android-sdk-opengl:13.0.2")
    implementation("androidx.car.app:app:1.7.0")
    implementation("androidx.car.app:app-projected:1.7.0")
}
