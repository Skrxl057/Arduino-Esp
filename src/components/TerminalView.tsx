import React, { useState, useRef, useEffect } from 'react';
import { LogEntry, LineEnding, Macro } from '../types';
import { Send, Trash2, Download, Search, Check, Play } from 'lucide-react';

interface Props {
  logs: LogEntry[];
  selectedSlot: number;
  onSendCommand: (command: string, ending: LineEnding) => void;
  onClearLogs: () => void;
}

export const TerminalView: React.FC<Props> = ({
  logs,
  selectedSlot,
  onSendCommand,
  onClearLogs
}) => {
  const [inputText, setInputText] = useState('');
  const [lineEnding, setLineEnding] = useState<LineEnding>('CRLF');
  const [displayMode, setDisplayMode] = useState<'TEXT' | 'HEX' | 'BOTH'>('TEXT');
  const [autoScroll, setAutoScroll] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const logContainerRef = useRef<HTMLDivElement>(null);

  const [macros, setMacros] = useState<Macro[]>([
    { id: '1', label: 'Ping', command: 'PING', lineEnding: 'CRLF' },
    { id: '2', label: 'Status', command: 'STATUS?', lineEnding: 'CRLF' },
    { id: '3', label: 'Stop Motors', command: 'S', lineEnding: 'CRLF' },
    { id: '4', label: 'Forward 50%', command: 'F', lineEnding: 'CRLF' },
    { id: '5', label: 'Speed Max (V9)', command: 'V9', lineEnding: 'CRLF' },
    { id: '6', label: 'Relay On', command: 'RELAY:1', lineEnding: 'CRLF' }
  ]);

  const [newMacroLabel, setNewMacroLabel] = useState('');
  const [newMacroCmd, setNewMacroCmd] = useState('');

  // Auto-scroll
  useEffect(() => {
    if (autoScroll && logContainerRef.current) {
      logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
    }
  }, [logs, autoScroll]);

  const filteredLogs = logs.filter(
    (l) =>
      l.slotIndex === selectedSlot &&
      (searchQuery === '' ||
        l.text.toLowerCase().includes(searchQuery.toLowerCase()) ||
        l.hex.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const handleSend = () => {
    if (!inputText.trim()) return;
    onSendCommand(inputText, lineEnding);
    setInputText('');
  };

  const handleExportTxt = () => {
    const textData = filteredLogs
      .map(
        (l) =>
          `[${new Date(l.timestamp).toLocaleTimeString()}] ${l.isTx ? 'TX ▶' : 'RX ◀'} ${l.text}`
      )
      .join('\n');
    const blob = new Blob([textData], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `EmbedHub-Slot${selectedSlot + 1}-Log.txt`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const addMacro = () => {
    if (!newMacroLabel || !newMacroCmd) return;
    setMacros((prev) => [
      ...prev,
      { id: Date.now().toString(), label: newMacroLabel, command: newMacroCmd, lineEnding: 'CRLF' }
    ]);
    setNewMacroLabel('');
    setNewMacroCmd('');
  };

  return (
    <div className="flex flex-col h-full bg-slate-950 text-slate-200">
      {/* Top Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-2 bg-slate-900 border-b border-slate-800 text-xs">
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="w-3.5 h-3.5 text-slate-500 absolute left-2.5 top-2" />
            <input
              type="text"
              placeholder="Filter log..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-8 pr-2 py-1 bg-slate-950 border border-slate-700 rounded text-xs text-slate-200 focus:outline-none focus:border-sky-500 w-40"
            />
          </div>

          <div className="flex border border-slate-700 rounded overflow-hidden">
            {(['TEXT', 'HEX', 'BOTH'] as const).map((mode) => (
              <button
                key={mode}
                onClick={() => setDisplayMode(mode)}
                className={`px-2 py-1 ${
                  displayMode === mode ? 'bg-sky-600 text-white font-semibold' : 'bg-slate-800 text-slate-400'
                }`}
              >
                {mode}
              </button>
            ))}
          </div>

          <label className="flex items-center gap-1.5 cursor-pointer text-slate-400 select-none">
            <input
              type="checkbox"
              checked={autoScroll}
              onChange={(e) => setAutoScroll(e.target.checked)}
              className="accent-sky-500"
            />
            Auto-Scroll
          </label>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleExportTxt}
            className="flex items-center gap-1 px-2.5 py-1 bg-slate-800 hover:bg-slate-700 rounded border border-slate-700 text-slate-300"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export .txt</span>
          </button>
          <button
            onClick={onClearLogs}
            className="flex items-center gap-1 px-2.5 py-1 bg-rose-950/40 hover:bg-rose-900/40 text-rose-300 rounded border border-rose-800/40"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>Clear</span>
          </button>
        </div>
      </div>

      {/* Terminal Output Log Area */}
      <div
        ref={logContainerRef}
        className="flex-1 p-3 overflow-y-auto font-mono text-xs space-y-1.5 selection:bg-sky-600 selection:text-white"
      >
        {filteredLogs.length === 0 ? (
          <div className="text-slate-600 py-12 text-center">
            No terminal packets yet for Slot {selectedSlot + 1}. Send a command or start the simulator!
          </div>
        ) : (
          filteredLogs.map((log) => {
            const timeStr = new Date(log.timestamp).toLocaleTimeString();
            return (
              <div key={log.id} className="flex items-start gap-2 leading-relaxed">
                <span className="text-slate-500 shrink-0 select-none">[{timeStr}]</span>
                <span
                  className={`font-bold shrink-0 select-none ${
                    log.isTx ? 'text-sky-400' : 'text-emerald-400'
                  }`}
                >
                  {log.isTx ? 'TX ▶' : 'RX ◀'}
                </span>
                <div className="flex-1 break-all">
                  {(displayMode === 'TEXT' || displayMode === 'BOTH') && (
                    <span className={log.isTx ? 'text-slate-300' : 'text-emerald-200'}>
                      {log.text}
                    </span>
                  )}
                  {displayMode === 'BOTH' && <span className="text-slate-600 mx-2">|</span>}
                  {(displayMode === 'HEX' || displayMode === 'BOTH') && (
                    <span className="text-amber-400/90 text-[11px]">{log.hex}</span>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Macros Row */}
      <div className="bg-slate-900/90 border-t border-slate-800/80 px-4 py-2">
        <div className="text-[11px] font-semibold text-slate-400 mb-1.5 flex items-center justify-between">
          <span>Saved Hardware Macros</span>
          <span className="text-slate-500">Click to dispatch instantly</span>
        </div>
        <div className="flex flex-wrap items-center gap-1.5">
          {macros.map((macro) => (
            <button
              key={macro.id}
              onClick={() => onSendCommand(macro.command, macro.lineEnding)}
              className="flex items-center gap-1 px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 text-xs font-mono transition-all active:scale-95"
            >
              <Play className="w-2.5 h-2.5 text-sky-400" />
              <span>{macro.label}</span>
            </button>
          ))}

          {/* Quick macro adder */}
          <div className="flex items-center gap-1 ml-auto">
            <input
              type="text"
              placeholder="Label"
              value={newMacroLabel}
              onChange={(e) => setNewMacroLabel(e.target.value)}
              className="w-16 px-1.5 py-0.5 bg-slate-950 border border-slate-700 rounded text-xs text-slate-200"
            />
            <input
              type="text"
              placeholder="Cmd"
              value={newMacroCmd}
              onChange={(e) => setNewMacroCmd(e.target.value)}
              className="w-16 px-1.5 py-0.5 bg-slate-950 border border-slate-700 rounded text-xs text-slate-200"
            />
            <button
              onClick={addMacro}
              className="px-2 py-0.5 bg-sky-600 hover:bg-sky-500 text-white rounded text-xs font-semibold"
            >
              +
            </button>
          </div>
        </div>
      </div>

      {/* Bottom Send Input Bar */}
      <div className="p-3 bg-slate-900 border-t border-slate-800 flex items-center gap-2">
        <select
          value={lineEnding}
          onChange={(e) => setLineEnding(e.target.value as LineEnding)}
          className="bg-slate-950 border border-slate-700 text-slate-300 text-xs rounded px-2 py-2 font-mono focus:outline-none focus:border-sky-500"
        >
          <option value="NONE">None</option>
          <option value="LF">\n (LF)</option>
          <option value="CR">\r (CR)</option>
          <option value="CRLF">\r\n (CRLF)</option>
        </select>

        <input
          type="text"
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSend()}
          placeholder={`Send to Slot ${selectedSlot + 1} (e.g. F, B, PING, X:50,Y:80)...`}
          className="flex-1 bg-slate-950 border border-slate-700 rounded px-3 py-2 text-xs font-mono text-slate-100 placeholder-slate-500 focus:outline-none focus:border-sky-500"
        />

        <button
          onClick={handleSend}
          className="flex items-center gap-1.5 px-4 py-2 bg-sky-600 hover:bg-sky-500 text-white rounded font-semibold text-xs transition-all shadow-md shadow-sky-600/30"
        >
          <Send className="w-3.5 h-3.5" />
          <span>Send</span>
        </button>
      </div>
    </div>
  );
};
