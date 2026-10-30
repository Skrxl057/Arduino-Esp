import { DeviceSlotState, LogEntry } from '../types';

export class HardwareSimulator {
  private intervalId: any = null;
  private onDataCallback: ((slotIndex: number, text: string, rawBytes: Uint8Array) => void) | null = null;
  private simulatedState = {
    carState: 'S',
    speed: 5,
    temp: 24.5,
    humidity: 58,
    dist: 85,
    voltage: 3.82,
    headlights: false,
    horn: false,
    fan: false
  };

  setDataCallback(callback: (slotIndex: number, text: string, rawBytes: Uint8Array) => void) {
    this.onDataCallback = callback;
  }

  start(slotIndex: number = 0) {
    if (this.intervalId) return;

    this.intervalId = setInterval(() => {
      // Simulate random telemetry drift
      this.simulatedState.temp = +(this.simulatedState.temp + (Math.random() * 0.4 - 0.2)).toFixed(1);
      this.simulatedState.humidity = Math.max(30, Math.min(90, +(this.simulatedState.humidity + (Math.random() * 0.8 - 0.4)).toFixed(1)));
      this.simulatedState.dist = Math.max(5, Math.min(200, +(this.simulatedState.dist + (Math.random() * 6 - 3)).toFixed(0)));
      this.simulatedState.voltage = +(3.80 + Math.random() * 0.04).toFixed(2);

      // Format as key=value stream
      const telemetryLine = `temp=${this.simulatedState.temp},humidity=${this.simulatedState.humidity},dist=${this.simulatedState.dist},voltage=${this.simulatedState.voltage},alert=${this.simulatedState.dist < 25 ? 1 : 0}`;
      const bytes = new TextEncoder().encode(telemetryLine + '\r\n');

      if (this.onDataCallback) {
        this.onDataCallback(slotIndex, telemetryLine, bytes);
      }
    }, 1000);
  }

  stop() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  handleCommand(slotIndex: number, command: string): string | null {
    const trimmed = command.trim();

    if (trimmed === 'F' || trimmed === 'B' || trimmed === 'L' || trimmed === 'R' || trimmed === 'S') {
      this.simulatedState.carState = trimmed;
      const response = `[CAR] State=${trimmed} Speed=${this.simulatedState.speed}`;
      this.onDataCallback?.(slotIndex, response, new TextEncoder().encode(response + '\r\n'));
      return response;
    }

    if (trimmed.startsWith('V') && !isNaN(Number(trimmed.slice(1)))) {
      this.simulatedState.speed = Number(trimmed.slice(1));
      const response = `[CAR] Speed set to ${this.simulatedState.speed}`;
      this.onDataCallback?.(slotIndex, response, new TextEncoder().encode(response + '\r\n'));
      return response;
    }

    if (trimmed.startsWith('X:') && trimmed.includes('Y:')) {
      const response = `[DRIVE] Differential Vector: ${trimmed}`;
      this.onDataCallback?.(slotIndex, response, new TextEncoder().encode(response + '\r\n'));
      return response;
    }

    if (trimmed === 'PING') {
      const response = 'PONG';
      this.onDataCallback?.(slotIndex, response, new TextEncoder().encode(response + '\r\n'));
      return response;
    }

    if (trimmed === 'STATUS?') {
      const response = `STATUS: OK | Motors=${this.simulatedState.carState} | Speed=${this.simulatedState.speed} | VBat=${this.simulatedState.voltage}V`;
      this.onDataCallback?.(slotIndex, response, new TextEncoder().encode(response + '\r\n'));
      return response;
    }

    // Default echo
    const echo = `ACK: ${trimmed}`;
    this.onDataCallback?.(slotIndex, echo, new TextEncoder().encode(echo + '\r\n'));
    return echo;
  }
}

export const simulator = new HardwareSimulator();
