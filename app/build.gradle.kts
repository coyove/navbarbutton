plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.navbarbutton"
    compileSdk = 36
    ndkVersion = "29.0.14206865"

    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = "zzzzzz"
            keyAlias = "release"
            keyPassword = "zzzzzz"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    defaultConfig {
        applicationId = "com.example.navbarbutton"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += setOf("arm64-v8a")
        }
        externalNativeBuild {
            ndkBuild {
                arguments(
                    "APP_CFLAGS+=-DPKGNAME=hev/sockstun -ffile-prefix-map=${rootDir}=.",
                    "APP_LDFLAGS+=-Wl,--build-id=none"
                )
            }
        }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation("androidx.core:core:1.7.0")
    // LSPosed/libxposed API 101.1 is compile-only: the framework provides it at runtime.
    compileOnly("io.github.libxposed:api:101.0.1")
}
