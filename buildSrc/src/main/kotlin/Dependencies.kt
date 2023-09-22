object Dependencies {

    object Androidx {
        const val coreKtx = "androidx.core:core-ktx:${Versions.AndroidX.coreKtx}"
    }

    object Test {
        const val junit = "junit:junit:${Versions.Test.junit}"
        const val extJunit = "androidx.test.ext:junit:${Versions.Test.extJunit}"
        const val runner = "androidx.test:runner:${Versions.Test.runner}"
        const val espresso = "androidx.test.espresso:espresso-core:${Versions.Test.espresso}"
        const val mockitoAndroid = "org.mockito:mockito-android:${Versions.Test.mockitoAndroid}"
        const val orgJson = "org.json:json:${Versions.Test.orgJson}"
    }

    object Crypto {
        const val web3jCrypto = "org.web3j:crypto:${Versions.Crypto.web3jCrypto}"
        const val trustWallet = "com.trustwallet:wallet-core:${Versions.Crypto.trustWallet}"
    }

    object Timber {
        const val timber = "com.jakewharton.timber:timber:${Versions.Timber.timberVersion}"
    }

    object Gradle {
        const val gradle = "com.android.tools.build:gradle:${Versions.Gradle.gradle}"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.Gradle.gradlePlugin}"
    }

}
