# EmbedHub 🚀
### Universal All-in-One Controller & Telemetry Studio for Arduino & ESP Boards

[![Android CI](https://github.com/your-username/EmbedHub/actions/workflows/android-ci.yml/badge.svg)](https://github.com/your-username/EmbedHub/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](https://android.com)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-purple.svg)](https://developer.android.com/jetpack/compose)

**EmbedHub** is an open-source, production-grade native Android application engineered in **Kotlin** and **Jetpack Compose (Material 3)**. It consolidates and replaces disjointed maker tools — **BLE Scanner, Serial Bluetooth Terminal, MacroDroid, Dabble, IoT Remote, Arduino Bluetooth Controller, ESP8266 WiFi Robot Car, and ArduinoDroid** — into one controller for embedded hardware.

---

## 🌟 Key Features

### 🔌 Multi-Transport Hardware Connectivity
- **Bluetooth Low Energy (BLE)**: Auto-detects **HM-10 / HM-11** (service `0000FFE0`), **Nordic UART (NUS)**, or generic notify+write pairs. Automatic MTU 247 negotiation, chunked writes, and 150 ms text flushes. Includes a full **GATT Browser**.
- **Classic Bluetooth (SPP)**: Standard RFCOMM SPP (`00001101-0000-1000-8000-00805F9B34FB`) for **HC-05, HC-06, and ESP32 `BluetoothSerial`**.
- **USB OTG Serial**: Plug-and-play driver support via `usb-serial-for-android` for **CH340, CP210x, FTDI, Arduino Uno/Mega/Nano, and ESP32 USB-JTAG**. Configurable baud rates (up to 921600), parity, stop bits, and DTR/RTS reset toggle.
- **WiFi TCP & UDP**: High-speed raw sockets and low-latency UDP datagrams for telemetry and real-time robotics.
- **WebSocket (`ws://`) & HTTP**: Native `ws://` client + HTTP command dispatcher (`GET /?State=<cmd>` for ESP8266 robot car sketches) with customizable URL templates.
- **MQTT Broker**: Full TLS support, broker URI, client ID, QoS 0/1/2, persistent subscribe topics, and publish queues.
- **mDNS / NSD Discovery**: Zero-config auto-discovery of local ESP32 / ESP8266 microcontrollers on your local WiFi.
- **BLE WiFi Provisioning**: Seamlessly push local WiFi credentials (SSID/Password) to unconfigured ESP32 boards over BLE using JSON.
- **Triple Device Manager**: Connect and orchestrate up to **3 hardware devices concurrently** (Slots A, B, C) with independent colors, queues, and stats.
- **Android 14 Foreground Service**: Keeps connections, failsafe triggers, and automations running with the screen locked or app backgrounded (`foregroundServiceType="connectedDevice"`).

---

### 💻 Terminal Screen
- Bi-directional timestamped ASCII & HEX logs with TX (cyan) and RX (emerald) color-coding.
- Configurable line endings: `None`, `\n` (LF), `\r` (CR), `\r\n` (CRLF).
- Fast macro buttons with one-shot execution or recurring loop intervals.
- Live search & regex filtering, auto-scroll toggle, byte counters, and instant `.txt` export via Android Share Sheet.

---

### 🕹️ Control Screen (Robot Car & Drone Driving Cockpit)
- **Hold-to-Move D-Pad**: Default `F`, `B`, `L`, `R` commands, automatically issuing `S` (stop) on finger release.
- **20Hz Analog Joystick**: Smooth differential drive transmission (`X:<-100..100>,Y:<-100..100>`) with deadzone and exponential sensitivity curve configuration.
- **Dynamic Screen Rotation & Landscape Cockpit Mode**:
  - One-tap orientation switching (`Auto-Sensor`, `Locked Portrait`, `Locked Landscape Cockpit`, `Reverse Landscape`).
  - Dedicated **Landscape Driving Cockpit**: transforms the smartphone into a dual-thumb console gamepad with Left Thumb D-Pad/Joystick, Center Pilot HUD with Artificial Horizon & Emergency STOP, and Right Thumb vehicle actuators (Horn, Lights, Aux Relay, Servo).
- **Artificial Horizon & Gyro Tilt Steering**: Real-time roll/pitch horizon indicator using the smartphone's internal 3-axis accelerometer sensor.
- **Hardware Failsafe**: Guaranteed immediate STOP sent when finger lifts, app loses focus, or the connection drops.
- **Speed Slider**: Stepped 0–9 speed settings with continuous or on-release dispatch.
- **Custom Actuators**: Horn, Headlights, Aux Relays with latching toggle or momentary push-button modes.

---

### 📊 Dashboard Screen
- Stream parsing for `key=value`, `key:value`, CSV with headers, and JSON packets.
- Real-time widgets built with custom Jetpack Compose Canvas:
  - **Radial Sweep Gauges** with min/max clamps and units
  - **Live Multi-Series Line Charts** with rolling historical buffers
  - **Digital Value Tiles**, **LED Indicators**, **Switches**, and **Sliders**
- **JSON Layout Export & Import**: Save your tailored dashboard widget configuration to JSON and restore it on any device.
- One-click numerical telemetry export to `.csv` files.

---

### ⚡ MacroDroid-Style Automation
- **Triggers**: Telemetry thresholds (`temp > 40`), value changes, periodic cron intervals, connection loss/gain, battery low, phone shake.
- **Conditions**: Boolean logic trees (AND / OR).
- **Actions**: Dispatch hardware commands, push system notifications, haptic vibrations, Text-to-Speech (TTS), and chain delays.
- **JSON Script Export & Restore**: Backup all automation rules, triggers, and action chains to portable JSON.

---

### 📁 Sketches & OTA Screen
- Comprehensive built-in Arduino/ESP32 reference sketches ready to copy/share.
- **Web OTA Flasher**: Pick `.bin` firmware binaries from Android storage and flash ESP8266 / ESP32 over HTTP with real-time percentage progress bars.

---

## 🛠️ Supported Hardware

| Hardware Module | Connection Transport | Supported Feature |
| :--- | :--- | :--- |
| **ESP32 / ESP32-S3 / ESP32-C3** | BLE, Classic BT, WiFi, USB OTG | Nordic UART, HTTP Robot Car, WebSockets, OTA, MQTT |
| **ESP8266 (NodeMCU, WeMos D1)** | WiFi (TCP, UDP, WebSocket, HTTP) | HTTP Robot Car, mDNS, OTA Flashing |
| **Arduino Uno / Nano / Mega / Leonardo** | USB OTG (CH340, CP210x, FTDI) | Full duplex 115200 baud serial & D-Pad control |
| **HM-10 / HM-11 / AT-09** | BLE | Auto-detection on Service `0000FFE0` |
| **HC-05 / HC-06** | Classic Bluetooth SPP | RFCOMM SPP standard pairing |

---

## 🏗️ Building and Running

### Prerequisites
- Android Studio Iguana (2023.2.1) or newer
- JDK 17
- Android SDK 34 (Android 14)

### Command-Line Build
```bash
cd android
./gradlew assembleDebug
```
The compiled APK will be located at:
`android/app/build/outputs/apk/debug/app-debug.apk`

### Running Unit Tests
```bash
cd android
./gradlew test
```

---

## 🔐 GitHub Actions CI & Keystore Secrets

The project includes automated CI (`.github/workflows/android-ci.yml`):
- Runs tests and builds `app-debug.apk` on every push and pull request.
- Automatically signs and attaches `app-release.apk` to GitHub Releases whenever a `v*` tag is pushed.

### Required GitHub Secrets for Release Signing:
- `KEYSTORE_BASE64`: Base64 encoded `.jks` release keystore file (`base64 -w 0 release.jks`).
- `KEYSTORE_PASSWORD`: Keystore password.
- `KEY_ALIAS`: Key alias name.
- `KEY_PASSWORD`: Key password.

---

## 📄 License
This project is open-source under the [MIT License](LICENSE).
