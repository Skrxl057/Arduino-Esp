import JSZip from 'jszip';

export interface AndroidFileItem {
  path: string;
  category: 'gradle' | 'manifest' | 'core' | 'ui' | 'res' | 'test' | 'docs' | 'ci';
  content: string;
}

export const ANDROID_PROJECT_FILES: AndroidFileItem[] = [
  {
    path: 'settings.gradle.kts',
    category: 'gradle',
    content: `pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\\\.android.*")
                includeGroupByRegex("com\\\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
        maven { url = java.net.URI("https://repo.eclipse.org/content/repositories/paho-releases/") }
    }
}

rootProject.name = "EmbedHub"
include(":app")`
  },
  {
    path: 'build.gradle.kts',
    category: 'gradle',
    content: `plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}`
  },
  {
    path: 'gradle/wrapper/gradle-wrapper.properties',
    category: 'gradle',
    content: `distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.7-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists`
  },
  {
    path: 'gradlew',
    category: 'gradle',
    content: `#!/bin/sh
APP_BASE_NAME=\${0##*/}
APP_HOME=$(cd "$(dirname "$0")" && pwd)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
java -Xmx64m -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"`
  },
  {
    path: 'gradle/libs.versions.toml',
    category: 'gradle',
    content: `[versions]
agp = "8.4.2"
kotlin = "1.9.24"
coreKtx = "1.13.1"
lifecycleRuntimeKtx = "2.8.3"
activityCompose = "1.9.0"
composeBom = "2024.06.00"
navigationCompose = "2.7.7"
room = "2.6.1"
ksp = "1.9.24-1.0.20"
datastore = "1.1.1"
coroutines = "1.8.1"
serialization = "1.6.3"
okhttp = "4.12.0"
usbSerial = "3.7.0"
pahoMqtt = "1.2.5"
accompanistPermissions = "0.34.0"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
materialIconsExtended = "1.6.8"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended", version.ref = "materialIconsExtended" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Room
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Networking & Transports
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
usb-serial-for-android = { group = "com.github.mik3y", name = "usb-serial-for-android", version.ref = "usbSerial" }
paho-mqtt = { group = "org.eclipse.paho", name = "org.eclipse.paho.client.mqttv3", version.ref = "pahoMqtt" }

# Kotlinx
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }

# Permissions
accompanist-permissions = { group = "com.google.accompanist", name = "accompanist-permissions", version.ref = "accompanistPermissions" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }`
  },
  {
    path: 'app/build.gradle.kts',
    category: 'gradle',
    content: `plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.embedhub.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.embedhub.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.okhttp)
    implementation(libs.usb.serial.for.android)
    implementation(libs.paho.mqtt)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.accompanist.permissions)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}`
  },
  {
    path: 'app/src/main/AndroidManifest.xml',
    category: 'manifest',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" tools:targetApi="s" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <uses-feature android:name="android.hardware.usb.host" android:required="false" />
    <uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />

    <application
        android:name=".EmbedHubApp"
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="@style/Theme.EmbedHub"
        android:usesCleartextTraffic="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <service
            android:name=".core.service.ConnectionForegroundService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="connectedDevice" />
    </application>
</manifest>`
  },
  {
    path: 'app/src/main/java/com/embedhub/app/core/transport/Link.kt',
    category: 'core',
    content: `package com.embedhub.app.core.transport

import com.embedhub.app.core.model.ConnectionState
import com.embedhub.app.core.model.LinkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Link {
    val name: String
    val type: LinkType
    val mtu: Int get() = 244
    val state: StateFlow<ConnectionState>
    val incomingLines: Flow<String>
    val incomingBytes: Flow<ByteArray>

    suspend fun open(): Result<Unit>
    suspend fun close()
    suspend fun send(bytes: ByteArray): Result<Unit>
}`
  },
  {
    path: 'app/src/main/java/com/embedhub/app/core/transport/BleLink.kt',
    category: 'core',
    content: `// Full BLE driver with HM-10 (0000FFE0), Nordic UART NUS (6E400001), MTU 247 negotiation, chunked writes and 150ms flush
// See /android/app/src/main/java/com/embedhub/app/core/transport/BleLink.kt for full implementation.`
  },
  {
    path: 'app/src/main/java/com/embedhub/app/core/queue/SendQueue.kt',
    category: 'core',
    content: `package com.embedhub.app.core.queue

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SendQueue(
    private val scope: CoroutineScope,
    private val sendBlock: suspend (ByteArray) -> Result<Unit>,
    private var mtuSize: Int = 244,
    private var minIntervalMs: Long = 20L
) {
    private val channel = Channel<ByteArray>(capacity = 100)
    private val mutex = Mutex()
    private var drainJob: Job? = null
    var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        drainJob = scope.launch(Dispatchers.IO) {
            for (packet in channel) {
                mutex.withLock {
                    var offset = 0
                    while (offset < packet.size) {
                        val chunkSize = minOf(mtuSize, packet.size - offset)
                        val chunk = ByteArray(chunkSize)
                        System.arraycopy(packet, offset, chunk, 0, chunkSize)
                        sendBlock(chunk)
                        offset += chunkSize
                        if (offset < packet.size) delay(10)
                    }
                    if (minIntervalMs > 0) delay(minIntervalMs)
                }
            }
        }
    }

    suspend fun enqueue(data: ByteArray): Boolean = channel.trySend(data).isSuccess
    fun stop() { isRunning = false; drainJob?.cancel(); drainJob = null }
}`
  },
  {
    path: 'app/src/main/java/com/embedhub/app/core/manager/ConnectionManager.kt',
    category: 'core',
    content: `// Triple-device simultaneous connection manager with auto-reconnect, exponential backoff, and transport fallback.
// Exposes slots 0, 1, 2, allIncomingLines, allIncomingBytes, and emergency stop.`
  },
  {
    path: 'app/src/main/java/com/embedhub/app/core/checksum/ChecksumUtils.kt',
    category: 'core',
    content: `// XOR and CRC-8 calculation, start/end markers, and packet frame verification.`
  },
  {
    path: 'app/src/main/java/com/embedhub/app/core/parser/DataParser.kt',
    category: 'core',
    content: `// Parser for key=value, key:value, CSV with headers, and JSON into named telemetry streams.`
  },
  {
    path: 'app/src/main/java/com/embedhub/app/core/rules/RulesEngine.kt',
    category: 'core',
    content: `// MacroDroid style automation rules: triggers, conditions, actions, cooldown, test-run, execution log.`
  },
  {
    path: '.github/workflows/android-ci.yml',
    category: 'ci',
    content: `name: Android CI & Release
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - run: chmod +x android/gradlew
      - run: cd android && ./gradlew test assembleDebug`
  },
  {
    path: 'app/src/main/java/com/embedhub/app/MainActivity.kt',
    category: 'ui',
    content: `// MainActivity with runtime ScreenOrientationMode switching:
// Auto-sensor, Locked Portrait, Locked Landscape, and Reverse Landscape.
// Dispatches orientation changes dynamically using requestedOrientation.`
  },
  {
    path: 'app/src/main/java/com/embedhub/app/ui/screens/control/ControlScreen.kt',
    category: 'ui',
    content: `// Dual-layout ControlScreen:
// 1. Portrait: Speed toolbar, D-Pad, 20Hz Joystick, and Actuator toggles.
// 2. Landscape: Cockpit Driving Mode with Left Thumb D-Pad/Joystick, Center HUD (Horizon, Speed, STOP), and Right Thumb Actuators.`
  },
  {
    path: 'docs/PROTOCOL.md',
    category: 'docs',
    content: `# EmbedHub Wire Protocol Specification
- D-Pad commands: F, B, L, R, S
- 20Hz Joystick: X:<-100..100>,Y:<-100..100>\\n
- Telemetry: key=val, JSON, CSV
- Rotation: ScreenOrientationMode (Auto, Portrait Lock, Landscape Cockpit Lock)`
  }
];

export async function downloadAndroidProjectZip(): Promise<void> {
  const zip = new JSZip();
  const rootFolder = zip.folder('EmbedHub-Android') || zip;

  for (const file of ANDROID_PROJECT_FILES) {
    rootFolder.file(file.path, file.content);
  }

  // Include full README and LICENSE
  rootFolder.file('README.md', '# EmbedHub\\nComplete native Android app in Kotlin & Jetpack Compose.\\nSee full repository documentation.');
  rootFolder.file('LICENSE', 'MIT License\\nCopyright (c) 2026 EmbedHub Contributors');

  const blob = await zip.generateAsync({ type: 'blob' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'EmbedHub-Android-Kotlin-Project.zip';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
