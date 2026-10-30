import React, { useState } from 'react';
import { Copy, Share2, CloudUpload, Check, Code, FileCode } from 'lucide-react';

interface Sketch {
  id: string;
  title: string;
  board: string;
  desc: string;
  code: string;
}

const SKETCHES: Sketch[] = [
  {
    id: 'esp32_ble',
    title: 'ESP32 BLE UART (Nordic NUS)',
    board: 'ESP32 / ESP32-S3 / C3',
    desc: 'Auto-detected by EmbedHub BLE scanner with high-speed notifications and bidirectional telemetry.',
    code: `/*
 * EmbedHub - ESP32 BLE UART (Nordic NUS)
 * Auto-detected in EmbedHub BLE Scanner!
 */
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define SERVICE_UUID           "6E400001-B5A3-F393-A0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-A0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-A0A9-E50E24DCCA9E"

BLECharacteristic *pTxCharacteristic;
bool deviceConnected = false;

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) { deviceConnected = true; }
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
            Serial.println(rxValue.c_str());
            pTxCharacteristic->setValue("ACK\\n");
            pTxCharacteristic->notify();
        }
    }
};

void setup() {
    Serial.begin(115200);
    BLEDevice::init("EmbedHub-ESP32");
    BLEServer *pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    BLEService *pService = pServer->createService(SERVICE_UUID);
    pTxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_TX, BLECharacteristic::PROPERTY_NOTIFY);
    pTxCharacteristic->addDescriptor(new BLE2902());

    BLECharacteristic *pRxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_RX, BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
    pRxCharacteristic->setCallbacks(new MyCallbacks());

    pService->start();
    pServer->getAdvertising()->start();
    Serial.println("BLE UART ready!");
}

void loop() {
    if (deviceConnected) {
        // Stream sample telemetry to EmbedHub Dashboard
        static unsigned long last = 0;
        if (millis() - last > 1000) {
            last = millis();
            char buf[64];
            snprintf(buf, sizeof(buf), "temp=%.1f,humidity=55,dist=80\\n", 24.5 + (random(-5, 5)/10.0));
            pTxCharacteristic->setValue(buf);
            pTxCharacteristic->notify();
        }
    }
    delay(20);
}`
  },
  {
    id: 'esp8266_robot',
    title: 'ESP8266 WiFi Robot Car (/State=)',
    board: 'ESP8266 NodeMCU / D1 Mini',
    desc: 'Matches EmbedHub Control Screen D-Pad (F/B/L/R/S) over HTTP web server.',
    code: `/*
 * EmbedHub - ESP8266 WiFi Robot Car (HTTP ?State=F/B/L/R/S)
 */
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>

ESP8266WebServer server(80);

void handleState() {
    if (server.hasArg("State")) {
        String cmd = server.arg("State");
        char c = cmd.charAt(0);
        switch (c) {
            case 'F': /* Drive forward */ break;
            case 'B': /* Drive backward */ break;
            case 'L': /* Turn left */ break;
            case 'R': /* Turn right */ break;
            case 'S': /* Failsafe STOP */ break;
        }
        server.send(200, "text/plain", "OK");
    } else {
        server.send(400, "text/plain", "Missing State");
    }
}

void setup() {
    Serial.begin(115200);
    WiFi.softAP("EmbedHub-Robot", "12345678");
    server.on("/", handleState);
    server.begin();
    Serial.println("Robot Car Web Server Running!");
}

void loop() {
    server.handleClient();
}`
  },
  {
    id: 'joystick_differential',
    title: '20Hz Differential Steering Parser',
    board: 'ESP32 / Arduino Uno',
    desc: 'Parses continuous X:<-100..100>,Y:<-100..100> into Left and Right PWM motor percentages.',
    code: `/*
 * EmbedHub - 20Hz Analog Joystick Differential Steering
 */
void setup() {
    Serial.begin(115200);
}

void drive(int x, int y) {
    int left = constrain(y + x, -100, 100);
    int right = constrain(y - x, -100, 100);
    Serial.printf("Motors -> Left: %d%% | Right: %d%%\\n", left, right);
}

void loop() {
    if (Serial.available()) {
        String s = Serial.readStringUntil('\\n');
        s.trim();
        if (s.startsWith("X:") && s.indexOf("Y:") != -1) {
            int yIdx = s.indexOf("Y:");
            int x = s.substring(2, yIdx).toInt();
            int y = s.substring(yIdx + 2).toInt();
            drive(x, y);
        } else if (s == "S") {
            drive(0, 0); // Failsafe STOP!
        }
    }
}`
  }
];

