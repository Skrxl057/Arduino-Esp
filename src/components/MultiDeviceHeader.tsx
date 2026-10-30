import React from 'react';
import { DeviceSlotState } from '../types';
import { Wifi, Bluetooth, Usb, Radio, AlertOctagon, Download, Code, RotateCw, Smartphone } from 'lucide-react';
import { downloadAndroidProjectZip } from '../services/androidProjectZip';
import { usePWAInstall } from '../services/usePWAInstall';

interface Props {
  slots: DeviceSlotState[];
  selectedSlot: number;
  onSelectSlot: (index: number) => void;
  onToggleConnect: (index: number) => void;
  onEmergencyStop: () => void;
  onOpenAndroidExplorer: () => void;
  onSelectAndroidTab?: () => void;
  isLandscape?: boolean;
  onToggleOrientation?: () => void;
}

export const MultiDeviceHeader: React.FC<Props> = ({
  slots,
  selectedSlot,
  onSelectSlot,
  onToggleConnect,
  onEmergencyStop,
  onOpenAndroidExplorer,
  onSelectAndroidTab,
  isLandscape = false,
  onToggleOrientation
}) => {
  const { isInstallable, install } = usePWAInstall();

  return (
    <header className="bg-slate-900 border-b border-slate-800 px-4 py-2 text-white">
      <div className="flex flex-col md:flex-row items-center justify-between gap-3">
        {/* App Title & Tagline */}
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-sky-600 flex items-center justify-center font-black tracking-wider text-lg shadow-md shadow-sky-600/30">
            EH
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-bold text-base tracking-tight text-slate-100">EmbedHub</h1>
              <span className="text-[10px] uppercase font-semibold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center gap-1">
                <Smartphone className="w-3 h-3 text-emerald-400" />
                <span>Android App Ready</span>
              </span>
            </div>
            <p className="text-xs text-slate-400 hidden sm:block">
              Universal Arduino & ESP32 Controller • BLE, Classic BT, USB, WiFi, MQTT
            </p>
          </div>
        </div>

        {/* 3 Concurrent Device Slots */}
        <div className="flex items-center gap-2 w-full md:w-auto overflow-x-auto py-1">
          {slots.map((slot, index) => {
            const isSelected = selectedSlot === index;
            return (
              <div
                key={slot.slotIndex}
                onClick={() => onSelectSlot(index)}
                className={`cursor-pointer px-3 py-1.5 rounded-lg border transition-all flex items-center gap-2 text-xs min-w-[150px] ${
                  isSelected
                    ? 'bg-slate-800 border-sky-500 shadow-sm shadow-sky-500/20'
                    : 'bg-slate-900/60 border-slate-800 hover:border-slate-700'
                }`}
              >
                <div
                  className="w-2.5 h-2.5 rounded-full shrink-0"
                  style={{
                    backgroundColor: slot.isConnected ? '#10B981' : slot.isConnecting ? '#F59E0B' : '#64748B'
                  }}
                />
                <div className="flex-1 min-w-0">
                  <div className="font-semibold truncate text-slate-200">
                    Slot {index + 1}: {slot.name}
                  </div>
                  <div className="text-[10px] text-slate-400 flex items-center justify-between">
                    <span>{slot.linkType}</span>
                    <span className={slot.isConnected ? 'text-emerald-400' : 'text-slate-500'}>
                      {slot.isConnected ? 'Connected' : 'Offline'}
                    </span>
                  </div>
                </div>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onToggleConnect(index);
                  }}
                  className={`text-[10px] px-2 py-0.5 rounded font-medium border ${
                    slot.isConnected
                      ? 'bg-rose-500/10 text-rose-400 border-rose-500/30 hover:bg-rose-500/20'
                      : 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30 hover:bg-emerald-500/20'
                  }`}
                >
                  {slot.isConnected ? 'Drop' : 'Link'}
                </button>
              </div>
            );
          })}
        </div>

        {/* Emergency Stop & Android Source Hub */}
        <div className="flex items-center gap-2">
          {onToggleOrientation && (
            <button
              onClick={onToggleOrientation}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg border font-semibold text-xs transition-all active:scale-95 ${
                isLandscape
                  ? 'bg-sky-500/20 text-sky-300 border-sky-500/40 shadow-sm shadow-sky-500/30'
                  : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'
              }`}
              title="Toggle Screen Orientation (Portrait ⇄ Landscape Cockpit)"
            >
              <RotateCw className="w-3.5 h-3.5 text-sky-400" />
              <span className="hidden sm:inline">{isLandscape ? 'Landscape' : 'Portrait'}</span>
            </button>
          )}

          {isInstallable && (
            <button
              onClick={install}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs shadow-md shadow-emerald-600/30 active:scale-95 transition-all"
              title="Install EmbedHub natively on Android device as WebAPK"
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>Install APK</span>
            </button>
          )}

          <button
            onClick={onEmergencyStop}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs shadow-md shadow-rose-600/30 active:scale-95 transition-all"
            title="Sends Emergency STOP (S) to all 3 slots immediately"
          >
            <AlertOctagon className="w-4 h-4" />
            <span>STOP ALL</span>
          </button>

          {onSelectAndroidTab && (
            <button
              onClick={onSelectAndroidTab}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-700/80 hover:bg-emerald-600 text-white font-semibold text-xs shadow-md transition-all border border-emerald-500/40"
              title="Open Android Apps Hub"
            >
              <Smartphone className="w-3.5 h-3.5 text-emerald-300" />
              <span className="hidden sm:inline">Android App</span>
            </button>
          )}

          <button
            onClick={onOpenAndroidExplorer}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-sky-600 hover:bg-sky-500 text-white font-semibold text-xs shadow-md shadow-sky-600/30 transition-all"
            title="Inspect complete Kotlin & Jetpack Compose Android repository files"
          >
            <Code className="w-4 h-4" />
            <span className="hidden sm:inline">Source</span>
          </button>

          <button
            onClick={() => downloadAndroidProjectZip()}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 font-semibold text-xs transition-all"
            title="Download complete compilable Android project .ZIP"
          >
            <Download className="w-4 h-4 text-emerald-400" />
            <span className="hidden md:inline">ZIP</span>
          </button>
        </div>
      </div>
    </header>
  );
};
