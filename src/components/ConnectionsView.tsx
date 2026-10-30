import React, { useState } from 'react';
import { DeviceSlotState, SignalQualitySample, LinkType } from '../types';
import { SignalQualityGraph } from './SignalQualityGraph';
import {
  Radio,
  Bluetooth,
  Usb,
  Wifi,
  Cpu,
  RefreshCw,
  Sliders,
  CheckCircle2,
  AlertCircle,
  ExternalLink,
  ChevronRight,
  ShieldCheck,
  Zap,
  HardDrive
} from 'lucide-react';

interface ConnectionsViewProps {
  slots: DeviceSlotState[];
  selectedSlot: number;
  onSelectSlot: (index: number) => void;
  onToggleConnect: (slotIndex: number) => void;
  signalSamples: SignalQualitySample[];
  onSendPing: (slotIndex: number) => void;
  onUpdateSlotConfig: (slotIndex: number, config: Partial<DeviceSlotState>) => void;
}

export const ConnectionsView: React.FC<ConnectionsViewProps> = ({
  slots,
  selectedSlot,
  onSelectSlot,
  onToggleConnect,
  signalSamples,
  onSendPing,
  onUpdateSlotConfig
}) => {
  const activeSlot = slots[selectedSlot] || slots[0];

  const [baudRate, setBaudRate] = useState<number>(115200);
  const [dtrRts, setDtrRts] = useState<boolean>(true);
  const [mqttBroker, setMqttBroker] = useState<string>('broker.hivemq.com');
  const [mqttPort, setMqttPort] = useState<number>(1883);
  const [mqttTopic, setMqttTopic] = useState<string>('embedhub/car/cmd');
  const [wifiSsid, setWifiSsid] = useState<string>('Home_Robotics_2.4G');
  const [wifiPass, setWifiPass] = useState<string>('');
  const [isScanningMdns, setIsScanningMdns] = useState<boolean>(false);
  const [discoveredDevices, setDiscoveredDevices] = useState<
    { name: string; ip: string; port: number; service: string }[]
  >([
    { name: 'ESP32-RobotCar.local', ip: '192.168.1.142', port: 8080, service: '_arduino._tcp' },
    { name: 'ESP8266-Telemetry.local', ip: '192.168.1.155', port: 81, service: '_http._tcp' }
  ]);

  const handleScanMdns = () => {
    setIsScanningMdns(true);
    setTimeout(() => {
      setIsScanningMdns(false);
      setDiscoveredDevices((prev) => [
        ...prev,
        {
          name: `ESP32-Node-${Math.floor(Math.random() * 900 + 100)}.local`,
          ip: `192.168.1.${Math.floor(Math.random() * 200 + 50)}`,
          port: 80,
          service: '_arduino._tcp'
        }
      ]);
    }, 1200);
  };

  return (
    <div className="p-4 md:p-6 max-w-6xl mx-auto h-full overflow-y-auto space-y-6">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl md:text-2xl font-bold flex items-center gap-2.5 text-white">
            <Radio className="w-6 h-6 text-sky-400" />
            <span>Hardware Connections & RF Telemetry</span>
          </h2>
          <p className="text-xs md:text-sm text-slate-400 mt-1">
            Manage up to 3 concurrent hardware links (BLE, Classic BT, USB OTG, WiFi Sockets, MQTT) with real-time signal analysis.
          </p>
        </div>

        {/* Slot Selector Buttons */}
        <div className="flex items-center gap-2 bg-slate-900 border border-slate-800 p-1.5 rounded-xl self-start md:self-auto">
          {slots.map((s, idx) => (
            <button
              key={s.slotIndex}
              onClick={() => onSelectSlot(idx)}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                selectedSlot === idx
                  ? 'bg-slate-800 text-white shadow-md'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
              }`}
            >
              <div
                className="w-2.5 h-2.5 rounded-full"
                style={{
                  backgroundColor: s.colorHex,
                  boxShadow: s.isConnected ? `0 0 8px ${s.colorHex}` : 'none'
                }}
              />
              <span>Slot {idx + 1}</span>
              <span className="text-[10px] font-mono text-slate-400">
                ({s.linkType})
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* D3 Real-Time Signal Quality & Latency Graph */}
      <SignalQualityGraph
        samples={signalSamples}
        slots={slots}
        selectedSlot={selectedSlot}
        onSendPing={onSendPing}
        onSelectSlot={onSelectSlot}
      />

      {/* Hardware Transport Cards Section */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-bold uppercase tracking-wider text-slate-300 flex items-center gap-2">
            <Sliders className="w-4 h-4 text-sky-400" />
            <span>Hardware Transports for Slot {selectedSlot + 1} ({activeSlot.name})</span>
          </h3>
          <span className="text-xs text-slate-400 font-mono">
            Status: {activeSlot.isConnected ? (
              <span className="text-emerald-400 font-bold">CONNECTED ({activeSlot.linkType})</span>
            ) : (
              <span className="text-slate-500">STANDBY</span>
            )}
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* 1. Bluetooth Low Energy (BLE) */}
          <div className={`p-4 rounded-xl bg-slate-900 border transition-all ${
            activeSlot.linkType === 'BLE' && activeSlot.isConnected
              ? 'border-sky-500/50 shadow-lg shadow-sky-950/40'
              : 'border-slate-800 hover:border-slate-700'
          }`}>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-lg bg-sky-500/10 text-sky-400">
                  <Bluetooth className="w-4 h-4" />
                </div>
                <div>
                  <h4 className="font-bold text-sm text-sky-400">1. Bluetooth Low Energy (BLE)</h4>
                  <p className="text-[11px] text-slate-400">Auto-detects HM-10 & Nordic NUS</p>
                </div>
              </div>
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-sky-500/10 text-sky-400 font-mono font-bold">
                MTU 247
              </span>
            </div>

            <p className="text-xs text-slate-400 mb-3">
              GATT client with auto-negotiation for Service <span className="font-mono text-slate-300">0000FFE0</span> (HM-10) and <span className="font-mono text-slate-300">6E400001</span> (Nordic UART). Chunked writes to MTU-3 with 150ms buffer flush.
            </p>

            <div className="space-y-2 mb-3">
              <div className="flex items-center justify-between text-xs font-mono bg-slate-950 p-2.5 rounded-lg border border-slate-800">
                <span className="text-slate-400">Device MAC:</span>
                <span className="text-white font-bold">{activeSlot.address || '4C:11:AE:0A:88:F2'}</span>
              </div>
              <div className="flex items-center justify-between text-xs font-mono bg-slate-950 p-2.5 rounded-lg border border-slate-800">
                <span className="text-slate-400">Auto-Reconnect:</span>
                <span className="text-emerald-400 font-bold">Exponential Backoff (Max 5s)</span>
              </div>
            </div>

            <button
              onClick={() => {
                onUpdateSlotConfig(selectedSlot, { linkType: 'BLE' });
                onToggleConnect(selectedSlot);
              }}
              className={`w-full py-2.5 rounded-lg font-semibold text-xs transition-all shadow-md flex items-center justify-center gap-2 ${
                activeSlot.isConnected && activeSlot.linkType === 'BLE'
                  ? 'bg-rose-600 hover:bg-rose-500 text-white shadow-rose-600/30'
                  : 'bg-sky-600 hover:bg-sky-500 text-white shadow-sky-600/30'
              }`}
            >
              <Bluetooth className="w-4 h-4" />
              <span>
                {activeSlot.isConnected && activeSlot.linkType === 'BLE'
                  ? 'Disconnect BLE Link'
                  : 'Connect BLE Link'}
              </span>
            </button>
          </div>

          {/* 2. Classic Bluetooth SPP */}
          <div className={`p-4 rounded-xl bg-slate-900 border transition-all ${
            activeSlot.linkType === 'CLASSIC_BT' && activeSlot.isConnected
              ? 'border-emerald-500/50 shadow-lg shadow-emerald-950/40'
              : 'border-slate-800 hover:border-slate-700'
          }`}>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400">
                  <Radio className="w-4 h-4" />
                </div>
                <div>
                  <h4 className="font-bold text-sm text-emerald-400">2. Classic Bluetooth SPP</h4>
                  <p className="text-[11px] text-slate-400">HC-05 / HC-06 / ESP32 BT</p>
                </div>
              </div>
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 font-mono font-bold">
                RFCOMM
              </span>
            </div>

            <p className="text-xs text-slate-400 mb-3">
              Standard Serial Port Profile (UUID <span className="font-mono text-slate-300">00001101-0000-1000-8000-00805F9B34FB</span>). Reads bonded device list and initiates system pairing.
            </p>

            <div className="space-y-2 mb-3">
              <div className="flex items-center justify-between text-xs font-mono bg-slate-950 p-2.5 rounded-lg border border-slate-800">
                <span className="text-slate-400">Paired Address:</span>
                <span className="text-white font-bold">{slots[1].address || '98:D3:31:F4:22:01'}</span>
              </div>
              <div className="flex items-center justify-between text-xs font-mono bg-slate-950 p-2.5 rounded-lg border border-slate-800">
                <span className="text-slate-400">Socket Protocol:</span>
                <span className="text-slate-300">BluetoothSocket.getOutputStream()</span>
              </div>
            </div>

            <button
              onClick={() => {
                onUpdateSlotConfig(selectedSlot, { linkType: 'CLASSIC_BT' });
                onToggleConnect(selectedSlot);
              }}
              className={`w-full py-2.5 rounded-lg font-semibold text-xs transition-all shadow-md flex items-center justify-center gap-2 ${
                activeSlot.isConnected && activeSlot.linkType === 'CLASSIC_BT'
                  ? 'bg-rose-600 hover:bg-rose-500 text-white shadow-rose-600/30'
                  : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-emerald-600/30'
              }`}
            >
              <Radio className="w-4 h-4" />
              <span>
                {activeSlot.isConnected && activeSlot.linkType === 'CLASSIC_BT'
                  ? 'Disconnect SPP Link'
                  : 'Connect via Classic SPP'}
              </span>
            </button>
          </div>

          {/* 3. USB OTG Serial */}
          <div className={`p-4 rounded-xl bg-slate-900 border transition-all ${
            activeSlot.linkType === 'USB_SERIAL' && activeSlot.isConnected
              ? 'border-purple-500/50 shadow-lg shadow-purple-950/40'
              : 'border-slate-800 hover:border-slate-700'
          }`}>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-lg bg-purple-500/10 text-purple-400">
                  <Usb className="w-4 h-4" />
                </div>
                <div>
                  <h4 className="font-bold text-sm text-purple-400">3. USB OTG Serial</h4>
                  <p className="text-[11px] text-slate-400">usb-serial-for-android</p>
                </div>
              </div>
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-purple-500/10 text-purple-400 font-mono font-bold">
                Zero Latency
              </span>
            </div>

            <p className="text-xs text-slate-400 mb-3">
              Hardware USB Host support for CH340, CP2102, FTDI FT232, and Arduino CDC-ACM with DTR/RTS auto-reset triggers.
            </p>

            <div className="grid grid-cols-2 gap-2 mb-3">
              <div className="bg-slate-950 p-2 rounded-lg border border-slate-800">
                <label className="text-[10px] text-slate-400 block mb-1">Baud Rate</label>
                <select
                  value={baudRate}
                  onChange={(e) => setBaudRate(Number(e.target.value))}
                  className="w-full bg-slate-900 text-white text-xs rounded border border-slate-700 px-2 py-1 outline-none font-mono"
                >
                  <option value={9600}>9600</option>
                  <option value={57600}>57600</option>
                  <option value={115200}>115200 (Default)</option>
                  <option value={230400}>230400</option>
                  <option value={921600}>921600</option>
                </select>
              </div>

              <div className="bg-slate-950 p-2 rounded-lg border border-slate-800 flex flex-col justify-between">
                <label className="text-[10px] text-slate-400 block">DTR / RTS Reset</label>
                <button
                  onClick={() => setDtrRts(!dtrRts)}
                  className={`py-1 px-2 text-xs rounded font-semibold transition-all ${
                    dtrRts ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40' : 'bg-slate-800 text-slate-400'
                  }`}
                >
                  {dtrRts ? 'Enabled (Auto-Reset)' : 'Disabled'}
                </button>
              </div>
            </div>

            <button
              onClick={() => {
                onUpdateSlotConfig(selectedSlot, { linkType: 'USB_SERIAL' });
                onToggleConnect(selectedSlot);
              }}
              className={`w-full py-2.5 rounded-lg font-semibold text-xs transition-all shadow-md flex items-center justify-center gap-2 ${
                activeSlot.isConnected && activeSlot.linkType === 'USB_SERIAL'
                  ? 'bg-rose-600 hover:bg-rose-500 text-white shadow-rose-600/30'
                  : 'bg-purple-600 hover:bg-purple-500 text-white shadow-purple-600/30'
              }`}
            >
              <Usb className="w-4 h-4" />
              <span>
                {activeSlot.isConnected && activeSlot.linkType === 'USB_SERIAL'
                  ? 'Close USB Serial'
                  : 'Open USB Host Port'}
              </span>
            </button>
          </div>

          {/* 4. WiFi Sockets & MQTT */}
          <div className={`p-4 rounded-xl bg-slate-900 border transition-all ${
            (activeSlot.linkType === 'WEBSOCKET' || activeSlot.linkType === 'MQTT' || activeSlot.linkType === 'TCP') && activeSlot.isConnected
              ? 'border-amber-500/50 shadow-lg shadow-amber-950/40'
              : 'border-slate-800 hover:border-slate-700'
          }`}>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-lg bg-amber-500/10 text-amber-400">
                  <Wifi className="w-4 h-4" />
                </div>
                <div>
                  <h4 className="font-bold text-sm text-amber-400">4. WiFi Sockets & MQTT</h4>
                  <p className="text-[11px] text-slate-400">TCP, UDP, WebSocket & MQTT Broker</p>
                </div>
              </div>
              <button
                onClick={handleScanMdns}
                disabled={isScanningMdns}
                className="text-[10px] px-2.5 py-1 rounded bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 font-mono flex items-center gap-1 transition-all"
              >
                <RefreshCw className={`w-3 h-3 ${isScanningMdns ? 'animate-spin' : ''}`} />
                <span>mDNS Scan</span>
              </button>
            </div>

            <p className="text-xs text-slate-400 mb-3">
              Full OkHttp WebSocket client, raw TCP/UDP stream, HTTP robot-car URL endpoints, and Eclipse Paho MQTT with TLS & QoS support.
            </p>

            <div className="space-y-2 mb-3">
              <div className="flex items-center gap-2 text-xs font-mono bg-slate-950 p-2 rounded-lg border border-slate-800">
                <span className="text-slate-400">Endpoint:</span>
                <input
                  type="text"
                  defaultValue="ws://192.168.4.1/ws"
                  className="bg-transparent text-white flex-1 outline-none text-xs"
                />
              </div>

              {/* Discovered mDNS nodes */}
              {discoveredDevices.length > 0 && (
                <div className="text-[11px] text-slate-400 bg-slate-950/60 p-2 rounded border border-slate-800/80">
                  <div className="text-[10px] text-slate-500 uppercase tracking-wider mb-1 font-semibold">
                    Discovered Nodes (mDNS / NSD):
                  </div>
                  <div className="space-y-1">
                    {discoveredDevices.map((d, i) => (
                      <div key={i} className="flex items-center justify-between text-slate-300 font-mono">
                        <span className="text-amber-400">{d.name}</span>
                        <span>{d.ip}:{d.port}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="grid grid-cols-2 gap-2">
              <button
                onClick={() => {
                  onUpdateSlotConfig(selectedSlot, { linkType: 'WEBSOCKET' });
                  onToggleConnect(selectedSlot);
                }}
                className="py-2 bg-amber-600 hover:bg-amber-500 text-white rounded-lg font-semibold text-xs transition-all shadow-md shadow-amber-600/30"
              >
                Connect WebSocket
              </button>
              <button
                onClick={() => {
                  onUpdateSlotConfig(selectedSlot, { linkType: 'MQTT' });
                  onToggleConnect(selectedSlot);
                }}
                className="py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-lg font-semibold text-xs transition-all"
              >
                Connect MQTT
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* BLE WiFi Provisioner Card */}
      <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-2">
            <Cpu className="w-5 h-5 text-sky-400" />
            <h4 className="text-sm font-bold text-white">BLE WiFi Provisioning Utility</h4>
          </div>
          <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-sky-500/10 text-sky-400">
            ESP32 / ESP8266 SoftAP
          </span>
        </div>
        <p className="text-xs text-slate-400 mb-3">
          Wirelessly provision network credentials to a factory-fresh ESP board over Bluetooth Low Energy as JSON payload.
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label className="text-[10px] text-slate-400 block mb-1">Target SSID</label>
            <input
              type="text"
              value={wifiSsid}
              onChange={(e) => setWifiSsid(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-white outline-none font-mono"
            />
          </div>
          <div>
            <label className="text-[10px] text-slate-400 block mb-1">Target Password</label>
            <input
              type="password"
              value={wifiPass}
              onChange={(e) => setWifiPass(e.target.value)}
              placeholder="WPA2 passphrase"
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-white outline-none font-mono"
            />
          </div>
          <div className="flex items-end">
            <button
              onClick={() => {
                alert(`Provisioning payload sent to Slot ${selectedSlot + 1}: {"ssid":"${wifiSsid}","pass":"***"}`);
              }}
              className="w-full py-2 bg-sky-600 hover:bg-sky-500 text-white rounded-lg text-xs font-semibold shadow-md shadow-sky-600/30 transition-all"
            >
              Send Provision JSON
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