export const SketchesView: React.FC = () => {
  const [selectedSketch, setSelectedSketch] = useState<Sketch>(SKETCHES[0]);
  const [copied, setCopied] = useState(false);
  const [otaProgress, setOtaProgress] = useState(0);
  const [isOtaActive, setIsOtaActive] = useState(false);
  const [otaStatus, setOtaStatus] = useState('');

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedSketch.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  const simulateOta = () => {
    setIsOtaActive(true);
    setOtaProgress(0);
    setOtaStatus('Connecting to ESP OTA port 80...');

    let p = 0;
    const interval = setInterval(() => {
      p += 10;
      setOtaProgress(p);
      if (p >= 100) {
        clearInterval(interval);
        setIsOtaActive(false);
        setOtaStatus('OTA Firmware flash complete! ESP is rebooting...');
      } else {
        setOtaStatus(`Flashing firmware binary... ${p}%`);
      }
    }, 200);
  };

  return (
    <div className="flex flex-col h-full bg-slate-950 text-slate-100 p-4 overflow-y-auto">
      <div className="flex items-center justify-between gap-3 mb-4">
        <div>
          <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
            <Code className="w-5 h-5 text-sky-400" />
            <span>Sketches & OTA Firmware Studio</span>
          </h2>
          <p className="text-xs text-slate-400">
            Pre-configured production sketches for Arduino, ESP32, and ESP8266 + HTTP OTA Flasher
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 flex-1 min-h-0">
        {/* Sketches Sidebar */}
        <div className="space-y-2">
          <div className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">
            Select Template
          </div>
          {SKETCHES.map((s) => (
            <button
              key={s.id}
              onClick={() => setSelectedSketch(s)}
              className={`w-full text-left p-3 rounded-xl border transition-all text-xs ${
                selectedSketch.id === s.id
                  ? 'bg-sky-600/20 border-sky-500 text-sky-200'
                  : 'bg-slate-900 border-slate-800 hover:border-slate-700 text-slate-300'
              }`}
            >
              <div className="font-bold">{s.title}</div>
              <div className="text-[10px] text-slate-400 mt-0.5">{s.board}</div>
            </button>
          ))}

          {/* OTA Updater Box */}
          <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 mt-6 space-y-3">
            <div className="flex items-center gap-2 font-bold text-xs text-slate-200">
              <CloudUpload className="w-4 h-4 text-emerald-400" />
              <span>Web OTA Flasher</span>
            </div>
            <p className="text-[11px] text-slate-400">
              Upload compiled <code>.bin</code> files to ESP8266/ESP32 directly over WiFi.
            </p>

            <button
              onClick={simulateOta}
              disabled={isOtaActive}
              className="w-full py-2 px-3 rounded-lg bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-800 text-white font-semibold text-xs transition-all shadow-md shadow-emerald-600/20"
            >
              {isOtaActive ? 'Flashing OTA...' : 'Simulate OTA Flash (.bin)'}
            </button>

            {isOtaActive && (
              <div className="space-y-1">
                <div className="h-2 w-full bg-slate-800 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-emerald-500 transition-all duration-200"
                    style={{ width: `${otaProgress}%` }}
                  />
                </div>
                <div className="text-[10px] text-slate-400 text-right">{otaProgress}%</div>
              </div>
            )}

            {otaStatus && <div className="text-[11px] text-emerald-300 font-medium">{otaStatus}</div>}
          </div>
        </div>

        {/* Code Viewer */}
        <div className="lg:col-span-3 flex flex-col bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 bg-slate-950/60 border-b border-slate-800">
            <div>
              <div className="font-bold text-sm text-slate-200">{selectedSketch.title}</div>
              <div className="text-xs text-slate-400">{selectedSketch.desc}</div>
            </div>

            <button
              onClick={handleCopy}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-sky-400 text-xs font-semibold border border-slate-700 transition-all"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copied ? 'Copied!' : 'Copy Sketch'}</span>
            </button>
          </div>

          <div className="flex-1 p-4 overflow-y-auto font-mono text-xs text-slate-300 bg-slate-950/40 select-all leading-relaxed whitespace-pre">
            {selectedSketch.code}
          </div>
        </div>
      </div>
    </div>
  );
};
