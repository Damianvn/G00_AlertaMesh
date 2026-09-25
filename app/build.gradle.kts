plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "pe.edu.ucsm.alertamesh"
    compileSdk = 34

    defaultConfig {
        applicationId = "pe.edu.ucsm.alertamesh"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Nearby Connections: usa Bluetooth LE, Bluetooth Clásico y Wi-Fi Direct/LAN por debajo
    implementation("com.google.android.gms:play-services-nearby:19.3.0")
    testImplementation("junit:junit:4.13.2")
}
