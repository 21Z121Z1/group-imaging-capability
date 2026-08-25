plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.z121z1.oplusprivateapiprobe"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.z121z1.oplusprivateapiprobe"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
    }
}
