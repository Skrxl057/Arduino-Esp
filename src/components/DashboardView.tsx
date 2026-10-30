import React, { useState } from 'react';
import { DashboardWidgetWeb, AutomationRuleWeb, TelemetryPoint } from '../types';
import {
  Download,
  Upload,
  Plus,
  Activity,
  FileJson,
  Check,
  AlertCircle,
  X
} from 'lucide-react';
import {
  exportDashboardAndAutomation,
  openJsonFilePicker,
  parseConfigBackup
} from '../services/configBackup';

interface Props {
  selectedSlot: number;
  telemetryHistory: Record<string, TelemetryPoint[]>;
  onSendCommand: (cmd: string) => void;
  widgets?: DashboardWidgetWeb[];
  onUpdateWidgets?: (widgets: DashboardWidgetWeb[]) => void;
  rules?: AutomationRuleWeb[];
  onUpdateRules?: (rules: AutomationRuleWeb[]) => void;
}

export const DashboardView: React.FC<Props> = ({
  selectedSlot,
  telemetryHistory,
  onSendCommand,
  widgets: controlledWidgets,
  onUpdateWidgets,
  rules = [],
  onUpdateRules
}) => {
  const [internalWidgets, setInternalWidgets] = useState<DashboardWidgetWeb[]>([
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

  const widgets = controlledWidgets || internalWidgets;
  const updateWidgets = (newWidgets: DashboardWidgetWeb[]) => {
    if (onUpdateWidgets) onUpdateWidgets(newWidgets);
    else setInternalWidgets(newWidgets);
  };

  const [switchStates, setSwitchStates] = useState<Record<string, boolean>>({});
  const [sliderStates, setSliderStates] = useState<Record<string, number>>({});
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [showPasteModal, setShowPasteModal] = useState(false);
  const [pastedJson, setPastedJson] = useState('');
  const [pasteError, setPasteError] = useState<string | null>(null);

  const showNotification = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  // Export current layout & automation rules
  const handleExportJson = () => {
    exportDashboardAndAutomation(widgets, rules);
    showNotification('Exported dashboard layout & automation scripts to JSON!');
  };

  // Import via file picker
  const handleImportJsonFile = () => {
    openJsonFilePicker(
      (importedWidgets, importedRules) => {
        let countW = 0;
        let countR = 0;

        if (importedWidgets && importedWidgets.length > 0) {
          updateWidgets(importedWidgets);
          countW = importedWidgets.length;
        }
        if (importedRules && importedRules.length > 0 && onUpdateRules) {
          onUpdateRules(importedRules);
          countR = importedRules.length;
        }

        showNotification(
          `Successfully restored ${countW} widgets${countR > 0 ? ` and ${countR} automation rules` : ''}!`
        );
      },
      (err) => {
        showNotification(`Import failed: ${err}`);
      }
    );
  };

  // Import via pasted text
  const handleApplyPastedJson = () => {
    setPasteError(null);
    const result = parseConfigBackup(pastedJson);
    if (result.error) {
      setPasteError(result.error);
      return;
    }

    let countW = 0;
    let countR = 0;
    if (result.widgets && result.widgets.length > 0) {
      updateWidgets(result.widgets);
      countW = result.widgets.length;
    }
    if (result.rules && result.rules.length > 0 && onUpdateRules) {
      onUpdateRules(result.rules);
      countR = result.rules.length;
    }

    setShowPasteModal(false);
    setPastedJson('');
    showNotification(
      `Restored ${countW} dashboard widgets${countR > 0 ? ` and ${countR} automation rules` : ''}!`
    );
  };

  const handleExportCsv = () => {
    let csvContent = 'timestamp_iso,stream_key,value\n';
    Object.entries(telemetryHistory).forEach(([key, points]) => {
      points.forEach((pt) => {
        csvContent += `${new Date(pt.time).toISOString()},${key},${pt.value}\n`;
      });
    });

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `EmbedHub-Telemetry-Slot${selectedSlot + 1}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="flex flex-col h-full bg-slate-950 text-slate-100 p-4 overflow-y-auto relative">
      {/* Toast Notification Banner */}
      {toastMessage && (
        <div className="fixed top-14 right-4 z-50 flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-600 text-white font-medium text-xs shadow-xl animate-fade-in">
          <Check className="w-4 h-4 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Top Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 mb-4">
        <div>
          <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
            <Activity className="w-5 h-5 text-sky-400" />
            <span>Telemetry Dashboard</span>
          </h2>
          <p className="text-xs text-slate-400">
            Real-time parser for <code>key=val</code>, <code>key:val</code>, CSV & JSON
          </p>
        </div>

        {/* Action Buttons: Export/Import JSON, CSV, Add Widget */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Export JSON Layout */}
          <button
            onClick={handleExportJson}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-sky-600/20 hover:bg-sky-600/30 text-sky-300 border border-sky-500/40 text-xs font-semibold transition-all active:scale-95 shadow-sm"
            title="Export dashboard layout and automation rules to JSON file"
          >
            <FileJson className="w-3.5 h-3.5" />
            <span>Export JSON</span>
          </button>

          {/* Import JSON File */}
          <button
            onClick={handleImportJsonFile}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/40 text-xs font-semibold transition-all active:scale-95 shadow-sm"
            title="Import and restore dashboard layout and automation rules from JSON file"
          >
            <Upload className="w-3.5 h-3.5" />
            <span>Import JSON</span>
          </button>

          {/* Paste JSON Modal Trigger */}
          <button
            onClick={() => setShowPasteModal(true)}
            className="text-[11px] text-slate-400 hover:text-slate-200 px-2 py-1 underline"
            title="Paste JSON text directly"
          >
            Paste JSON
          </button>

          {/* Export CSV */}
          <button
            onClick={handleExportCsv}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs font-semibold transition-all"
            title="Export live numerical stream data to CSV"
          >
            <Download className="w-3.5 h-3.5 text-emerald-400" />
            <span className="hidden sm:inline">CSV</span>
          </button>

          {/* Add Widget */}
          <button
            onClick={() => {
              const newKey = prompt('Enter telemetry data key (e.g. voltage, dist, rpm):', 'rpm');
              if (newKey) {
                updateWidgets([
                  ...widgets,
                  {
                    id: Date.now().toString(),
                    title: newKey.toUpperCase(),
                    type: 'VALUE_TILE',
                    dataKey: newKey,
                    min: 0,
                    max: 100,
                    unit: '',
                    colorHex: '#38BDF8',
                    lastValue: 0
                  }
                ]);
              }
            }}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold transition-all shadow-md shadow-sky-600/30"
          >
            <Plus className="w-4 h-4" />
            <span>Add Widget</span>
          </button>
        </div>
      </div>

      {/* Widgets Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {widgets.map((widget) => {
          const points = telemetryHistory[widget.dataKey] || [];
          const currentValue = points.length > 0 ? points[points.length - 1].value : widget.lastValue;

          return (
            <div
              key={widget.id}
              className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col justify-between shadow-sm relative overflow-hidden"
            >
              {/* Header */}
              <div className="flex items-center justify-between mb-3">
                <span className="font-semibold text-xs text-slate-200">{widget.title}</span>
                <div className="flex items-center gap-1.5">
                  <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
                    {widget.dataKey}
                  </span>
                  <button
                    onClick={() => {
                      updateWidgets(widgets.filter((w) => w.id !== widget.id));
                    }}
                    className="text-slate-600 hover:text-rose-400 text-xs px-1"
                    title="Remove widget"
                  >
                    ×
                  </button>
                </div>
              </div>

              {/* Widget Body based on Type */}
              <div className="flex-1 flex items-center justify-center py-2">
                {widget.type === 'GAUGE' && (
                  <div className="flex flex-col items-center">
                    <div className="relative w-36 h-36 flex items-center justify-center">
                      <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
                        <circle
                          cx="50"
                          cy="50"
                          r="40"
                          className="stroke-slate-800 fill-none"
                          strokeWidth="10"
                          strokeDasharray="188 63"
                          strokeLinecap="round"
                        />
                        {(() => {
                          const fraction = Math.min(
                            1,
                            Math.max(0, (currentValue - widget.min) / (widget.max - widget.min))
                          );
                          const strokeDashoffset = 188 - 188 * fraction;
                          return (
                            <circle
                              cx="50"
                              cy="50"
                              r="40"
                              className="fill-none transition-all duration-300"
                              stroke={widget.colorHex}
                              strokeWidth="10"
                              strokeDasharray="188 63"
                              strokeDashoffset={strokeDashoffset}
                              strokeLinecap="round"
                            />
                          );
                        })()}
                      </svg>
                      <div className="absolute inset-0 flex flex-col items-center justify-center">
                        <span className="text-2xl font-bold font-mono tracking-tight text-white">
                          {currentValue.toFixed(1)}
                        </span>
                        <span className="text-[11px] text-slate-400">{widget.unit}</span>
                      </div>
                    </div>
                  </div>
                )}

                {widget.type === 'LINE_GRAPH' && (
                  <div className="w-full h-28 flex flex-col justify-end">
                    {points.length < 2 ? (
                      <div className="text-xs text-slate-500 text-center my-auto">
                        Waiting for stream <code>{widget.dataKey}</code>...
                      </div>
                    ) : (
                      <div className="h-full flex items-end gap-1 px-1">
                        {points.slice(-25).map((pt, i) => {
                          const minV = Math.min(...points.map((p) => p.value));
                          const maxV = Math.max(...points.map((p) => p.value));
                          const range = maxV > minV ? maxV - minV : 1;
                          const heightPct = Math.max(
                            8,
                            Math.min(100, Math.round(((pt.value - minV) / range) * 100))
                          );

                          return (
                            <div
                              key={i}
                              className="flex-1 rounded-t transition-all duration-300"
                              style={{
                                height: `${heightPct}%`,
                                backgroundColor: widget.colorHex
                              }}
                              title={`${pt.value} ${widget.unit} at ${new Date(pt.time).toLocaleTimeString()}`}
                            />
                          );
                        })}
                      </div>
                    )}
                  </div>
                )}

                {widget.type === 'VALUE_TILE' && (
                  <div className="flex flex-col items-center my-auto">
                    <span
                      className="text-4xl font-extrabold font-mono tracking-tight"
                      style={{ color: widget.colorHex }}
                    >
                      {currentValue.toFixed(1)}
                    </span>
                    <span className="text-xs text-slate-400 mt-1">{widget.unit}</span>
                  </div>
                )}

                {widget.type === 'LED_INDICATOR' && (
                  <div className="flex flex-col items-center my-auto gap-2">
                    <div
                      className={`w-12 h-12 rounded-full flex items-center justify-center border-2 transition-all duration-300 ${
                        currentValue > 0.5
                          ? 'bg-rose-500 border-rose-300 shadow-lg shadow-rose-500/50 animate-pulse'
                          : 'bg-slate-800 border-slate-700'
                      }`}
                    >
                      <div className="w-4 h-4 rounded-full bg-white/60" />
                    </div>
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-300">
                      {currentValue > 0.5 ? 'ALERT HIGH' : 'NORMAL'}
                    </span>
                  </div>
                )}

                {widget.type === 'SWITCH' && (
                  <div className="flex flex-col items-center my-auto gap-3">
                    <button
                      onClick={() => {
                        const next = !switchStates[widget.id];
                        setSwitchStates((prev) => ({ ...prev, [widget.id]: next }));
                        onSendCommand(`RELAY:${next ? 1 : 0}`);
                      }}
                      className={`w-16 h-8 rounded-full transition-all relative p-1 ${
                        switchStates[widget.id] ? 'bg-amber-500 shadow-md shadow-amber-500/30' : 'bg-slate-800'
                      }`}
                    >
                      <div
                        className={`w-6 h-6 rounded-full bg-white transition-all transform ${
                          switchStates[widget.id] ? 'translate-x-8' : 'translate-x-0'
                        }`}
                      />
                    </button>
                    <span className="text-xs text-slate-400 font-mono">
                      State: {switchStates[widget.id] ? 'ENABLED' : 'DISABLED'}
                    </span>
                  </div>
                )}

                {widget.type === 'SLIDER' && (
                  <div className="flex flex-col items-center w-full px-2 my-auto gap-2">
                    <input
                      type="range"
                      min={widget.min}
                      max={widget.max}
                      value={sliderStates[widget.id] ?? widget.lastValue}
                      onChange={(e) => {
                        const val = Number(e.target.value);
                        setSliderStates((prev) => ({ ...prev, [widget.id]: val }));
                        onSendCommand(`SPEED:${val}`);
                      }}
                      className="w-full accent-purple-500 h-2 bg-slate-800 rounded-lg cursor-pointer"
                    />
                    <span className="text-xs font-mono font-bold text-purple-400">
                      {(sliderStates[widget.id] ?? widget.lastValue).toFixed(0)} {widget.unit}
                    </span>
                  </div>
                )}
              </div>

              {/* Bottom Footer Details */}
              <div className="flex items-center justify-between pt-2 border-t border-slate-800/80 text-[10px] text-slate-500">
                <span>
                  Range: {widget.min} - {widget.max} {widget.unit}
                </span>
                <span>{points.length} samples</span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Paste JSON Import Modal */}
      {showPasteModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-5 shadow-2xl">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <FileJson className="w-5 h-5 text-sky-400" />
                <h3 className="font-bold text-sm text-slate-100">
                  Import Dashboard &amp; Automation JSON
                </h3>
              </div>
              <button
                onClick={() => {
                  setShowPasteModal(false);
                  setPasteError(null);
                }}
                className="text-slate-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-slate-400 mb-3">
              Paste the JSON configuration exported from EmbedHub to restore all dashboard widgets and automation scripts.
            </p>

            <textarea
              value={pastedJson}
              onChange={(e) => setPastedJson(e.target.value)}
              placeholder="Paste JSON content here..."
              rows={8}
              className="w-full p-2.5 bg-slate-950 border border-slate-700 rounded-xl font-mono text-xs text-slate-200 placeholder-slate-600 focus:outline-none focus:border-sky-500 mb-3"
            />

            {pasteError && (
              <div className="flex items-center gap-2 p-2 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs mb-3">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{pasteError}</span>
              </div>
            )}

            <div className="flex justify-end gap-2">
              <button
                onClick={() => {
                  setShowPasteModal(false);
                  setPasteError(null);
                }}
                className="px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-400 hover:text-slate-200"
              >
                Cancel
              </button>
              <button
                onClick={handleApplyPastedJson}
                disabled={!pastedJson.trim()}
                className="px-4 py-1.5 rounded-lg bg-sky-600 hover:bg-sky-500 disabled:bg-slate-800 text-white font-semibold text-xs shadow-md shadow-sky-600/30 transition-all"
              >
                Restore Configuration
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
