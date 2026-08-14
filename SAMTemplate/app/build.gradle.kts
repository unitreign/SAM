plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "fyi.reign.sam.shortcut.PLACEHOLDER"
    compileSdk = 36

    defaultConfig {
        applicationId = "fyi.reign.sam.shortcut.PLACEHOLDER"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin")
            java.setSrcDirs(emptyList<File>())
        }
        getByName("test") {
            kotlin.setSrcDirs(emptyList<File>())
            java.setSrcDirs(emptyList<File>())
        }
        getByName("androidTest") {
            kotlin.setSrcDirs(emptyList<File>())
            java.setSrcDirs(emptyList<File>())
        }
    }
}

dependencies {
}