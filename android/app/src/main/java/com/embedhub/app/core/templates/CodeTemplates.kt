package com.embedhub.app.core.templates

data class SketchTemplate(
    val id: String,
    val title: String,
    val board: String,
    val description: String,
    val code: String
)

object CodeTemplates {

    val allTemplates: List<SketchTemplate> = listOf(
        SketchTemplate(
            id = "esp32_ble_uart",
            title = "ESP32 BLE UART (Nordic NUS)",
            board = "ESP32 / ESP32-S3 / C3",
            description = "High-throughput Nordic UART Service compatible with EmbedHub BLE scanner and terminal.",
            code = """
/*
 * EmbedHub - ESP32 BLE UART (Nordic NUS)
 * Compatible with HM-10 / Nordic UART auto-detection in EmbedHub.
 */
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define SERVICE_UUID           "6E400001-B5A3-F393-A0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-A0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-A0A9-E50E24DCCA9E"

BLEServer *pServer = NULL;
BLECharacteristic *pTxCharacteristic;
bool deviceConnected = false;

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
    }
    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        pServer->getAdvertising()->start();
    }
};

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        std::string rxValue = pCharacteristic->getValue();
        if (rxValue.length() > 0) {
            Serial.print("EmbedHub Command: ");
            for (int i = 0; i < rxValue.length(); i++) {
                Serial.print(rxValue[i]);
            }
            Serial.println();
            
            // Echo back with status
            std::string reply = "ACK: " + rxValue + "\n";
            pTxCharacteristic->setValue(reply);
            pTxCharacteristic->notify();
        }
    }
};

void setup() {
    Serial.begin(115200);
    BLEDevice::init("EmbedHub-ESP32");
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    BLEService *pService = pServer->createService(SERVICE_UUID);
    pTxCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID_TX,
        BLECharacteristic::PROPERTY_NOTIFY
    );
    pTxCharacteristic->addDescriptor(new BLE2902());

    BLECharacteristic *pRxCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID_RX,
        BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR
    );
    pRxCharacteristic->setCallbacks(new MyCallbacks());

    pService->start();
    pServer->getAdvertising()->start();
    Serial.println("EmbedHub BLE UART ready for pairing!");
}

void loop() {
    if (deviceConnected) {
        // Send simulated telemetry every second: key=value
        static unsigned long lastSend = 0;
        if (millis() - lastSend > 1000) {
            lastSend = millis();
            int pot = analogRead(34);
            char buffer[32];
            snprintf(buffer, sizeof(buffer), "pot=%d,uptime=%lu\n", pot, millis() / 1000);
            pTxCharacteristic->setValue(buffer);
            pTxCharacteristic->notify();
        }
    }
    delay(20);
}
""".trimIndent()
        ),

        SketchTemplate(
            id = "esp8266_robot_car",
            title = "ESP8266 WiFi Robot Car (HTTP /?State=)",
            board = "ESP8266 NodeMCU / WeMos D1",
            description = "Handles EmbedHub Control Screen D-Pad commands (F, B, L, R, S) over HTTP or SoftAP.",
            code = """
/*
 * EmbedHub - ESP8266 WiFi Robot Car Controller
 * Compatible with EmbedHub D-Pad: F (forward), B (backward), L (left), R (right), S (stop)
 */
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>

const char *ssid = "EmbedHub-Robot";
const char *password = "12345678";

ESP8266WebServer server(80);

// Motor Pins (L298N / TB6612FNG)
const int ENA = D1;
const int IN1 = D2;
const int IN2 = D3;
const int IN3 = D4;
const int IN4 = D5;
const int ENB = D6;

void stopMotors() {
    digitalWrite(IN1, LOW); digitalWrite(IN2, LOW);
    digitalWrite(IN3, LOW); digitalWrite(IN4, LOW);
}

void forward() {
    digitalWrite(IN1, HIGH); digitalWrite(IN2, LOW);
    digitalWrite(IN3, HIGH); digitalWrite(IN4, LOW);
}

void backward() {
    digitalWrite(IN1, LOW); digitalWrite(IN2, HIGH);
    digitalWrite(IN3, LOW); digitalWrite(IN4, HIGH);
}

void turnLeft() {
    digitalWrite(IN1, LOW); digitalWrite(IN2, HIGH);
    digitalWrite(IN3, HIGH); digitalWrite(IN4, LOW);
}

void turnRight() {
    digitalWrite(IN1, HIGH); digitalWrite(IN2, LOW);
    digitalWrite(IN3, LOW); digitalWrite(IN4, HIGH);
}

void handleCommand() {
    if (server.hasArg("State")) {
        String cmd = server.arg("State");
        char c = cmd.charAt(0);
        switch (c) {
            case 'F': forward(); break;
            case 'B': backward(); break;
            case 'L': turnLeft(); break;
            case 'R': turnRight(); break;
            case 'S': stopMotors(); break;
            default: break;
        }
        server.send(200, "text/plain", "OK:" + cmd);
    } else {
        server.send(400, "text/plain", "Missing State parameter");
    }
}

void setup() {
    Serial.begin(115200);
    pinMode(ENA, OUTPUT); pinMode(IN1, OUTPUT); pinMode(IN2, OUTPUT);
    pinMode(IN3, OUTPUT); pinMode(IN4, OUTPUT); pinMode(ENB, OUTPUT);
    analogWrite(ENA, 800); analogWrite(ENB, 800);
    stopMotors();

    WiFi.softAP(ssid, password);
    Serial.print("SoftAP IP: ");
    Serial.println(WiFi.softAPIP());

    server.on("/", handleCommand);
    server.begin();
    Serial.println("Robot car HTTP server started");
}

void loop() {
    server.handleClient();
}
""".trimIndent()
        ),

        SketchTemplate(
            id = "analog_joystick_parser",
            title = "ESP32 20Hz Analog Joystick & Speed Parser",
            board = "ESP32 / Arduino Uno",
            description = "Parses differential steering from EmbedHub joystick format: X:<-100..100>,Y:<-100..100>",
            code = """
/*
 * EmbedHub - Analog Joystick 20Hz Parser with Differential Steering
 * Receives: X:<int>,Y:<int>\n  (values between -100 and +100)
 */
void setup() {
    Serial.begin(115200);
    Serial.println("Waiting for EmbedHub Joystick data...");
}

void processDrive(int x, int y) {
    // Differential motor mixing
    int leftMotor = y + x;
    int rightMotor = y - x;
    leftMotor = constrain(leftMotor, -100, 100);
    rightMotor = constrain(rightMotor, -100, 100);

    Serial.print("Motors -> Left: ");
    Serial.print(leftMotor);
    Serial.print("% | Right: ");
    Serial.print(rightMotor);
    Serial.println("%");
}

void loop() {
    if (Serial.available()) {
        String line = Serial.readStringUntil('\n');
        line.trim();
        if (line.startsWith("X:") && line.indexOf("Y:") != -1) {
            int yIndex = line.indexOf("Y:");
            String xStr = line.substring(2, line.indexOf(',', 2) != -1 ? line.indexOf(',', 2) : yIndex);
            xStr.replace(",", "");
            xStr.trim();
            String yStr = line.substring(yIndex + 2);
            yStr.trim();

            int xVal = xStr.toInt();
            int yVal = yStr.toInt();
            processDrive(xVal, yVal);
        } else if (line == "S") {
            processDrive(0, 0); // Failsafe Stop
        }
    }
}
""".trimIndent()
        ),

        SketchTemplate(
            id = "sensor_json_sender",
            title = "ESP32 Multi-Sensor JSON Streamer",
            board = "ESP32 / ESP8266",
            description = "Streams structured JSON telemetry for EmbedHub live gauges, graphs, and automation rules.",
            code = """
/*
 * EmbedHub - Sensor JSON Telemetry Streamer
 * Automatically visualized in EmbedHub Dashboard widgets & charts.
 */
#include <ArduinoJson.h> // ArduinoJson library v6 or v7

void setup() {
    Serial.begin(115200);
}

void loop() {
    StaticJsonDocument<200> doc;
    
    // Replace with real sensors: BME280, MPU6050, Ultrasonic, Battery
    float temperature = 24.2 + (random(-5, 6) / 10.0);
    float humidity = 55.0 + (random(-10, 11) / 10.0);
    int distanceCm = random(10, 180);
    float batteryVolts = 3.82 - (millis() / 1000000.0);
    
    doc["temp"] = temperature;
    doc["humidity"] = humidity;
    doc["dist"] = distanceCm;
    doc["vbat"] = batteryVolts;
    doc["alert"] = (distanceCm < 30) ? 1 : 0;

    serializeJson(doc, Serial);
    Serial.println(); // EmbedHub line delimiter

    delay(200); // 5 Hz stream
}
""".trimIndent()
        ),

        SketchTemplate(
            id = "ble_wifi_provisioning",
            title = "ESP32 BLE WiFi Provisioner",
            board = "ESP32",
            description = "Receives WiFi SSID and password via EmbedHub BLE provisioning screen and connects.",
            code = """
/*
 * EmbedHub - BLE WiFi Provisioning Receiver
 * Listens for JSON: {"ssid":"...","pass":"...","hostname":"..."}
 */
#include <WiFi.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <ArduinoJson.h>

#define PROV_SERVICE_UUID "0000FFE0-0000-1000-8000-00805F9B34FB"
#define PROV_CHAR_UUID    "0000FFE1-0000-1000-8000-00805F9B34FB"

BLECharacteristic *pCharacteristic;

class ProvisionCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pChar) {
        std::string val = pChar->getValue();
        if (val.length() > 0) {
            StaticJsonDocument<256> doc;
            DeserializationError err = deserializeJson(doc, val.c_str());
            if (!err) {
                const char* ssid = doc["ssid"];
                const char* pass = doc["pass"];
                Serial.printf("Connecting to SSID: %s\n", ssid);
                WiFi.begin(ssid, pass);
                int tries = 0;
                while (WiFi.status() != WL_CONNECTED && tries < 20) {
                    delay(500);
                    Serial.print(".");
                    tries++;
                }
                if (WiFi.status() == WL_CONNECTED) {
                    String ip = WiFi.localIP().toString();
                    String reply = "CONNECTED:" + ip + "\n";
                    pChar->setValue(reply.c_str());
                    pChar->notify();
                    Serial.println("\nWiFi Connected! IP: " + ip);
                } else {
                    pChar->setValue("AUTH_FAILED\n");
                    pChar->notify();
                }
            }
        }
    }
};

void setup() {
    Serial.begin(115200);
    BLEDevice::init("ESP32-Provision");
    BLEServer *pServer = BLEDevice::createServer();
    BLEService *pService = pServer->createService(PROV_SERVICE_UUID);
    pCharacteristic = pService->createCharacteristic(
        PROV_CHAR_UUID,
        BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_NOTIFY
    );
    pCharacteristic->setCallbacks(new ProvisionCallbacks());
    pCharacteristic->addDescriptor(new BLE2902());
    pService->start();
    pServer->getAdvertising()->start();
    Serial.println("Send WiFi credentials from EmbedHub BLE Provisioning!");
}

void loop() {
    delay(100);
}
""".trimIndent()
        ),

        SketchTemplate(
            id = "esp_ota_update",
            title = "ESP8266/ESP32 Web OTA Firmware Receiver",
            board = "ESP8266 / ESP32",
            description = "Enables HTTP OTA updates directly from the EmbedHub Sketches & OTA Screen.",
            code = """
/*
 * EmbedHub - HTTP Web OTA Update Server
 * Upload .bin directly from the EmbedHub app OTA Flashing screen.
 */
#ifdef ESP32
#include <WiFi.h>
#include <WebServer.h>
#include <Update.h>
WebServer server(80);
#else
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
ESP8266WebServer server(80);
#endif

const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

void setup() {
    Serial.begin(115200);
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nConnected! IP: " + WiFi.localIP().toString());

    server.on("/update", HTTP_POST, []() {
        server.sendHeader("Connection", "close");
        server.send(200, "text/plain", (Update.hasError()) ? "FAIL" : "OK");
        ESP.restart();
    }, []() {
        HTTPUpload& upload = server.upload();
        if (upload.status == UPLOAD_FILE_START) {
            Serial.printf("Update: %s\n", upload.filename.c_str());
            if (!Update.begin(UPDATE_SIZE_UNKNOWN)) {
                Update.printError(Serial);
            }
        } else if (upload.status == UPLOAD_FILE_WRITE) {
            if (Update.write(upload.buf, upload.currentSize) != upload.currentSize) {
                Update.printError(Serial);
            }
        } else if (upload.status == UPLOAD_FILE_END) {
            if (Update.end(true)) {
                Serial.printf("Update Success: %u bytes\nRebooting...\n", upload.totalSize);
            } else {
                Update.printError(Serial);
            }
        }
    });

    server.begin();
    Serial.println("OTA server ready at /update");
}

void loop() {
    server.handleClient();
}
""".trimIndent()
        )
    )
}
