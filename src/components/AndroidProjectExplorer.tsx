import React, { useState } from 'react';
import { ANDROID_PROJECT_FILES, downloadAndroidProjectZip, AndroidFileItem } from '../services/androidProjectZip';
import { Download, FileCode, Check, Copy, Folder, Terminal, Sparkles } from 'lucide-react';

interface Props {
  onClose: () => void;
}

export const AndroidProjectExplorer: React.FC<Props> = ({ onClose }) => {
  const [selectedFile, setSelectedFile] = useState<AndroidFileItem>(ANDROID_PROJECT_FILES[0]);
  const [copied, setCopied] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  const handleDownload = async () => {
    setDownloading(true);
    try {
      await downloadAndroidProjectZip();
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-5xl h-[85vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Modal Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/80">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-sky-600/20 text-sky-400 flex items-center justify-center font-bold">
              <FileCode className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-bold text-base text-slate-100">
                EmbedHub Native Android Project Tree
              </h2>
              <p className="text-xs text-slate-400">
                Kotlin • Jetpack Compose • Gradle Version Catalog • Full Source Code
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleDownload}
              disabled={downloading}
              className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-800 text-white font-semibold text-xs rounded-xl shadow-md shadow-emerald-600/30 transition-all active:scale-95"
            >
              <Download className="w-4 h-4" />
              <span>{downloading ? 'Packing Zip...' : 'Download Project .ZIP'}</span>
            </button>

            <button
              onClick={onClose}
              className="px-3 py-2 text-xs font-semibold text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition-all"
            >
              Close
            </button>
          </div>
        </div>

        {/* Explorer Content */}
        <div className="flex-1 grid grid-cols-1 md:grid-cols-3 min-h-0">
          {/* File Tree Column */}
          <div className="border-r border-slate-800 bg-slate-950/50 p-4 overflow-y-auto space-y-1 font-mono text-xs">
            <div className="text-[11px] font-sans font-bold text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1.5">
              <Folder className="w-3.5 h-3.5 text-sky-400" />
              <span>Android Repository Files</span>
            </div>

            {ANDROID_PROJECT_FILES.map((file) => {
              const isSelected = selectedFile.path === file.path;
              return (
                <button
                  key={file.path}
                  onClick={() => setSelectedFile(file)}
                  className={`w-full text-left px-2.5 py-1.5 rounded-lg transition-all flex items-center justify-between ${
                    isSelected
                      ? 'bg-sky-600/20 text-sky-300 font-semibold border border-sky-500/30'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
                  }`}
                >
                  <span className="truncate">{file.path}</span>
                  <span className="text-[9px] uppercase px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 shrink-0 ml-1">
                    {file.category}
                  </span>
                </button>
              );
            })}

            {/* Terminal Command Tip */}
            <div className="p-3 bg-slate-900 border border-slate-800 rounded-xl mt-4 text-[11px] font-sans text-slate-300 space-y-1">
              <div className="font-bold flex items-center gap-1 text-sky-400">
                <Terminal className="w-3.5 h-3.5" />
                <span>Build via Terminal:</span>
              </div>
              <code className="block p-1.5 bg-slate-950 rounded text-slate-200 font-mono text-[10px]">
                cd android && ./gradlew assembleDebug
              </code>
            </div>
          </div>

          {/* Code Preview Column */}
          <div className="md:col-span-2 flex flex-col bg-slate-950">
            <div className="flex items-center justify-between px-4 py-2 border-b border-slate-800 bg-slate-900/40 text-xs">
              <span className="font-mono text-slate-300">{selectedFile.path}</span>
              <button
                onClick={handleCopy}
                className="flex items-center gap-1 text-sky-400 hover:text-sky-300 text-xs font-medium"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copied ? 'Copied!' : 'Copy Code'}</span>
              </button>
            </div>

            <pre className="flex-1 p-4 overflow-y-auto font-mono text-xs text-slate-300 bg-slate-950/80 leading-relaxed whitespace-pre select-all">
              {selectedFile.content}
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
};
