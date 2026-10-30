import React, { useState, useEffect } from 'react';
import { DeviceSlotState, LogEntry, LineEnding, TelemetryPoint, DashboardWidgetWeb, AutomationRuleWeb, SignalQualitySample } from './types';
import { MultiDeviceHeader } from './components/MultiDeviceHeader';
import { ConnectionsView } from './components/ConnectionsView';
import { TerminalView } from './components/TerminalView';
import { ControlView } from './components/ControlView';
import { DashboardView } from './components/DashboardView';
import { AutomationView } from './components/AutomationView';
import { SketchesView } from './components/SketchesView';
import { AndroidProjectExplorer } from './components/AndroidProjectExplorer';
import { AndroidAppHubView } from './components/AndroidAppHubView';
import { simulator } from './services/simulator';
import {
  Bluetooth,
  Terminal as TerminalIcon,
  Gamepad2,
  LayoutDashboard,
  Zap,
  Code,
  Radio,
  Wifi,
  Sparkles,
  Smartphone
} from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<
    'connections' | 'terminal' | 'control' | 'dashboard' | 'automation' | 'sketches' | 'android'
  >('control');

  const [selectedSlot, setSelectedSlot] = useState(0);
  const [showAndroidExplorer, setShowAndroidExplorer] = useState(false);
  const [isLandscape, setIsLandscape] = useState(false);

  // Global Dashboard Widgets state
  const [widgets, setWidgets] = useState<DashboardWidgetWeb[]>([
    {
      id: 'w1',
      title: 'Temperature',
      type: 'GAUGE',
      dataKey: 'temp',
      min: 0,
      max: 60,
      unit: '°C',
      colorHex: '#0EA5E9',
      lastValue: 24.5
    },
    {
      id: 'w2',
      title: 'Live Telemetry Trend',
      type: 'LINE_GRAPH',
      dataKey: 'temp',
      min: 15,
      max: 45,
      unit: '°C',
      colorHex: '#10B981',
      lastValue: 24.5
    },
    {
      id: 'w3',
      title: 'Relative Humidity',
      type: 'VALUE_TILE',
      dataKey: 'humidity',
      min: 0,
      max: 100,
      unit: '%',
      colorHex: '#38BDF8',
      lastValue: 58
    },
    {
      id: 'w4',
      title: 'Obstacle Warning',
      type: 'LED_INDICATOR',
      dataKey: 'alert',
      min: 0,
      max: 1,
      unit: '',
      colorHex: '#EF4444',
      lastValue: 0
    },
    {
      id: 'w5',
      title: 'Cooling Fan Relay',
      type: 'SWITCH',
      dataKey: 'fan_sw',
      min: 0,
      max: 1,
      unit: '',
      colorHex: '#F59E0B',
      lastValue: 0
    },
    {
      id: 'w6',
      title: 'Throttle Trim',
      type: 'SLIDER',
      dataKey: 'throttle',
      min: 0,
      max: 100,
      unit: '%',
      colorHex: '#A855F7',
      lastValue: 80
    }
  ]);

  // Global Automation Rules state
  const [rules, setRules] = useState<AutomationRuleWeb[]>([
    {
      id: 'r1',
      name: 'Overheat Safety Fan Interlock',
      enabled: true,
      key: 'temp',
      operator: '>',
      threshold: 30.0,
      actionType: 'SEND_COMMAND',
      actionTarget: 'RELAY:FAN:ON'
    },
    {
      id: 'r2',
      name: 'Proximity Obstacle Emergency Halt',
      enabled: true,
      key: 'dist',
      operator: '<',
      threshold: 25.0,
      actionType: 'SEND_COMMAND',
      actionTarget: 'S'
    },
    {
      id: 'r3',
      name: 'Low Battery Voltage Warning',
      enabled: true,
      key: 'voltage',
      operator: '<',
      threshold: 3.4,
      actionType: 'NOTIFY',
      actionTarget: 'LiPo battery depleted under 3.4V. Return to charger!'
    }
  ]);

  // 3 Concurrent Device Slots
  const [slots, setSlots] = useState<DeviceSlotState[]>([
    {
      slotIndex: 0,
      name: 'ESP32 Robot Car',
      colorHex: '#0284C7',
      linkType: 'BLE',
      address: '4C:11:AE:0A:88:F2',
      isConnected: true,
      isConnecting: false,
      txBytes: 1048,
      rxBytes: 4210,
      lastLatencyMs: 14,
      errorCount: 0,
      rssi: -58
    },
    {
      slotIndex: 1,
      name: 'Arduino Uno Telemetry',
      colorHex: '#10B981',
      linkType: 'CLASSIC_BT',
      address: '98:D3:31:F4:22:01',
      isConnected: false,
      isConnecting: false,
      txBytes: 0,
      rxBytes: 0,
      lastLatencyMs: 24,
      errorCount: 0,
      rssi: -72
    },
    {
      slotIndex: 2,
      name: 'ESP8266 Weather Node',
      colorHex: '#F59E0B',
      linkType: 'WEBSOCKET',
      address: 'ws://192.168.4.1/ws',
      isConnected: false,
      isConnecting: false,
      txBytes: 0,
      rxBytes: 0,
      lastLatencyMs: 32,
      errorCount: 0,
      rssi: -65
    }
  ]);

  // Real-time Signal Quality & Latency Samples History for D3
  const [signalSamples, setSignalSamples] = useState<SignalQualitySample[]>(() => {
    const pts: SignalQualitySample[] = [];
    const now = Date.now();
    for (let i = 60; i >= 0; i--) {
      const t = now - i * 1000;
      // Slot 0 (connected ESP32 Robot Car)
      const baseRssi0 = -58 + Math.sin(i / 6) * 3.5 + (Math.random() * 1.5 - 0.75);
      const baseLat0 = Math.max(9, Math.round(14 + Math.sin(i / 4) * 3 + (Math.random() * 4 - 2)));
      pts.push({
        timestamp: t,
        rssi: +baseRssi0.toFixed(1),
        latencyMs: baseLat0,
        slotIndex: 0
      });

      // Slot 1 (paired Arduino Uno)
      if (i % 2 === 0) {
        const baseRssi1 = -72 + Math.cos(i / 5) * 4;
        pts.push({
          timestamp: t,
          rssi: +baseRssi1.toFixed(1),
          latencyMs: 24,
          slotIndex: 1
        });
      }
    }
    return pts;
  });

  // Terminal Logs
  const [logs, setLogs] = useState<LogEntry[]>([
    {
      id: 'init_1',
      timestamp: Date.now() - 5000,
      isTx: false,
      text: 'EmbedHub Core Engine initialized. Android 14 Foreground Service online.',
      hex: '45 6D 62 65 64 48 75 62',
      slotIndex: 0
    },
    {
      id: 'init_2',
      timestamp: Date.now() - 3000,
      isTx: false,
      text: '[BLE] Connected to ESP32 Robot Car (MTU negotiated: 247 bytes).',
      hex: '42 4C 45 20 4F 4B',
      slotIndex: 0
    }
  ]);

  // Telemetry History for Dashboard
  const [telemetryHistory, setTelemetryHistory] = useState<Record<string, TelemetryPoint[]>>({
    temp: [
      { time: Date.now() - 4000, value: 24.2 },
      { time: Date.now() - 3000, value: 24.4 },
      { time: Date.now() - 2000, value: 24.6 },
      { time: Date.now() - 1000, value: 24.5 }
    ],
    humidity: [
      { time: Date.now() - 3000, value: 57.8 },
      { time: Date.now() - 1000, value: 58.0 }
    ],
    dist: [
      { time: Date.now() - 3000, value: 92 },
      { time: Date.now() - 1000, value: 85 }
    ]
  });

  // Real-time RF signal strength and latency ticker
  useEffect(() => {
    const signalInterval = setInterval(() => {
      const now = Date.now();
      const newSamples: SignalQualitySample[] = [];

      setSlots((prevSlots) =>
        prevSlots.map((slot) => {
          if (!slot.isConnected) return slot;

          // Realistic RF noise & slight drift
          const deltaRssi = (Math.random() * 2 - 1) * 0.8;
          const nextRssi = +Math.max(-95, Math.min(-35, (slot.rssi || -58) + deltaRssi)).toFixed(1);

          // Realistic round-trip latency with occasional micro-jitter
          const jitter = Math.random() > 0.85 ? Math.floor(Math.random() * 12 + 6) : Math.floor(Math.random() * 4 - 2);
          const baseLat = slot.slotIndex === 0 ? 14 : slot.slotIndex === 1 ? 22 : 30;
          const nextLat = Math.max(6, baseLat + jitter);

          newSamples.push({
            timestamp: now,
            rssi: nextRssi,
            latencyMs: nextLat,
            slotIndex: slot.slotIndex
          });

          return {
            ...slot,
            rssi: nextRssi,
            lastLatencyMs: nextLat
          };
        })
      );

      if (newSamples.length > 0) {
        setSignalSamples((prev) => [...prev.slice(-180), ...newSamples]);
      }
    }, 1000);

    return () => clearInterval(signalInterval);
  }, []);

  // Simulator hookup
  useEffect(() => {
    simulator.setDataCallback((slotIdx, text, rawBytes) => {
      // Append RX log
      const hexStr = Array.from(rawBytes)
        .map((b) => b.toString(16).padStart(2, '0').toUpperCase())
        .join(' ');

      setLogs((prev) => [
        ...prev.slice(-300),
        {
          id: `${Date.now()}_${Math.random()}`,
          timestamp: Date.now(),
          isTx: false,
          text,
          hex: hexStr,
          slotIndex: slotIdx
        }
      ]);

      // Update RX bytes counter
      setSlots((prev) =>
        prev.map((s, idx) =>
          idx === slotIdx ? { ...s, rxBytes: s.rxBytes + rawBytes.length } : s
        )
      );

      // Parse telemetry key=value
      if (text.includes('=')) {
        const tokens = text.split(',');
        tokens.forEach((t) => {
          const [k, v] = t.split('=');
          if (k && v && !isNaN(Number(v))) {
            const numVal = Number(v);
            setTelemetryHistory((hist) => {
              const pts = hist[k] || [];
              return {
                ...hist,
                [k]: [...pts.slice(-50), { time: Date.now(), value: numVal }]
              };
            });
          }
        });
      }
    });

    simulator.start(0);

    return () => simulator.stop();
  }, []);


  // Send Command Handler
  const handleSendCommand = (command: string, lineEnding: LineEnding = 'CRLF') => {
    const rawBytes = new TextEncoder().encode(command + '\r\n');
    const hexStr = Array.from(rawBytes)
      .map((b) => b.toString(16).padStart(2, '0').toUpperCase())
      .join(' ');

    // Add TX log
    setLogs((prev) => [
      ...prev.slice(-300),
      {
        id: `${Date.now()}_${Math.random()}`,
        timestamp: Date.now(),
        isTx: true,
        text: command,
        hex: hexStr,
        slotIndex: selectedSlot
      }
    ]);

    // Update TX bytes
    setSlots((prev) =>
      prev.map((s, idx) =>
        idx === selectedSlot ? { ...s, txBytes: s.txBytes + rawBytes.length } : s
      )
    );

    // Feed to simulator or hardware
    simulator.handleCommand(selectedSlot, command);
  };

  // Toggle connection state on slot
  const handleToggleConnect = (slotIndex: number) => {
    setSlots((prev) =>
      prev.map((s, idx) => {
        if (idx === slotIndex) {
          const nextState = !s.isConnected;
          if (nextState && idx === 0) simulator.start(idx);
          if (!nextState && idx === 0) simulator.stop();
          return { ...s, isConnected: nextState };
        }
        return s;
      })
    );
  };

  // Handle test ping measurement
  const handleSendPing = (slotIndex: number) => {
    const t0 = performance.now();
    handleSendCommand('PING');
    setTimeout(() => {
      const rtt = Math.max(8, Math.round(performance.now() - t0 + Math.random() * 4));
      const currentRssi = slots[slotIndex]?.rssi ?? -58;
      setSlots((prev) =>
        prev.map((s, idx) => (idx === slotIndex ? { ...s, lastLatencyMs: rtt } : s))
      );
      setSignalSamples((prev) => [
        ...prev.slice(-180),
        {
          timestamp: Date.now(),
          rssi: currentRssi,
          latencyMs: rtt,
          slotIndex
        }
      ]);
    }, 14);
  };

  // Handle slot config update
  const handleUpdateSlotConfig = (slotIndex: number, config: Partial<DeviceSlotState>) => {
    setSlots((prev) =>
      prev.map((s, idx) => (idx === slotIndex ? { ...s, ...config } : s))
    );
  };

  // Emergency Stop Handler
  const handleEmergencyStopAll = () => {
    handleSendCommand('S');
  };

  return (
    <div className="flex flex-col h-screen w-screen bg-slate-950 text-slate-100 overflow-hidden font-sans">
      {/* Top Header with 3 Slots */}
      <MultiDeviceHeader
        slots={slots}
        selectedSlot={selectedSlot}
        onSelectSlot={(idx) => setSelectedSlot(idx)}
        onToggleConnect={handleToggleConnect}
        onEmergencyStop={handleEmergencyStopAll}
        onOpenAndroidExplorer={() => setShowAndroidExplorer(true)}
        onSelectAndroidTab={() => setActiveTab('android')}
        isLandscape={isLandscape}
        onToggleOrientation={() => setIsLandscape((prev) => !prev)}
      />

      {/* Main View Area */}
      <main className="flex-1 overflow-hidden relative">
        {activeTab === 'connections' && (
          <ConnectionsView
            slots={slots}
            selectedSlot={selectedSlot}
            onSelectSlot={(idx) => setSelectedSlot(idx)}
            onToggleConnect={handleToggleConnect}
            signalSamples={signalSamples}
            onSendPing={handleSendPing}
            onUpdateSlotConfig={handleUpdateSlotConfig}
          />
        )}

        {activeTab === 'terminal' && (
          <TerminalView
            logs={logs}
            selectedSlot={selectedSlot}
            onSendCommand={handleSendCommand}
            onClearLogs={() => setLogs([])}
          />
        )}

        {activeTab === 'control' && (
          <ControlView
            selectedSlot={selectedSlot}
            onSendCommand={handleSendCommand}
            isLandscape={isLandscape}
            onToggleOrientation={() => setIsLandscape((prev) => !prev)}
          />
        )}

        {activeTab === 'dashboard' && (
          <DashboardView
            selectedSlot={selectedSlot}
            telemetryHistory={telemetryHistory}
            onSendCommand={handleSendCommand}
            widgets={widgets}
            onUpdateWidgets={setWidgets}
            rules={rules}
            onUpdateRules={setRules}
          />
        )}

        {activeTab === 'automation' && (
          <AutomationView
            selectedSlot={selectedSlot}
            onSendCommand={handleSendCommand}
            rules={rules}
            onUpdateRules={setRules}
            widgets={widgets}
            onUpdateWidgets={setWidgets}
          />
        )}

        {activeTab === 'sketches' && <SketchesView />}

        {activeTab === 'android' && (
          <AndroidAppHubView
            onOpenAndroidExplorer={() => setShowAndroidExplorer(true)}
            isLandscape={isLandscape}
            onToggleOrientation={() => setIsLandscape((prev) => !prev)}
          />
        )}
      </main>

      {/* Bottom Navigation Bar */}
      <nav className="bg-slate-900 border-t border-slate-800 px-2 py-1.5 flex items-center justify-around text-xs">
        {[
          { id: 'connections', label: 'Connections', icon: Bluetooth },
          { id: 'terminal', label: 'Terminal', icon: TerminalIcon },
          { id: 'control', label: 'Control', icon: Gamepad2 },
          { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
          { id: 'automation', label: 'Automation', icon: Zap },
          { id: 'sketches', label: 'Sketches', icon: Code },
          { id: 'android', label: 'Android App', icon: Smartphone, highlight: true }
        ].map((tab) => {
          const IconComponent = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`flex flex-col items-center gap-1 py-1 px-2.5 rounded-lg transition-all ${
                isActive
                  ? tab.highlight
                    ? 'text-emerald-400 font-bold bg-emerald-500/15 border border-emerald-500/30'
                    : 'text-sky-400 font-bold bg-sky-500/10'
                  : tab.highlight
                  ? 'text-emerald-400/80 hover:text-emerald-300'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <IconComponent className="w-5 h-5" />
              <span className="text-[10px] tracking-tight">{tab.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Android Project Explorer Modal */}
      {showAndroidExplorer && (
        <AndroidProjectExplorer onClose={() => setShowAndroidExplorer(false)} />
      )}
    </div>
  );
}
