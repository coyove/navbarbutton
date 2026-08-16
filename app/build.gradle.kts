plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.navbarbutton"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.navbarbutton"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    // LSPosed/libxposed API 101.1 is compile-only: the framework provides it at runtime.
    compileOnly("io.github.libxposed:api:101.0.1")
}
