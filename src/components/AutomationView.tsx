import React, { useState } from 'react';
import { AutomationRuleWeb, DashboardWidgetWeb } from '../types';
import {
  Zap,
  Play,
  Plus,
  Trash2,
  FileJson,
  Upload,
  Check,
  Clock,
  CheckCircle,
  X,
  AlertCircle
} from 'lucide-react';
import {
  exportDashboardAndAutomation,
  openJsonFilePicker,
  parseConfigBackup
} from '../services/configBackup';

interface Props {
  selectedSlot: number;
  onSendCommand: (cmd: string) => void;
  rules?: AutomationRuleWeb[];
  onUpdateRules?: (rules: AutomationRuleWeb[]) => void;
  widgets?: DashboardWidgetWeb[];
  onUpdateWidgets?: (widgets: DashboardWidgetWeb[]) => void;
}

export const AutomationView: React.FC<Props> = ({
  selectedSlot,
  onSendCommand,
  rules: controlledRules,
  onUpdateRules,
  widgets = [],
  onUpdateWidgets
}) => {
  const [internalRules, setInternalRules] = useState<AutomationRuleWeb[]>([
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

  const rules = controlledRules || internalRules;
  const updateRules = (newRules: AutomationRuleWeb[]) => {
    if (onUpdateRules) onUpdateRules(newRules);
    else setInternalRules(newRules);
  };

  const [executionLogs, setExecutionLogs] = useState<
    { id: string; time: number; ruleName: string; action: string }[]
  >([
    {
      id: 'l1',
      time: Date.now() - 15000,
      ruleName: 'Proximity Obstacle Emergency Halt',
      action: 'Sent "S" to Slot 1'
    }
  ]);

  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [showPasteModal, setShowPasteModal] = useState(false);
  const [pastedJson, setPastedJson] = useState('');
  const [pasteError, setPasteError] = useState<string | null>(null);

  const showNotification = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const toggleRule = (id: string) => {
    updateRules(rules.map((r) => (r.id === id ? { ...r, enabled: !r.enabled } : r)));
  };

  const deleteRule = (id: string) => {
    updateRules(rules.filter((r) => r.id !== id));
  };

  const testRunRule = (rule: AutomationRuleWeb) => {
    if (rule.actionType === 'SEND_COMMAND') {
      onSendCommand(rule.actionTarget);
    }
    setExecutionLogs((prev) => [
      {
        id: Date.now().toString(),
        time: Date.now(),
        ruleName: rule.name,
        action: `Executed action: ${rule.actionTarget}`
      },
      ...prev.slice(0, 50)
    ]);
  };

  const handleExportJson = () => {
    exportDashboardAndAutomation(widgets, rules);
    showNotification('Exported automation scripts and dashboard layout to JSON!');
  };

  const handleImportJsonFile = () => {
    openJsonFilePicker(
      (importedWidgets, importedRules) => {
        let countR = 0;
        let countW = 0;
        if (importedRules && importedRules.length > 0) {
          updateRules(importedRules);
          countR = importedRules.length;
        }
        if (importedWidgets && importedWidgets.length > 0 && onUpdateWidgets) {
          onUpdateWidgets(importedWidgets);
          countW = importedWidgets.length;
        }
        showNotification(
          `Successfully restored ${countR} automation rules${countW > 0 ? ` and ${countW} dashboard widgets` : ''}!`
        );
      },
      (err) => {
        showNotification(`Import failed: ${err}`);
      }
    );
  };

  const handleApplyPastedJson = () => {
    setPasteError(null);
    const result = parseConfigBackup(pastedJson);
    if (result.error) {
      setPasteError(result.error);
      return;
    }

    let countR = 0;
    let countW = 0;
    if (result.rules && result.rules.length > 0) {
      updateRules(result.rules);
      countR = result.rules.length;
    }
    if (result.widgets && result.widgets.length > 0 && onUpdateWidgets) {
      onUpdateWidgets(result.widgets);
      countW = result.widgets.length;
    }

    setShowPasteModal(false);
    setPastedJson('');
    showNotification(
      `Restored ${countR} automation rules${countW > 0 ? ` and ${countW} dashboard widgets` : ''}!`
    );
  };

  return (
    <div className="flex flex-col h-full bg-slate-950 text-slate-100 p-4 overflow-y-auto relative">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed top-14 right-4 z-50 flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-600 text-white font-medium text-xs shadow-xl animate-fade-in">
          <Check className="w-4 h-4 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Top Header & Actions */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 mb-4">
        <div>
          <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
            <Zap className="w-5 h-5 text-amber-400" />
            <span>MacroDroid Automation Engine</span>
          </h2>
          <p className="text-xs text-slate-400">
            Real-time event triggers, thresholds, failsafes, and multi-step actuator actions
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Export JSON */}
          <button
            onClick={handleExportJson}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-sky-600/20 hover:bg-sky-600/30 text-sky-300 border border-sky-500/40 text-xs font-semibold transition-all active:scale-95 shadow-sm"
            title="Export automation rules and dashboard layout to JSON file"
          >
            <FileJson className="w-3.5 h-3.5" />
            <span>Export JSON</span>
          </button>

          {/* Import JSON */}
          <button
            onClick={handleImportJsonFile}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/40 text-xs font-semibold transition-all active:scale-95 shadow-sm"
            title="Import automation rules and dashboard layout from JSON file"
          >
            <Upload className="w-3.5 h-3.5" />
            <span>Import JSON</span>
          </button>

          <button
            onClick={() => setShowPasteModal(true)}
            className="text-[11px] text-slate-400 hover:text-slate-200 px-2 py-1 underline"
            title="Paste JSON text directly"
          >
            Paste JSON
          </button>

          {/* New Rule */}
          <button
            onClick={() => {
              const name = prompt('Enter rule name:', 'Custom Telemetry Rule');
              if (name) {
                updateRules([
                  ...rules,
                  {
                    id: Date.now().toString(),
                    name,
                    enabled: true,
                    key: 'temp',
                    operator: '>',
                    threshold: 40,
                    actionType: 'SEND_COMMAND',
                    actionTarget: 'ALARM:1'
                  }
                ]);
              }
            }}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold shadow-md shadow-sky-600/30 transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>New Rule</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Rules List (2 Cols) */}
        <div className="lg:col-span-2 space-y-3">
          {rules.length === 0 ? (
            <div className="p-8 text-center text-slate-500 bg-slate-900 border border-slate-800 rounded-xl">
              No automation rules configured. Add a new rule or click "Import JSON" to restore rules.
            </div>
          ) : (
            rules.map((rule) => (
              <div
                key={rule.id}
                className={`p-4 rounded-xl border transition-all ${
                  rule.enabled
                    ? 'bg-slate-900 border-slate-800'
                    : 'bg-slate-900/40 border-slate-800/60 opacity-60'
                }`}
              >
                <div className="flex items-center justify-between gap-2 mb-2">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-sm text-slate-200">{rule.name}</span>
                    <span
                      className={`text-[10px] uppercase font-bold px-2 py-0.5 rounded ${
                        rule.enabled
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                          : 'bg-slate-800 text-slate-500'
                      }`}
                    >
                      {rule.enabled ? 'Active' : 'Disabled'}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => testRunRule(rule)}
                      className="flex items-center gap-1 px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-sky-400 text-xs font-medium border border-slate-700 transition-all"
                    >
                      <Play className="w-3 h-3" />
                      <span>Test Run</span>
                    </button>

                    <button
                      onClick={() => toggleRule(rule.id)}
                      className={`px-3 py-1 rounded text-xs font-semibold ${
                        rule.enabled ? 'bg-amber-600/20 text-amber-300' : 'bg-slate-800 text-slate-400'
                      }`}
                    >
                      {rule.enabled ? 'Disable' : 'Enable'}
                    </button>

                    <button
                      onClick={() => deleteRule(rule.id)}
                      className="p-1 rounded text-slate-500 hover:text-rose-400 transition-all"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {/* Rule Logic Representation */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs font-mono mt-3">
                  <div className="p-2.5 rounded-lg bg-slate-950/80 border border-slate-800 flex items-center gap-2">
                    <span className="text-sky-400 font-bold shrink-0">TRIGGER:</span>
                    <span className="text-slate-300">
                      When <code>{rule.key}</code> {rule.operator} {rule.threshold}
                    </span>
                  </div>
                  <div className="p-2.5 rounded-lg bg-slate-950/80 border border-slate-800 flex items-center gap-2">
                    <span className="text-emerald-400 font-bold shrink-0">ACTION:</span>
                    <span className="text-slate-300 truncate">
                      {rule.actionType === 'SEND_COMMAND' ? `TX: "${rule.actionTarget}"` : rule.actionTarget}
                    </span>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Execution Log Sidebar (1 Col) */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col h-[500px]">
          <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-300 flex items-center gap-1.5">
              <Clock className="w-4 h-4 text-sky-400" />
              <span>Rule Execution History</span>
            </h3>
            <span className="text-[11px] text-slate-500">{executionLogs.length} events</span>
          </div>

          <div className="flex-1 overflow-y-auto space-y-2 text-xs font-mono">
            {executionLogs.map((log) => (
              <div key={log.id} className="p-2 rounded bg-slate-950 border border-slate-800/80">
                <div className="flex items-center justify-between text-[10px] text-slate-500 mb-1">
                  <span>{new Date(log.time).toLocaleTimeString()}</span>
                  <span className="text-emerald-400 flex items-center gap-1">
                    <CheckCircle className="w-3 h-3" /> Fired
                  </span>
                </div>
                <div className="font-semibold text-slate-200">{log.ruleName}</div>
                <div className="text-[11px] text-slate-400 mt-0.5">{log.action}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Paste JSON Modal */}
      {showPasteModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-5 shadow-2xl">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <FileJson className="w-5 h-5 text-sky-400" />
                <h3 className="font-bold text-sm text-slate-100">
                  Import Automation &amp; Dashboard JSON
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
              Paste exported configuration JSON below to restore automation scripts and dashboard layouts.
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
