export type LinkType = 'BLE' | 'CLASSIC_BT' | 'USB_SERIAL' | 'TCP' | 'UDP' | 'WEBSOCKET' | 'HTTP' | 'MQTT';

export type LineEnding = 'NONE' | 'LF' | 'CR' | 'CRLF';

export interface DeviceSlotState {
  slotIndex: number;
  name: string;
  colorHex: string;
  linkType: LinkType;
  address: string;
  isConnected: boolean;
  isConnecting: boolean;
  txBytes: number;
  rxBytes: number;
  lastLatencyMs: number;
  errorCount: number;
  rssi: number; // RSSI in dBm (-30 to -100)
}

export interface SignalQualitySample {
  timestamp: number;
  rssi: number; // dBm (-100 to -30)
  latencyMs: number; // ms
  slotIndex: number;
}


export interface LogEntry {
  id: string;
  timestamp: number;
  isTx: boolean;
  text: string;
  hex: string;
  slotIndex: number;
}

export interface Macro {
  id: string;
  label: string;
  command: string;
  lineEnding: LineEnding;
}

export interface TelemetryPoint {
  time: number;
  value: number;
}

export interface AutomationRuleWeb {
  id: string;
  name: string;
  enabled: boolean;
  key: string;
  operator: '>' | '<' | '==' | '!=';
  threshold: number;
  actionType: 'SEND_COMMAND' | 'NOTIFY' | 'VIBRATE';
  actionTarget: string;
  lastTriggered?: number;
}

export interface DashboardWidgetWeb {
  id: string;
  title: string;
  type: 'GAUGE' | 'LINE_GRAPH' | 'VALUE_TILE' | 'LED_INDICATOR' | 'SWITCH' | 'SLIDER';
  dataKey: string;
  min: number;
  max: number;
  unit: string;
  colorHex: string;
  lastValue: number;
}
