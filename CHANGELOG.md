# Changelog

All notable changes to the EmbedHub project are documented in this file.

## [1.0.0] - Initial Open-Source Release
### Added
- Multi-transport layer: BLE (HM-10 & Nordic NUS auto-detect), Classic Bluetooth SPP (HC-05/06), USB OTG Serial (CH340, CP210x, FTDI), WiFi TCP/UDP, WebSocket (`ws://`), HTTP command sender, and MQTT with TLS.
- 3-device simultaneous ConnectionManager with independent slots and auto-reconnect fallback.
- SendQueue with MTU packet chunking and configurable rate-limiting (up to 50Hz).
- Terminal Screen with HEX/ASCII views, filter, byte counters, macros, and share sheet export.
- Control Screen with hold-to-move D-Pad, 20Hz analog joystick, speed slider 0-9, and instant emergency stop failsafe.
- Dashboard Screen with custom Jetpack Compose Canvas radial gauges, live scrolling line charts, LED indicators, switches, and CSV export.
- MacroDroid-style Automation Rules Engine supporting thresholds, triggers, AND/OR conditions, notifications, vibration, and TTS.
- Full sketches library & HTTP Web OTA firmware flasher for ESP8266 and ESP32.
- Room database for Profiles with JSON import/export.
- Full unit tests for DataParser, RulesEngine, ChecksumUtils, and SendQueue.
- GitHub Actions CI workflow for automated APK building and release tagging.
