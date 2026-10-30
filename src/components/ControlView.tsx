import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  AlertOctagon,
  Volume2,
  Lightbulb,
  Zap,
  ShieldAlert,
  Compass,
  RotateCw,
  Gauge,
  Sliders,
  Radio,
  Crosshair
} from 'lucide-react';

interface Props {
  selectedSlot: number;
  onSendCommand: (cmd: string) => void;
  isLandscape?: boolean;
  onToggleOrientation?: () => void;
}

export const ControlView: React.FC<Props> = ({
  selectedSlot,
  onSendCommand,
  isLandscape = false,
  onToggleOrientation
}) => {
  const [speed, setSpeed] = useState(5);
  const [activeDirection, setActiveDirection] = useState<string | null>(null);
  const [joystickPos, setJoystickPos] = useState({ x: 0, y: 0 });
  const [isDraggingJoystick, setIsDraggingJoystick] = useState(false);
  const [tiltSteering, setTiltSteering] = useState(false);
  const [headlights, setHeadlights] = useState(false);
  const [auxRelay, setAuxRelay] = useState(false);
  const [failsafeTriggered, setFailsafeTriggered] = useState(false);
  const [leftMode, setLeftMode] = useState<'DPAD' | 'JOYSTICK'>('DPAD');

  // Simulated gyro pitch and roll for HUD artificial horizon
  const [simulatedRoll, setSimulatedRoll] = useState(0);
  const [simulatedPitch, setSimulatedPitch] = useState(0);

  const joystickRef = useRef<HTMLDivElement>(null);
  const joystickLoopRef = useRef<any>(null);

  // Send Stop failsafe helper
  const sendStop = useCallback(() => {
    setActiveDirection(null);
    setJoystickPos({ x: 0, y: 0 });
    setSimulatedRoll(0);
    setSimulatedPitch(0);
    onSendCommand('S');
    setFailsafeTriggered(true);
    setTimeout(() => setFailsafeTriggered(false), 800);
  }, [onSendCommand]);

  // D-Pad press and release handlers
  const handleDpadPress = (cmd: string) => {
    setActiveDirection(cmd);
    if (cmd === 'F') setSimulatedPitch(15);
    if (cmd === 'B') setSimulatedPitch(-15);
    if (cmd === 'L') setSimulatedRoll(-25);
    if (cmd === 'R') setSimulatedRoll(25);
    onSendCommand(cmd);
  };

  const handleDpadRelease = () => {
    sendStop();
  };

  // 20Hz continuous Joystick transmit loop
  useEffect(() => {
    if (isDraggingJoystick) {
      joystickLoopRef.current = setInterval(() => {
        onSendCommand(`X:${joystickPos.x},Y:${joystickPos.y}`);
        setSimulatedRoll(Math.round(joystickPos.x * 0.3));
        setSimulatedPitch(Math.round(joystickPos.y * 0.25));
      }, 50); // 20Hz
    } else {
      if (joystickLoopRef.current) {
        clearInterval(joystickLoopRef.current);
        joystickLoopRef.current = null;
      }
    }
    return () => {
      if (joystickLoopRef.current) clearInterval(joystickLoopRef.current);
    };
  }, [isDraggingJoystick, joystickPos, onSendCommand]);

  // Window blur / mouseleave failsafe
  useEffect(() => {
    const handleBlur = () => {
      if (activeDirection || isDraggingJoystick) {
        sendStop();
      }
    };
    window.addEventListener('blur', handleBlur);
    return () => window.removeEventListener('blur', handleBlur);
  }, [activeDirection, isDraggingJoystick, sendStop]);

  // Joystick pointer calculations
  const updateJoystickFromEvent = (clientX: number, clientY: number) => {
    if (!joystickRef.current) return;
    const rect = joystickRef.current.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    const maxRadius = rect.width / 2 - 20;

    let deltaX = clientX - centerX;
    let deltaY = clientY - centerY;
    const dist = Math.hypot(deltaX, deltaY);

    if (dist > maxRadius) {
      deltaX = (deltaX / dist) * maxRadius;
      deltaY = (deltaY / dist) * maxRadius;
    }

    const normX = Math.round((deltaX / maxRadius) * 100);
    const normY = Math.round((-deltaY / maxRadius) * 100);

    setJoystickPos({ x: normX, y: normY });
  };

  const onPointerDown = (e: React.PointerEvent) => {
    setIsDraggingJoystick(true);
    updateJoystickFromEvent(e.clientX, e.clientY);
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
  };

  const onPointerMove = (e: React.PointerEvent) => {
    if (isDraggingJoystick) {
      updateJoystickFromEvent(e.clientX, e.clientY);
    }
  };

  const onPointerUp = (e: React.PointerEvent) => {
    if (isDraggingJoystick) {
      setIsDraggingJoystick(false);
      sendStop();
    }
  };

  return (
    <div className="flex flex-col h-full bg-slate-950 text-slate-100 p-3 select-none overflow-y-auto">
      {/* Top Header Strip */}
      <div className="flex flex-wrap items-center justify-between gap-3 p-2.5 bg-slate-900 border border-slate-800 rounded-xl mb-3">
        {/* Speed Slider */}
        <div className="flex items-center gap-2.5 flex-1 min-w-[200px]">
          <span className="text-xs font-semibold text-slate-400">PWM Speed:</span>
          <input
            type="range"
            min="0"
            max="9"
            value={speed}
            onChange={(e) => {
              const val = Number(e.target.value);
              setSpeed(val);
              onSendCommand(`V${val}`);
            }}
            className="flex-1 accent-sky-500 h-2 bg-slate-800 rounded-lg cursor-pointer"
          />
          <span className="font-mono font-bold text-sky-400 text-sm w-5 text-center">{speed}</span>
        </div>

        {/* Failsafe indicator & Screen Rotation Controls */}
        <div className="flex items-center gap-2">
          {failsafeTriggered && (
            <div className="flex items-center gap-1 text-[11px] font-bold text-rose-400 bg-rose-500/10 px-2 py-1 rounded border border-rose-500/20 animate-pulse">
              <ShieldAlert className="w-3.5 h-3.5" />
              <span>FAILSAFE 'S'</span>
            </div>
          )}

          {/* Screen Rotation Toggle Button */}
          {onToggleOrientation && (
            <button
              onClick={onToggleOrientation}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-xs font-semibold transition-all ${
                isLandscape
                  ? 'bg-sky-500/20 text-sky-300 border-sky-500/40 shadow-sm shadow-sky-500/20'
                  : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'
              }`}
              title="Rotate Screen: Toggle between Portrait and Landscape Driving Cockpit"
            >
              <RotateCw className="w-3.5 h-3.5 text-sky-400" />
              <span>{isLandscape ? 'Landscape Cockpit' : 'Portrait'}</span>
            </button>
          )}

          {/* Emergency Stop Button */}
          <button
            onClick={sendStop}
            className="flex items-center gap-1.5 px-4 py-1.5 rounded-lg bg-gradient-to-r from-rose-600 to-red-700 hover:from-rose-500 hover:to-red-600 text-white font-extrabold text-xs shadow-md shadow-rose-600/30 active:scale-95 transition-all"
          >
            <AlertOctagon className="w-4 h-4" />
            <span>STOP</span>
          </button>
        </div>
      </div>

      {isLandscape ? (
        /* WIDESCREEN LANDSCAPE COCKPIT VIEW */
        <div className="flex-1 grid grid-cols-3 gap-4 items-center justify-between max-w-6xl mx-auto w-full min-h-0 py-2">
          {/* LEFT THUMB: D-Pad or Joystick */}
          <div className="flex flex-col items-center justify-center p-3 bg-slate-900/60 border border-slate-800 rounded-2xl h-full">
            <div className="flex items-center gap-2 mb-2">
              <button
                onClick={() => setLeftMode('DPAD')}
                className={`text-[10px] font-bold px-2.5 py-1 rounded-md transition-all ${
                  leftMode === 'DPAD' ? 'bg-sky-600 text-white' : 'bg-slate-800 text-slate-400'
                }`}
              >
                D-Pad
              </button>
              <button
                onClick={() => setLeftMode('JOYSTICK')}
                className={`text-[10px] font-bold px-2.5 py-1 rounded-md transition-all ${
                  leftMode === 'JOYSTICK' ? 'bg-sky-600 text-white' : 'bg-slate-800 text-slate-400'
                }`}
              >
                20Hz Joystick
              </button>
            </div>

            {leftMode === 'DPAD' ? (
              <div className="relative w-44 h-44 flex items-center justify-center scale-90">
                <button
                  onMouseDown={() => handleDpadPress('F')}
                  onMouseUp={handleDpadRelease}
                  onTouchStart={(e) => { e.preventDefault(); handleDpadPress('F'); }}
                  onTouchEnd={handleDpadRelease}
                  className={`absolute top-0 w-14 h-14 rounded-xl font-black text-xl flex items-center justify-center border shadow-md transition-all ${
                    activeDirection === 'F' ? 'bg-sky-500 text-white border-sky-400 scale-95' : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'
                  }`}
                >
                  ▲
                </button>
                <button
                  onMouseDown={() => handleDpadPress('L')}
                  onMouseUp={handleDpadRelease}
                  onTouchStart={(e) => { e.preventDefault(); handleDpadPress('L'); }}
                  onTouchEnd={handleDpadRelease}
                  className={`absolute left-0 w-14 h-14 rounded-xl font-black text-xl flex items-center justify-center border shadow-md transition-all ${
                    activeDirection === 'L' ? 'bg-sky-500 text-white border-sky-400 scale-95' : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'
                  }`}
                >
                  ◀
                </button>
                <div className="w-12 h-12 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-center text-xs font-mono font-bold text-slate-400">
                  {activeDirection || '●'}
                </div>
                <button
                  onMouseDown={() => handleDpadPress('R')}
                  onMouseUp={handleDpadRelease}
                  onTouchStart={(e) => { e.preventDefault(); handleDpadPress('R'); }}
                  onTouchEnd={handleDpadRelease}
                  className={`absolute right-0 w-14 h-14 rounded-xl font-black text-xl flex items-center justify-center border shadow-md transition-all ${
                    activeDirection === 'R' ? 'bg-sky-500 text-white border-sky-400 scale-95' : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'
                  }`}
                >
                  ▶
                </button>
                <button
                  onMouseDown={() => handleDpadPress('B')}
                  onMouseUp={handleDpadRelease}
                  onTouchStart={(e) => { e.preventDefault(); handleDpadPress('B'); }}
                  onTouchEnd={handleDpadRelease}
                  className={`absolute bottom-0 w-14 h-14 rounded-xl font-black text-xl flex items-center justify-center border shadow-md transition-all ${
                    activeDirection === 'B' ? 'bg-sky-500 text-white border-sky-400 scale-95' : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'
                  }`}
                >
                  ▼
                </button>
              </div>
            ) : (
              <div
                ref={joystickRef}
                onPointerDown={onPointerDown}
                onPointerMove={onPointerMove}
                onPointerUp={onPointerUp}
                className="w-40 h-40 rounded-full bg-slate-950 border-2 border-slate-800 shadow-inner flex items-center justify-center cursor-grab active:cursor-grabbing relative touch-none"
              >
                <div className="absolute inset-x-0 top-1/2 h-px bg-slate-800/80 pointer-events-none" />
                <div className="absolute inset-y-0 left-1/2 w-px bg-slate-800/80 pointer-events-none" />
                <div
                  className={`w-14 h-14 rounded-full border-2 shadow-xl flex items-center justify-center transition-transform pointer-events-none ${
                    isDraggingJoystick ? 'bg-sky-500 border-sky-300 shadow-sky-500/50' : 'bg-slate-800 border-slate-600'
                  }`}
                  style={{
                    transform: `translate(${joystickPos.x * 0.55}px, ${-joystickPos.y * 0.55}px)`
                  }}
                >
                  <div className="w-3.5 h-3.5 rounded-full bg-white/40" />
                </div>
              </div>
            )}
            <div className="text-[10px] text-slate-500 font-mono mt-1">
              {leftMode === 'DPAD' ? 'F/B/L/R (S on lift)' : `X:${joystickPos.x} Y:${joystickPos.y}`}
            </div>
          </div>

          {/* CENTER: HUD Flight/Drive Deck */}
          <div className="flex flex-col items-center justify-between p-3 bg-slate-900/60 border border-slate-800 rounded-2xl h-full">
            {/* Horizon HUD Canvas */}
            <div className="w-full flex items-center justify-between px-2 text-[10px] text-slate-400 font-mono">
              <span>ROLL: {simulatedRoll}°</span>
              <span className="text-sky-400 font-bold">COCKPIT HUD</span>
              <span>PITCH: {simulatedPitch}°</span>
            </div>

            {/* Artificial Horizon Graphic */}
            <div className="relative w-44 h-24 rounded-xl bg-slate-950 border border-slate-800 overflow-hidden flex items-center justify-center">
              <div
                className="absolute inset-x-0 h-0.5 bg-sky-400 shadow-sm shadow-sky-400/80 transition-transform duration-100"
                style={{
                  transform: `translateY(${simulatedPitch * 1.2}px) rotate(${simulatedRoll}deg)`
                }}
              />
              <Crosshair className="w-6 h-6 text-amber-400 pointer-events-none" />
            </div>

            {/* Emergency STOP Button in center of HUD */}
            <button
              onClick={sendStop}
              className="w-full py-3 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-black text-sm flex items-center justify-center gap-2 shadow-lg shadow-rose-600/30 active:scale-95 transition-all"
            >
              <AlertOctagon className="w-5 h-5" />
              <span>EMERGENCY STOP (S)</span>
            </button>

            {/* Status indicators */}
            <div className="flex items-center gap-4 text-[10px] text-slate-400 font-mono">
              <span>Motors: {activeDirection || 'IDLE'}</span>
              <span>PWM: {speed}/9</span>
              <span>Rate: 20Hz</span>
            </div>
          </div>

          {/* RIGHT THUMB: Speed & Auxiliary Actuators */}
          <div className="flex flex-col items-center justify-center p-3 bg-slate-900/60 border border-slate-800 rounded-2xl h-full gap-2">
            <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1">
              Vehicle Actuators
            </div>

            <div className="grid grid-cols-2 gap-2 w-full">
              {/* Horn */}
              <button
                onMouseDown={() => onSendCommand('H')}
                onMouseUp={() => onSendCommand('h')}
                className="py-2.5 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-semibold flex items-center justify-center gap-1.5 active:bg-amber-600 active:text-white"
              >
                <Volume2 className="w-3.5 h-3.5 text-amber-400" />
                <span>Horn</span>
              </button>

              {/* Headlights */}
              <button
                onClick={() => {
                  const next = !headlights;
                  setHeadlights(next);
                  onSendCommand(next ? 'W' : 'w');
                }}
                className={`py-2.5 px-3 rounded-xl border text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
                  headlights
                    ? 'bg-amber-500/20 text-amber-300 border-amber-500/40 shadow-sm shadow-amber-500/30'
                    : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'
                }`}
              >
                <Lightbulb className={`w-3.5 h-3.5 ${headlights ? 'text-amber-400' : 'text-slate-500'}`} />
                <span>Lights</span>
              </button>

              {/* Aux Relay */}
              <button
                onClick={() => {
                  const next = !auxRelay;
                  setAuxRelay(next);
                  onSendCommand(next ? 'A1' : 'A0');
                }}
                className={`py-2.5 px-3 rounded-xl border text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
                  auxRelay
                    ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                    : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'
                }`}
              >
                <Zap className={`w-3.5 h-3.5 ${auxRelay ? 'text-emerald-400' : 'text-slate-500'}`} />
                <span>Relay</span>
              </button>

              {/* Center Servo */}
              <button
                onClick={() => onSendCommand('SERVO:90')}
                className="py-2.5 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-semibold flex items-center justify-center gap-1.5 active:scale-95 transition-all"
              >
                <Crosshair className="w-3.5 h-3.5 text-sky-400" />
                <span>Servo 90°</span>
              </button>
            </div>

            {/* Quick 20Hz Joystick on right if in DPAD mode */}
            {leftMode === 'DPAD' && (
              <button
                onClick={() => setLeftMode('JOYSTICK')}
                className="w-full mt-2 py-2 rounded-lg bg-sky-600/20 hover:bg-sky-600/30 border border-sky-500/30 text-sky-300 text-xs font-medium flex items-center justify-center gap-1.5"
              >
                <Radio className="w-3.5 h-3.5" />
                <span>Switch to 20Hz Analog Joystick</span>
              </button>
            )}
          </div>
        </div>
      ) : (
        /* PORTRAIT VIEW */
        <div className="flex-1 flex flex-col justify-between max-w-4xl mx-auto w-full">
          {/* Main Driving Controls: D-Pad and Joystick Side-by-Side */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-center justify-center flex-1 my-auto">
            {/* D-Pad Controller */}
            <div className="flex flex-col items-center justify-center p-4 bg-slate-900/60 border border-slate-800 rounded-2xl">
              <div className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-3 flex items-center gap-1.5">
                <Compass className="w-4 h-4 text-sky-400" />
                <span>Hold-to-Move D-Pad (F/B/L/R)</span>
              </div>

              <div className="relative w-48 h-48 flex items-center justify-center">
                <button
                  onMouseDown={() => handleDpadPress('F')}
                  onMouseUp={handleDpadRelease}
                  onTouchStart={(e) => { e.preventDefault(); handleDpadPress('F'); }}
                  onTouchEnd={handleDpadRelease}
                  className={`absolute top-0 w-14 h-14 rounded-xl font-black text-2xl flex items-center justify-center border shadow-md transition-all ${
                    activeDirection === 'F' ? 'bg-sky-500 text-white border-sky-400 shadow-sky-500/50 scale-95' : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'
                  }`}
                >
                  ▲
                </button>
                <button
                  onMouseDown={() => handleDpadPress('L')}
                  onMouseUp={handleDpadRelease}
                  onTouchStart={(e) => { e.preventDefault(); handleDpadPress('L'); }}
                  onTouchEnd={handleDpadRelease}
                  className={`absolute left-0 w-14 h-14 rounded-xl font-black text-2xl flex items-center justify-center border shadow-md transition-all ${
                    activeDirection === 'L' ? 'bg-sky-500 text-white border-sky-400 shadow-sky-500/50 scale-95' : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'
                  }`}
                >
                  ◀
                </button>
                <div className="w-12 h-12 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-center text-xs font-mono font-bold text-slate-400">
                  {activeDirection || '●'}
                </div>
                <button
                  onMouseDown={() => handleDpadPress('R')}
                  onMouseUp={handleDpadRelease}
                  onTouchStart={(e) => { e.preventDefault(); handleDpadPress('R'); }}
                  onTouchEnd={handleDpadRelease}
                  className={`absolute right-0 w-14 h-14 rounded-xl font-black text-2xl flex items-center justify-center border shadow-md transition-all ${
                    activeDirection === 'R' ? 'bg-sky-500 text-white border-sky-400 shadow-sky-500/50 scale-95' : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'
                  }`}
                >
                  ▶
                </button>
                <button
                  onMouseDown={() => handleDpadPress('B')}
                  onMouseUp={handleDpadRelease}
                  onTouchStart={(e) => { e.preventDefault(); handleDpadPress('B'); }}
                  onTouchEnd={handleDpadRelease}
                  className={`absolute bottom-0 w-14 h-14 rounded-xl font-black text-2xl flex items-center justify-center border shadow-md transition-all ${
                    activeDirection === 'B' ? 'bg-sky-500 text-white border-sky-400 shadow-sky-500/50 scale-95' : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border-slate-700'
                  }`}
                >
                  ▼
                </button>
              </div>

              <p className="text-[11px] text-slate-500 mt-2 text-center">
                Release emits failsafe <code>'S'</code>
              </p>
            </div>

            {/* 20Hz Analog Joystick */}
            <div className="flex flex-col items-center justify-center p-4 bg-slate-900/60 border border-slate-800 rounded-2xl">
              <div className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-3 flex items-center gap-1.5">
                <span>20Hz Analog Joystick</span>
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-sky-500/20 text-sky-400 font-mono">
                  X:{joystickPos.x} Y:{joystickPos.y}
                </span>
              </div>

              <div
                ref={joystickRef}
                onPointerDown={onPointerDown}
                onPointerMove={onPointerMove}
                onPointerUp={onPointerUp}
                className="w-48 h-48 rounded-full bg-slate-950 border-2 border-slate-800 shadow-inner flex items-center justify-center cursor-grab active:cursor-grabbing relative touch-none"
              >
                <div className="absolute inset-x-0 top-1/2 h-px bg-slate-800/80 pointer-events-none" />
                <div className="absolute inset-y-0 left-1/2 w-px bg-slate-800/80 pointer-events-none" />
                <div
                  className={`w-14 h-14 rounded-full border-2 shadow-xl flex items-center justify-center transition-transform pointer-events-none ${
                    isDraggingJoystick ? 'bg-sky-500 border-sky-300 shadow-sky-500/50' : 'bg-slate-800 border-slate-600'
                  }`}
                  style={{
                    transform: `translate(${joystickPos.x * 0.7}px, ${-joystickPos.y * 0.7}px)`
                  }}
                >
                  <div className="w-3.5 h-3.5 rounded-full bg-white/40" />
                </div>
              </div>

              <p className="text-[11px] text-slate-500 mt-2 text-center font-mono">
                <code>X:-100..100, Y:-100..100</code>
              </p>
            </div>
          </div>

          {/* Auxiliary Buttons */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 mt-3">
            <button
              onMouseDown={() => onSendCommand('H')}
              onMouseUp={() => onSendCommand('h')}
              className="py-2.5 px-3 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-700 text-slate-200 font-semibold text-xs active:bg-amber-600 active:text-white transition-all shadow-sm flex items-center justify-center gap-1.5"
            >
              <Volume2 className="w-3.5 h-3.5 text-amber-400" />
              <span>Horn (H)</span>
            </button>

            <button
              onClick={() => {
                const next = !headlights;
                setHeadlights(next);
                onSendCommand(next ? 'W' : 'w');
              }}
              className={`py-2.5 px-3 rounded-xl border font-semibold text-xs transition-all shadow-sm flex items-center justify-center gap-1.5 ${
                headlights
                  ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                  : 'bg-slate-900 hover:bg-slate-800 text-slate-200 border-slate-700'
              }`}
            >
              <Lightbulb className={`w-3.5 h-3.5 ${headlights ? 'text-amber-400' : 'text-slate-400'}`} />
              <span>Lights: {headlights ? 'ON' : 'OFF'}</span>
            </button>

            <button
              onClick={() => {
                const next = !auxRelay;
                setAuxRelay(next);
                onSendCommand(next ? 'A1' : 'A0');
              }}
              className={`py-2.5 px-3 rounded-xl border font-semibold text-xs transition-all shadow-sm flex items-center justify-center gap-1.5 ${
                auxRelay
                  ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                  : 'bg-slate-900 hover:bg-slate-800 text-slate-200 border-slate-700'
              }`}
            >
              <Zap className={`w-3.5 h-3.5 ${auxRelay ? 'text-emerald-400' : 'text-slate-400'}`} />
              <span>Relay: {auxRelay ? 'ON' : 'OFF'}</span>
            </button>

            <button
              onClick={() => onSendCommand('SERVO:90')}
              className="py-2.5 px-3 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-700 text-slate-200 font-semibold text-xs active:scale-95 transition-all shadow-sm"
            >
              <span>Center Servo (90°)</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
