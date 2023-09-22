plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdk = Versions.compileSdk

    packagingOptions {
        exclude("META-INF/INDEX.LIST")
    }
    // will be used in library don't change
    defaultConfig {
        minSdk = Versions.minSdk
        targetSdk = Versions.targetSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = false
    }
}

dependencies {
    implementation(Dependencies.Androidx.coreKtx)
    implementation(Dependencies.Crypto.trustWallet)

    testImplementation(Dependencies.Test.junit)
    testImplementation(Dependencies.Test.orgJson)
    androidTestImplementation(Dependencies.Test.extJunit)
    androidTestImplementation(Dependencies.Test.mockitoAndroid)
    androidTestImplementation(Dependencies.Test.espresso)
}
