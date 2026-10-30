import React, { useState, useEffect } from 'react';
import { usePWAInstall } from '../services/usePWAInstall';
import { downloadAndroidProjectZip } from '../services/androidProjectZip';
import {
  Smartphone,
  Download,
  Terminal,
  ShieldCheck,
  CheckCircle2,
  Copy,
  ExternalLink,
  Cpu,
  Bluetooth,
  Usb,
  Wifi,
  Radio,
  Layers,
  ArrowRight,
  Sparkles,
  RotateCw,
  FolderTree,
  AlertCircle
} from 'lucide-react';

interface Props {
  onOpenAndroidExplorer: () => void;
  isLandscape?: boolean;
  onToggleOrientation?: () => void;
}

export const AndroidAppHubView: React.FC<Props> = ({
  onOpenAndroidExplorer,
  isLandscape = false,
  onToggleOrientation
}) => {
  const { isInstallable, isInstalled, isAndroid, isIOS, install } = usePWAInstall();
  const [downloading, setDownloading] = useState(false);
  const [copiedCmd, setCopiedCmd] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'native' | 'pwa' | 'capacitor' | 'permissions'>('overview');

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedCmd(id);
    setTimeout(() => setCopiedCmd(null), 2000);
  };

  const handleDownloadZip = async () => {
    setDownloading(true);
    try {
      await downloadAndroidProjectZip();
    } finally {
      setDownloading(false);
    }
  };

  // Hardware API capabilities detected on current device
  const hasBluetooth = typeof navigator !== 'undefined' && 'bluetooth' in navigator;
  const hasSerial = typeof navigator !== 'undefined' && 'serial' in navigator;
  const hasServiceWorker = typeof navigator !== 'undefined' && 'serviceWorker' in navigator;

  return (
    <div className="h-full flex flex-col bg-slate-950 text-slate-100 overflow-y-auto p-4 md:p-6 font-sans">
      {/* Hero Banner */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-sky-900/40 via-slate-900 to-emerald-950/40 border border-sky-500/30 p-6 shadow-2xl mb-6">
        <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="max-w-2xl space-y-2">
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                Android 14 Ready (API 34)
              </span>
              <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-sky-500/20 text-sky-300 border border-sky-500/30">
                Jetpack Compose + Material 3
              </span>
            </div>
            <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
              <Smartphone className="w-8 h-8 text-sky-400" />
              Convert EmbedHub to Android Apps
            </h1>
            <p className="text-sm text-slate-300 leading-relaxed">
              Run EmbedHub on any Android phone or tablet as a <strong>Native Kotlin + Compose App</strong>, an <strong>Installable WebAPK PWA</strong>, or a <strong>Capacitor Hybrid APK</strong> with direct hardware access (BLE, Classic BT SPP, USB OTG, WiFi, MQTT).
            </p>
          </div>

          {/* Quick Action Buttons */}
          <div className="flex flex-wrap items-center gap-3">
            <button
              onClick={handleDownloadZip}
              disabled={downloading}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-800 text-white font-bold text-sm shadow-lg shadow-emerald-600/30 transition-all active:scale-95"
            >
              <Download className="w-4 h-4" />
              <span>{downloading ? 'Packing Zip...' : 'Download Android .ZIP'}</span>
            </button>

            {isInstallable && (
              <button
                onClick={install}
                className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-bold text-sm shadow-lg shadow-sky-600/30 transition-all active:scale-95"
              >
                <Smartphone className="w-4 h-4" />
                <span>Install on Android</span>
              </button>
            )}

            <button
              onClick={onOpenAndroidExplorer}
              className="flex items-center gap-2 px-3.5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 font-semibold text-sm transition-all"
            >
              <FolderTree className="w-4 h-4 text-sky-400" />
              <span>Browse Source Tree</span>
            </button>
          </div>
        </div>

        {/* Decorative Grid Lines */}
        <div className="absolute -right-8 -bottom-8 w-64 h-64 bg-sky-500/10 rounded-full blur-3xl pointer-events-none" />
      </div>

      {/* Navigation Sub-Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-3 mb-6 overflow-x-auto text-sm">
        {[
          { id: 'overview', label: 'Conversion Methods' },
          { id: 'native', label: 'Native Kotlin App' },
          { id: 'pwa', label: 'Android PWA / WebAPK' },
          { id: 'capacitor', label: 'Capacitor Hybrid' },
          { id: 'permissions', label: 'Permissions & Drivers' }
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={`px-3 py-1.5 rounded-lg font-medium whitespace-nowrap transition-all ${
              activeTab === tab.id
                ? 'bg-sky-600/20 text-sky-300 font-bold border border-sky-500/30'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* TAB 1: OVERVIEW & COMPARISON */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {/* Native Android Card */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 hover:border-sky-500/50 transition-all flex flex-col justify-between">
              <div>
                <div className="w-10 h-10 rounded-lg bg-sky-500/20 text-sky-400 flex items-center justify-center font-bold mb-3">
                  <Cpu className="w-5 h-5" />
                </div>
                <div className="flex items-center justify-between mb-1">
                  <h3 className="font-bold text-base text-slate-100">Native Kotlin & Compose</h3>
                  <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-sky-500/10 text-sky-400 border border-sky-500/20">
                    Highest Performance
                  </span>
                </div>
                <p className="text-xs text-slate-400 mb-4">
                  Full Android Studio project in <code>/android</code> with Gradle version catalog, Coroutines, StateFlow, Room DB, and Android 14 Foreground Service.
                </p>
                <ul className="text-xs space-y-1.5 text-slate-300 mb-4">
                  <li className="flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <span>Native BLE, Classic BT SPP & USB OTG</span>
                  </li>
                  <li className="flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <span>Screen-off background connection keepalive</span>
                  </li>
                  <li className="flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <span>Ready for Google Play Store publication</span>
                  </li>
                </ul>
              </div>
              <button
                onClick={() => setActiveTab('native')}
                className="w-full py-2 bg-sky-600/20 hover:bg-sky-600/30 text-sky-300 font-semibold text-xs rounded-lg border border-sky-500/30 flex items-center justify-center gap-1 transition-all"
              >
                <span>View Build Steps</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* PWA / WebAPK Card */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 hover:border-emerald-500/50 transition-all flex flex-col justify-between">
              <div>
                <div className="w-10 h-10 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold mb-3">
                  <Smartphone className="w-5 h-5" />
                </div>
                <div className="flex items-center justify-between mb-1">
                  <h3 className="font-bold text-base text-slate-100">Android PWA / WebAPK</h3>
                  <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                    Zero Compile Needed
                  </span>
                </div>
                <p className="text-xs text-slate-400 mb-4">
                  Installs immediately from Android Chrome/Edge/Brave. Android automatically wraps it into a real WebAPK that runs full-screen with offline support.
                </p>
                <ul className="text-xs space-y-1.5 text-slate-300 mb-4">
                  <li className="flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <span>Instant home screen launch without compilation</span>
                  </li>
                  <li className="flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <span>Hardware Web Bluetooth & Web Serial access</span>
                  </li>
                  <li className="flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <span>Offline caching via VitePWA Service Worker</span>
                  </li>
                </ul>
              </div>
              <button
                onClick={() => setActiveTab('pwa')}
                className="w-full py-2 bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 font-semibold text-xs rounded-lg border border-emerald-500/30 flex items-center justify-center gap-1 transition-all"
              >
                <span>Install on Phone</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Capacitor Hybrid Card */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 hover:border-purple-500/50 transition-all flex flex-col justify-between">
              <div>
                <div className="w-10 h-10 rounded-lg bg-purple-500/20 text-purple-400 flex items-center justify-center font-bold mb-3">
                  <Layers className="w-5 h-5" />
                </div>
                <div className="flex items-center justify-between mb-1">
                  <h3 className="font-bold text-base text-slate-100">Capacitor Android</h3>
                  <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-purple-500/10 text-purple-400 border border-purple-500/20">
                    Fastest Hybrid APK
                  </span>
                </div>
                <p className="text-xs text-slate-400 mb-4">
                  Wraps this complete React + Vite UI inside an Android Studio APK with native Android plugins in 3 terminal commands.
                </p>
                <ul className="text-xs space-y-1.5 text-slate-300 mb-4">
                  <li className="flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <span>Reuses the exact same responsive React UI</span>
                  </li>
                  <li className="flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <span>Includes <code>capacitor.config.ts</code> configuration</span>
                  </li>
                  <li className="flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <span>Generates signed APK or AAB bundle</span>
                  </li>
                </ul>
              </div>
              <button
                onClick={() => setActiveTab('capacitor')}
                className="w-full py-2 bg-purple-600/20 hover:bg-purple-600/30 text-purple-300 font-semibold text-xs rounded-lg border border-purple-500/30 flex items-center justify-center gap-1 transition-all"
              >
                <span>View Capacitor Setup</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* Device Capability Status Bar */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4 flex flex-wrap items-center justify-between gap-4 text-xs">
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-sky-400" />
              <span className="font-bold text-slate-200">Current Device Hardware Capabilities:</span>
            </div>
            <div className="flex flex-wrap items-center gap-3">
              <span className={`px-2 py-1 rounded flex items-center gap-1.5 ${hasBluetooth ? 'bg-emerald-500/20 text-emerald-300' : 'bg-slate-800 text-slate-400'}`}>
                <Bluetooth className="w-3.5 h-3.5" />
                <span>Web Bluetooth: {hasBluetooth ? 'Supported' : 'Native only'}</span>
              </span>
              <span className={`px-2 py-1 rounded flex items-center gap-1.5 ${hasSerial ? 'bg-emerald-500/20 text-emerald-300' : 'bg-slate-800 text-slate-400'}`}>
                <Usb className="w-3.5 h-3.5" />
                <span>USB Serial OTG: {hasSerial ? 'Supported' : 'Native only'}</span>
              </span>
              <span className={`px-2 py-1 rounded flex items-center gap-1.5 ${hasServiceWorker ? 'bg-emerald-500/20 text-emerald-300' : 'bg-slate-800 text-slate-400'}`}>
                <Wifi className="w-3.5 h-3.5" />
                <span>PWA Service Worker: {hasServiceWorker ? 'Active' : 'Unavailable'}</span>
              </span>
              <span className={`px-2 py-1 rounded flex items-center gap-1.5 ${isInstalled ? 'bg-emerald-500/20 text-emerald-300' : 'bg-slate-800 text-slate-400'}`}>
                <Smartphone className="w-3.5 h-3.5" />
                <span>WebAPK Status: {isInstalled ? 'Installed' : 'Browser Mode'}</span>
              </span>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: NATIVE KOTLIN & JETPACK COMPOSE */}
      {activeTab === 'native' && (
        <div className="space-y-6">
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
            <h3 className="font-bold text-base text-slate-100 mb-2 flex items-center gap-2">
              <Cpu className="w-5 h-5 text-sky-400" />
              Build the Native Android APK (.apk / .aab)
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              EmbedHub includes a complete native Kotlin Gradle project located in <code>/android</code>. You can build it directly via the Gradle wrapper or open it in Android Studio.
            </p>

            <div className="space-y-4">
              {/* Step 1 */}
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-sky-400 flex items-center gap-1.5">
                    <span className="w-5 h-5 rounded-full bg-sky-500/20 flex items-center justify-center text-[10px]">1</span>
                    Clone or Download Android Project
                  </span>
                  <button
                    onClick={handleDownloadZip}
                    disabled={downloading}
                    className="text-xs font-semibold px-3 py-1 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg flex items-center gap-1 transition-all"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>{downloading ? 'Downloading...' : 'Download .ZIP'}</span>
                  </button>
                </div>
                <p className="text-xs text-slate-400">
                  The downloaded zip includes <code>settings.gradle.kts</code>, <code>gradle/libs.versions.toml</code>, <code>build.gradle.kts</code>, <code>AndroidManifest.xml</code>, all Kotlin models, transports, and Jetpack Compose screens.
                </p>
              </div>

              {/* Step 2 */}
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-sky-400 flex items-center gap-1.5">
                    <span className="w-5 h-5 rounded-full bg-sky-500/20 flex items-center justify-center text-[10px]">2</span>
                    Compile APK via Command Line
                  </span>
                  <button
                    onClick={() => copyToClipboard('cd android && ./gradlew assembleDebug', 'step2')}
                    className="text-xs text-sky-400 hover:text-sky-300 flex items-center gap-1 font-mono"
                  >
                    {copiedCmd === 'step2' ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>{copiedCmd === 'step2' ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
                <code className="block p-2.5 bg-slate-900 rounded-lg text-emerald-400 font-mono text-xs">
                  cd android && ./gradlew assembleDebug
                </code>
                <p className="text-[11px] text-slate-400 mt-2">
                  Build output will be located at: <code>android/app/build/outputs/apk/debug/app-debug.apk</code>.
                </p>
              </div>

              {/* Step 3 */}
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-sky-400 flex items-center gap-1.5">
                    <span className="w-5 h-5 rounded-full bg-sky-500/20 flex items-center justify-center text-[10px]">3</span>
                    Install to Android Phone via ADB
                  </span>
                  <button
                    onClick={() => copyToClipboard('adb install -r android/app/build/outputs/apk/debug/app-debug.apk', 'step3')}
                    className="text-xs text-sky-400 hover:text-sky-300 flex items-center gap-1 font-mono"
                  >
                    {copiedCmd === 'step3' ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>{copiedCmd === 'step3' ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
                <code className="block p-2.5 bg-slate-900 rounded-lg text-emerald-400 font-mono text-xs">
                  adb install -r android/app/build/outputs/apk/debug/app-debug.apk
                </code>
                <p className="text-[11px] text-slate-400 mt-2">
                  Or drag and drop the <code>.apk</code> directly onto an Android emulator or connected device with USB debugging enabled.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: ANDROID PWA / WEBAPK */}
      {activeTab === 'pwa' && (
        <div className="space-y-6">
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
            <h3 className="font-bold text-base text-slate-100 mb-2 flex items-center gap-2">
              <Smartphone className="w-5 h-5 text-emerald-400" />
              Direct Installation on Android (PWA / WebAPK)
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Modern Android devices automatically convert standards-compliant PWAs into <strong>native WebAPKs</strong> generated by Google Play Services. The app receives a dedicated home screen icon, runs full-screen without URL bars, and accesses Web Bluetooth and Web Serial.
            </p>

            {/* Install Button & Status */}
            <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4 mb-4">
              <div>
                <div className="font-bold text-sm text-slate-200">
                  {isInstalled ? 'EmbedHub is already installed on this device!' : 'Ready to Install on this Device'}
                </div>
                <div className="text-xs text-slate-400">
                  {isInstalled
                    ? 'Running in native standalone WebAPK mode with full hardware access.'
                    : 'Tap below to trigger Android native installation prompt.'}
                </div>
              </div>

              {!isInstalled && (
                <button
                  onClick={install}
                  className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs rounded-xl shadow-md shadow-emerald-600/30 flex items-center gap-2 transition-all active:scale-95"
                >
                  <Smartphone className="w-4 h-4" />
                  <span>Install EmbedHub APK</span>
                </button>
              )}
            </div>

            {/* Manual Instructions for Android Browsers */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                <h4 className="font-bold text-xs text-sky-400 mb-2">Android Chrome / Brave</h4>
                <ol className="text-xs text-slate-300 space-y-1.5 list-decimal list-inside">
                  <li>Tap the three dots menu (⋮) in the top-right corner.</li>
                  <li>Select <strong>"Install app"</strong> or <strong>"Add to Home screen"</strong>.</li>
                  <li>Confirm by tapping <strong>"Install"</strong>.</li>
                  <li>Android will generate an official WebAPK and place it in your app drawer.</li>
                </ol>
              </div>

              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                <h4 className="font-bold text-xs text-sky-400 mb-2">Samsung Internet</h4>
                <ol className="text-xs text-slate-300 space-y-1.5 list-decimal list-inside">
                  <li>Tap the hamburger menu (≡) in the bottom-right bar.</li>
                  <li>Tap <strong>"+ Add page to"</strong>.</li>
                  <li>Select <strong>"App screen"</strong> or <strong>"Home screen"</strong>.</li>
                  <li>The app will launch in immersive fullscreen mode.</li>
                </ol>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: CAPACITOR HYBRID CONVERSION */}
      {activeTab === 'capacitor' && (
        <div className="space-y-6">
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
            <h3 className="font-bold text-base text-slate-100 mb-2 flex items-center gap-2">
              <Layers className="w-5 h-5 text-purple-400" />
              Capacitor Android Hybrid APK Builder
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Capacitor turns any React web application into a native Android Studio project with full hardware bridge plugins. <code>capacitor.config.ts</code> is already configured in the repository root.
            </p>

            <div className="space-y-4">
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-purple-400">Step 1: Install Capacitor CLI & Core</span>
                  <button
                    onClick={() => copyToClipboard('npm install -D @capacitor/cli @capacitor/core @capacitor/android', 'cap1')}
                    className="text-xs text-sky-400 hover:text-sky-300 flex items-center gap-1 font-mono"
                  >
                    {copiedCmd === 'cap1' ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>{copiedCmd === 'cap1' ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
                <code className="block p-2 bg-slate-900 rounded text-purple-300 font-mono text-xs">
                  npm install -D @capacitor/cli @capacitor/core @capacitor/android
                </code>
              </div>

              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-purple-400">Step 2: Build Web Assets & Add Android Platform</span>
                  <button
                    onClick={() => copyToClipboard('npm run build && npx cap add android', 'cap2')}
                    className="text-xs text-sky-400 hover:text-sky-300 flex items-center gap-1 font-mono"
                  >
                    {copiedCmd === 'cap2' ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>{copiedCmd === 'cap2' ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
                <code className="block p-2 bg-slate-900 rounded text-purple-300 font-mono text-xs">
                  npm run build && npx cap add android
                </code>
              </div>

              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-purple-400">Step 3: Open in Android Studio & Run on Phone</span>
                  <button
                    onClick={() => copyToClipboard('npx cap open android', 'cap3')}
                    className="text-xs text-sky-400 hover:text-sky-300 flex items-center gap-1 font-mono"
                  >
                    {copiedCmd === 'cap3' ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>{copiedCmd === 'cap3' ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
                <code className="block p-2 bg-slate-900 rounded text-purple-300 font-mono text-xs">
                  npx cap open android
                </code>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 5: PERMISSIONS & DRIVERS */}
      {activeTab === 'permissions' && (
        <div className="space-y-6">
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
            <h3 className="font-bold text-base text-slate-100 mb-2 flex items-center gap-2">
              <ShieldCheck className="w-5 h-5 text-sky-400" />
              Android 12-14+ Runtime Permissions & Hardware Drivers Matrix
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              EmbedHub contains full production compliance for modern Android operating systems, avoiding common pitfalls with Bluetooth and USB permissions.
            </p>

            <div className="overflow-x-auto">
              <table className="w-full text-xs text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-800 text-slate-400">
                    <th className="py-2 px-3 font-semibold">Permission / Feature</th>
                    <th className="py-2 px-3 font-semibold">Android Target</th>
                    <th className="py-2 px-3 font-semibold">Purpose in EmbedHub</th>
                    <th className="py-2 px-3 font-semibold">Configured In</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60 text-slate-300">
                  <tr>
                    <td className="py-2.5 px-3 font-mono text-sky-300">BLUETOOTH_SCAN</td>
                    <td className="py-2.5 px-3">Android 12+ (API 31+)</td>
                    <td className="py-2.5 px-3">Scans for HM-10, ESP32, and Nordic BLE peripherals with <code>neverForLocation</code> flag.</td>
                    <td className="py-2.5 px-3 text-emerald-400">AndroidManifest.xml</td>
                  </tr>
                  <tr>
                    <td className="py-2.5 px-3 font-mono text-sky-300">BLUETOOTH_CONNECT</td>
                    <td className="py-2.5 px-3">Android 12+ (API 31+)</td>
                    <td className="py-2.5 px-3">Establishes GATT and Classic RFCOMM SPP socket connections.</td>
                    <td className="py-2.5 px-3 text-emerald-400">AndroidManifest.xml</td>
                  </tr>
                  <tr>
                    <td className="py-2.5 px-3 font-mono text-sky-300">android.hardware.usb.host</td>
                    <td className="py-2.5 px-3">All versions</td>
                    <td className="py-2.5 px-3">Direct USB OTG access to CH340, CP210x, FTDI, and Arduino Uno serial chips.</td>
                    <td className="py-2.5 px-3 text-emerald-400">device_filter.xml</td>
                  </tr>
                  <tr>
                    <td className="py-2.5 px-3 font-mono text-sky-300">FOREGROUND_SERVICE_CONNECTED_DEVICE</td>
                    <td className="py-2.5 px-3">Android 14+ (API 34)</td>
                    <td className="py-2.5 px-3">Prevents Android from freezing hardware telemetry or dropping links when screen turns off.</td>
                    <td className="py-2.5 px-3 text-emerald-400">AndroidManifest.xml</td>
                  </tr>
                  <tr>
                    <td className="py-2.5 px-3 font-mono text-sky-300">CHANGE_WIFI_MULTICAST_STATE</td>
                    <td className="py-2.5 px-3">All versions</td>
                    <td className="py-2.5 px-3">Enables mDNS / NSD network discovery of local ESP8266 and ESP32 boards on WiFi.</td>
                    <td className="py-2.5 px-3 text-emerald-400">AndroidManifest.xml</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
