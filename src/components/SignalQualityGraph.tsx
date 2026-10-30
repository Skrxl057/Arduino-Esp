import React, { useEffect, useRef, useState, useMemo } from 'react';
import * as d3 from 'd3';
import { DeviceSlotState, SignalQualitySample } from '../types';
import {
  Activity,
  Radio,
  Clock,
  Wifi,
  Zap,
  Filter,
  Eye,
  EyeOff,
  Pause,
  Play,
  RotateCcw,
  Sparkles,
  Layers
} from 'lucide-react';

interface SignalQualityGraphProps {
  samples: SignalQualitySample[];
  slots: DeviceSlotState[];
  selectedSlot: number;
  onSendPing?: (slotIndex: number) => void;
  onSelectSlot?: (slotIndex: number) => void;
}

type GraphMode = 'combined' | 'rssi' | 'latency';

export const SignalQualityGraph: React.FC<SignalQualityGraphProps> = ({
  samples,
  slots,
  selectedSlot,
  onSendPing,
  onSelectSlot
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const svgRef = useRef<SVGSVGElement>(null);

  const [mode, setMode] = useState<GraphMode>('combined');
  const [timeWindowSec, setTimeWindowSec] = useState<number>(60); // 30s, 60s, 120s
  const [isPaused, setIsPaused] = useState<boolean>(false);
  const [visibleSlots, setVisibleSlots] = useState<Record<number, boolean>>({
    0: true,
    1: true,
    2: true
  });

  // Hover state for interactive inspection
  const [hoverData, setHoverData] = useState<{
    x: number;
    y: number;
    timestamp: number;
    slotData: { slot: DeviceSlotState; sample: SignalQualitySample | null }[];
  } | null>(null);

  const activeSlot = slots[selectedSlot] || slots[0];

  // Latest samples per slot
  const latestBySlot = useMemo(() => {
    const map: Record<number, SignalQualitySample | undefined> = {};
    for (const s of samples) {
      if (!map[s.slotIndex] || s.timestamp > map[s.slotIndex]!.timestamp) {
        map[s.slotIndex] = s;
      }
    }
    return map;
  }, [samples]);

  // Statistics for selected slot in current window
  const activeSlotStats = useMemo(() => {
    const now = Date.now();
    const cutoff = now - timeWindowSec * 1000;
    const slotSamples = samples.filter(
      (s) => s.slotIndex === selectedSlot && s.timestamp >= cutoff
    );

    if (slotSamples.length === 0) {
      return {
        currentRssi: activeSlot.rssi ?? -65,
        currentLatency: activeSlot.lastLatencyMs || 15,
        minLatency: 0,
        maxLatency: 0,
        avgLatency: 0,
        jitter: 0,
        qualityScore: activeSlot.isConnected ? 95 : 0
      };
    }

    const latencies = slotSamples.map((s) => s.latencyMs);
    const minL = Math.min(...latencies);
    const maxL = Math.max(...latencies);
    const avgL = +(latencies.reduce((a, b) => a + b, 0) / latencies.length).toFixed(1);

    // Calculate jitter as average absolute difference between consecutive latency samples
    let jitterSum = 0;
    for (let i = 1; i < latencies.length; i++) {
      jitterSum += Math.abs(latencies[i] - latencies[i - 1]);
    }
    const jitter = latencies.length > 1 ? +(jitterSum / (latencies.length - 1)).toFixed(1) : 1.2;

    const latest = slotSamples[slotSamples.length - 1];
    // Quality score 0-100 based on RSSI (-100 to -30) and latency
    const rssiFactor = Math.max(0, Math.min(100, ((latest.rssi + 100) / 70) * 100));
    const latencyPenalty = Math.max(0, Math.min(40, (latest.latencyMs / 100) * 40));
    const qualityScore = Math.round(Math.max(10, Math.min(100, rssiFactor * 0.8 + (40 - latencyPenalty))));

    return {
      currentRssi: latest.rssi,
      currentLatency: latest.latencyMs,
      minLatency: minL,
      maxLatency: maxL,
      avgLatency: avgL,
      jitter,
      qualityScore
    };
  }, [samples, selectedSlot, timeWindowSec, activeSlot]);

  // Helper for RSSI signal strength level
  const getSignalStrengthMeta = (rssi: number) => {
    if (rssi >= -60) return { label: 'Excellent', color: 'text-emerald-400', bg: 'bg-emerald-500/20', bars: 4 };
    if (rssi >= -75) return { label: 'Good', color: 'text-sky-400', bg: 'bg-sky-500/20', bars: 3 };
    if (rssi >= -85) return { label: 'Fair', color: 'text-amber-400', bg: 'bg-amber-500/20', bars: 2 };
    return { label: 'Weak', color: 'text-rose-400', bg: 'bg-rose-500/20', bars: 1 };
  };

  const signalMeta = getSignalStrengthMeta(activeSlotStats.currentRssi);

  // Toggle slot visibility
  const toggleSlotVisibility = (slotIdx: number) => {
    setVisibleSlots((prev) => ({
      ...prev,
      [slotIdx]: !prev[slotIdx]
    }));
  };

  // D3 Rendering Hook
  useEffect(() => {
    if (!svgRef.current || !containerRef.current) return;

    const svg = d3.select(svgRef.current);
    const container = containerRef.current;
    const width = container.clientWidth || 700;
    const height = Math.max(260, container.clientHeight || 280);

    const margin = {
      top: 24,
      right: mode === 'rssi' ? 24 : 52,
      bottom: 28,
      left: mode === 'latency' ? 36 : 56
    };

    const innerWidth = width - margin.left - margin.right;
    const innerHeight = height - margin.top - margin.bottom;

    svg.attr('width', width).attr('height', height);
    svg.selectAll('*').remove();

    if (innerWidth <= 0 || innerHeight <= 0) return;

    // Defs for gradients & clip-path
    const defs = svg.append('defs');

    // Clip path so lines don't draw over axes
    defs
      .append('clipPath')
      .attr('id', 'chart-clip')
      .append('rect')
      .attr('x', 0)
      .attr('y', 0)
      .attr('width', innerWidth)
      .attr('height', innerHeight);

    // Slot gradients
    slots.forEach((slot) => {
      const grad = defs
        .append('linearGradient')
        .attr('id', `slot-gradient-${slot.slotIndex}`)
        .attr('x1', '0%')
        .attr('y1', '0%')
        .attr('x2', '0%')
        .attr('y2', '100%');

      grad
        .append('stop')
        .attr('offset', '0%')
        .attr('stop-color', slot.colorHex)
        .attr('stop-opacity', 0.25);

      grad
        .append('stop')
        .attr('offset', '100%')
        .attr('stop-color', slot.colorHex)
        .attr('stop-opacity', 0.0);
    });

    const g = svg
      .append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);

    const now = Date.now();
    const timeStart = now - timeWindowSec * 1000;
    const timeEnd = now;

    // X Scale: Time
    const xScale = d3.scaleTime().domain([new Date(timeStart), new Date(timeEnd)]).range([0, innerWidth]);

    // Y Scale Left: RSSI in dBm (-100 to -30)
    const yScaleRssi = d3.scaleLinear().domain([-100, -30]).range([innerHeight, 0]);

    // Y Scale Right: Latency in ms (0 to dynamic max, min 100)
    const currentWindowSamples = samples.filter((s) => s.timestamp >= timeStart);
    const maxObservedLatency = d3.max(currentWindowSamples, (d) => d.latencyMs) || 50;
    const latencyDomainMax = Math.max(100, Math.ceil((maxObservedLatency * 1.25) / 25) * 25);
    const yScaleLatency = d3.scaleLinear().domain([0, latencyDomainMax]).range([innerHeight, 0]);

    // Draw background RF Quality Bands when RSSI is displayed
    if (mode === 'rssi' || mode === 'combined') {
      const bands = [
        { from: -30, to: -60, color: 'rgba(16, 185, 129, 0.04)', label: 'EXCELLENT' },
        { from: -60, to: -75, color: 'rgba(14, 165, 233, 0.03)', label: 'GOOD' },
        { from: -75, to: -85, color: 'rgba(245, 158, 11, 0.03)', label: 'FAIR' },
        { from: -85, to: -100, color: 'rgba(239, 68, 68, 0.04)', label: 'POOR' }
      ];

      bands.forEach((b) => {
        const yTop = yScaleRssi(b.from);
        const yBottom = yScaleRssi(b.to);
        const bHeight = Math.max(0, yBottom - yTop);

        g.append('rect')
          .attr('x', 0)
          .attr('y', yTop)
          .attr('width', innerWidth)
          .attr('height', bHeight)
          .attr('fill', b.color);

        // Subtle band label
        g.append('text')
          .attr('x', innerWidth - 8)
          .attr('y', yTop + 14)
          .attr('text-anchor', 'end')
          .attr('fill', '#64748b')
          .attr('font-size', '9px')
          .attr('font-family', 'ui-monospace, monospace')
          .attr('letter-spacing', '0.08em')
          .attr('opacity', 0.6)
          .text(b.label);
      });
    }

    // Gridlines (Horizontal)
    if (mode === 'rssi' || mode === 'combined') {
      const rssiTicks = [-90, -75, -60, -45];
      rssiTicks.forEach((tickVal) => {
        const y = yScaleRssi(tickVal);
        g.append('line')
          .attr('x1', 0)
          .attr('x2', innerWidth)
          .attr('y1', y)
          .attr('y2', y)
          .attr('stroke', '#334155')
          .attr('stroke-dasharray', '2,4')
          .attr('stroke-width', 0.7)
          .attr('opacity', 0.4);
      });
    }

    // Chart content group with clipping
    const chartContent = g.append('g').attr('clip-path', 'url(#chart-clip)');

    // Line & Area generators
    const rssiLineGenerator = d3
      .line<SignalQualitySample>()
      .x((d) => xScale(new Date(d.timestamp)))
      .y((d) => yScaleRssi(d.rssi))
      .curve(d3.curveMonotoneX);

    const rssiAreaGenerator = d3
      .area<SignalQualitySample>()
      .x((d) => xScale(new Date(d.timestamp)))
      .y0(innerHeight)
      .y1((d) => yScaleRssi(d.rssi))
      .curve(d3.curveMonotoneX);

    const latencyLineGenerator = d3
      .line<SignalQualitySample>()
      .x((d) => xScale(new Date(d.timestamp)))
      .y((d) => yScaleLatency(d.latencyMs))
      .curve(d3.curveMonotoneX);

    // Draw lines and areas per slot
    slots.forEach((slot) => {
      const isVisible = visibleSlots[slot.slotIndex];
      if (!isVisible) return;

      const slotSamples = samples
        .filter((s) => s.slotIndex === slot.slotIndex && s.timestamp >= timeStart - 2000)
        .sort((a, b) => a.timestamp - b.timestamp);

      if (slotSamples.length < 2) return;

      const isFocused = slot.slotIndex === selectedSlot;
      const strokeWidth = isFocused ? 2.2 : 1.5;
      const opacity = isFocused ? 1 : 0.65;

      // Draw RSSI Area & Line
      if (mode === 'rssi' || mode === 'combined') {
        chartContent
          .append('path')
          .datum(slotSamples)
          .attr('d', rssiAreaGenerator)
          .attr('fill', `url(#slot-gradient-${slot.slotIndex})`)
          .attr('opacity', isFocused ? 0.9 : 0.4);

        chartContent
          .append('path')
          .datum(slotSamples)
          .attr('d', rssiLineGenerator)
          .attr('fill', 'none')
          .attr('stroke', slot.colorHex)
          .attr('stroke-width', strokeWidth)
          .attr('stroke-linejoin', 'round')
          .attr('stroke-linecap', 'round')
          .attr('opacity', opacity);
      }

      // Draw Latency Line (dashed if in combined mode, solid in latency mode)
      if (mode === 'latency' || mode === 'combined') {
        chartContent
          .append('path')
          .datum(slotSamples)
          .attr('d', latencyLineGenerator)
          .attr('fill', 'none')
          .attr('stroke', mode === 'combined' ? slot.colorHex : slot.colorHex)
          .attr('stroke-width', mode === 'combined' ? 1.5 : strokeWidth)
          .attr('stroke-dasharray', mode === 'combined' ? '3,3' : 'none')
          .attr('stroke-linejoin', 'round')
          .attr('stroke-linecap', 'round')
          .attr('opacity', mode === 'combined' ? (isFocused ? 0.85 : 0.5) : opacity);
      }

      // Live pulsating head marker at latest point
      const lastPoint = slotSamples[slotSamples.length - 1];
      if (lastPoint && slot.isConnected) {
        const lastX = xScale(new Date(lastPoint.timestamp));
        const lastY = mode === 'latency' ? yScaleLatency(lastPoint.latencyMs) : yScaleRssi(lastPoint.rssi);

        // Pulsing glow ring
        chartContent
          .append('circle')
          .attr('cx', lastX)
          .attr('cy', lastY)
          .attr('r', 5)
          .attr('fill', slot.colorHex)
          .attr('opacity', 0.3)
          .append('animate')
          .attr('attributeName', 'r')
          .attr('values', '4;8;4')
          .attr('dur', '1.8s')
          .attr('repeatCount', 'indefinite');

        // Solid center dot
        chartContent
          .append('circle')
          .attr('cx', lastX)
          .attr('cy', lastY)
          .attr('r', 3.5)
          .attr('fill', '#ffffff')
          .attr('stroke', slot.colorHex)
          .attr('stroke-width', 2);
      }
    });

    // Draw Latency Threshold Markers (50ms and 100ms) if latency is active
    if (mode === 'latency' || mode === 'combined') {
      [50, 100].forEach((thr) => {
        if (thr < latencyDomainMax) {
          const y = yScaleLatency(thr);
          g.append('line')
            .attr('x1', 0)
            .attr('x2', innerWidth)
            .attr('y1', y)
            .attr('y2', y)
            .attr('stroke', thr === 100 ? '#f43f5e' : '#f59e0b')
            .attr('stroke-dasharray', '1,4')
            .attr('stroke-width', 1)
            .attr('opacity', 0.5);

          if (mode === 'latency') {
            g.append('text')
              .attr('x', 6)
              .attr('y', y - 4)
              .attr('fill', thr === 100 ? '#fb7185' : '#fbbf24')
              .attr('font-size', '9px')
              .attr('font-mono', 'true')
              .text(`${thr}ms Threshold`);
          }
        }
      });
    }

    // X Axis (Time)
    const xAxis = d3
      .axisBottom<Date>(xScale)
      .ticks(Math.max(3, Math.floor(innerWidth / 110)))
      .tickFormat((d) => {
        const date = d as Date;
        return d3.timeFormat('%H:%M:%S')(date);
      });

    const xAxisGroup = g
      .append('g')
      .attr('transform', `translate(0,${innerHeight})`)
      .call(xAxis);

    xAxisGroup.select('.domain').attr('stroke', '#334155');
    xAxisGroup.selectAll('.tick line').attr('stroke', '#334155');
    xAxisGroup
      .selectAll('.tick text')
      .attr('fill', '#94a3b8')
      .attr('font-size', '10px')
      .attr('font-family', 'ui-monospace, monospace');

    // Left Y Axis (RSSI dBm)
    if (mode === 'rssi' || mode === 'combined') {
      const yAxisLeft = d3
        .axisLeft(yScaleRssi)
        .tickValues([-90, -75, -60, -45, -30])
        .tickFormat((d) => `${d} dBm`);

      const yLeftGroup = g.append('g').call(yAxisLeft);
      yLeftGroup.select('.domain').attr('stroke', '#334155');
      yLeftGroup.selectAll('.tick line').attr('stroke', '#334155');
      yLeftGroup
        .selectAll('.tick text')
        .attr('fill', '#94a3b8')
        .attr('font-size', '9px')
        .attr('font-family', 'ui-monospace, monospace');

      // Axis Title
      g.append('text')
        .attr('transform', 'rotate(-90)')
        .attr('x', -innerHeight / 2)
        .attr('y', -42)
        .attr('text-anchor', 'middle')
        .attr('fill', '#94a3b8')
        .attr('font-size', '10px')
        .attr('font-weight', '600')
        .text('Signal RSSI (dBm)');
    }

    // Right Y Axis (Latency ms)
    if (mode === 'latency' || mode === 'combined') {
      const yAxisRight = d3
        .axisRight(yScaleLatency)
        .ticks(4)
        .tickFormat((d) => `${d} ms`);

      const yRightGroup = g
        .append('g')
        .attr('transform', `translate(${innerWidth},0)`)
        .call(yAxisRight);

      yRightGroup.select('.domain').attr('stroke', '#334155');
      yRightGroup.selectAll('.tick line').attr('stroke', '#334155');
      yRightGroup
        .selectAll('.tick text')
        .attr('fill', '#94a3b8')
        .attr('font-size', '9px')
        .attr('font-family', 'ui-monospace, monospace');

      // Axis Title
      g.append('text')
        .attr('transform', 'rotate(90)')
        .attr('x', innerHeight / 2)
        .attr('y', -innerWidth - 38)
        .attr('text-anchor', 'middle')
        .attr('fill', '#94a3b8')
        .attr('font-size', '10px')
        .attr('font-weight', '600')
        .text('Latency (ms)');
    }

    // Invisible Overlay for Interactive Crosshair & Tooltip
    const bisectDate = d3.bisector((d: SignalQualitySample) => d.timestamp).center;

    const overlay = g
      .append('rect')
      .attr('width', innerWidth)
      .attr('height', innerHeight)
      .attr('fill', 'transparent')
      .attr('cursor', 'crosshair');

    // Crosshair line
    const crosshair = g
      .append('line')
      .attr('y1', 0)
      .attr('y2', innerHeight)
      .attr('stroke', '#94a3b8')
      .attr('stroke-width', 1)
      .attr('stroke-dasharray', '3,3')
      .attr('opacity', 0);

    overlay
      .on('mousemove', (event: MouseEvent) => {
        const [pointerX, pointerY] = d3.pointer(event);
        const hoveredDate = xScale.invert(pointerX);
        const hoveredTimestamp = hoveredDate.getTime();

        crosshair
          .attr('x1', pointerX)
          .attr('x2', pointerX)
          .attr('opacity', 0.8);

        // Find nearest sample for each active slot
        const slotResults: { slot: DeviceSlotState; sample: SignalQualitySample | null }[] = [];

        slots.forEach((s) => {
          if (!visibleSlots[s.slotIndex]) return;
          const slotSamples = samples.filter((sp) => sp.slotIndex === s.slotIndex);
          if (slotSamples.length === 0) {
            slotResults.push({ slot: s, sample: null });
            return;
          }

          const idx = bisectDate(slotSamples, hoveredTimestamp);
          const sample = slotSamples[Math.max(0, Math.min(slotSamples.length - 1, idx))];
          if (sample && Math.abs(sample.timestamp - hoveredTimestamp) < 5000) {
            slotResults.push({ slot: s, sample });
          } else {
            slotResults.push({ slot: s, sample: null });
          }
        });

        setHoverData({
          x: pointerX + margin.left,
          y: pointerY + margin.top,
          timestamp: hoveredTimestamp,
          slotData: slotResults
        });
      })
      .on('mouseleave', () => {
        crosshair.attr('opacity', 0);
        setHoverData(null);
      });
  }, [samples, slots, selectedSlot, mode, timeWindowSec, visibleSlots, isPaused]);

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 md:p-5 flex flex-col gap-4 shadow-xl">
      {/* Top Header & View Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800/80">
        <div>
          <div className="flex items-center gap-2">
            <Radio className="w-5 h-5 text-sky-400 animate-pulse" />
            <h3 className="text-base font-bold text-white tracking-wide">
              Real-Time Signal Quality & Latency Telemetry
            </h3>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-sky-500/10 text-sky-400 border border-sky-500/20">
              D3 Engine
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Real-time RF RSSI (dBm) and round-trip ping latency across active device slots
          </p>
        </div>

        {/* Action Controls & Mode Switcher */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Mode switch */}
          <div className="flex items-center bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs">
            <button
              onClick={() => setMode('combined')}
              className={`px-2.5 py-1 rounded font-medium transition-all ${
                mode === 'combined'
                  ? 'bg-sky-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Combined
            </button>
            <button
              onClick={() => setMode('rssi')}
              className={`px-2.5 py-1 rounded font-medium transition-all ${
                mode === 'rssi'
                  ? 'bg-emerald-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              RSSI
            </button>
            <button
              onClick={() => setMode('latency')}
              className={`px-2.5 py-1 rounded font-medium transition-all ${
                mode === 'latency'
                  ? 'bg-amber-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Latency
            </button>
          </div>

          {/* Time window selector */}
          <div className="flex items-center bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs text-slate-300">
            <Clock className="w-3.5 h-3.5 text-slate-400 ml-1.5 mr-1" />
            {[30, 60, 120].map((sec) => (
              <button
                key={sec}
                onClick={() => setTimeWindowSec(sec)}
                className={`px-2 py-0.5 rounded font-mono text-[11px] transition-all ${
                  timeWindowSec === sec
                    ? 'bg-slate-800 text-sky-400 font-bold'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {sec}s
              </button>
            ))}
          </div>

          {/* Pause / Resume */}
          <button
            onClick={() => setIsPaused((prev) => !prev)}
            title={isPaused ? 'Resume live graph' : 'Pause graph'}
            className="p-1.5 bg-slate-950 hover:bg-slate-800 border border-slate-800 rounded-lg text-slate-300 transition-colors"
          >
            {isPaused ? <Play className="w-4 h-4 text-emerald-400" /> : <Pause className="w-4 h-4 text-amber-400" />}
          </button>

          {/* Test Ping Trigger */}
          {onSendPing && (
            <button
              onClick={() => onSendPing(selectedSlot)}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-sky-600 hover:bg-sky-500 active:scale-95 text-white text-xs font-semibold rounded-lg shadow-sm shadow-sky-600/30 transition-all"
            >
              <Zap className="w-3.5 h-3.5 fill-current" />
              <span>Send Ping</span>
            </button>
          )}
        </div>
      </div>

      {/* KPI Cards for Active Selected Slot */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 text-xs">
        {/* RSSI Signal */}
        <div className="p-3 bg-slate-950/70 border border-slate-800/80 rounded-xl flex flex-col justify-between">
          <div className="flex items-center justify-between text-slate-400">
            <span className="font-medium">Signal Strength</span>
            <div className="flex items-center gap-0.5">
              {[1, 2, 3, 4].map((bar) => (
                <div
                  key={bar}
                  className={`w-1 rounded-xs transition-all ${
                    bar <= signalMeta.bars ? 'bg-emerald-400' : 'bg-slate-700'
                  }`}
                  style={{ height: `${bar * 3.5}px` }}
                />
              ))}
            </div>
          </div>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-xl font-bold font-mono text-white">
              {activeSlotStats.currentRssi}{' '}
              <span className="text-xs font-normal text-slate-400">dBm</span>
            </span>
            <span
              className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${signalMeta.bg} ${signalMeta.color}`}
            >
              {signalMeta.label}
            </span>
          </div>
        </div>

        {/* Latency */}
        <div className="p-3 bg-slate-950/70 border border-slate-800/80 rounded-xl flex flex-col justify-between">
          <div className="flex items-center justify-between text-slate-400">
            <span className="font-medium">Round-Trip Latency</span>
            <Activity className="w-3.5 h-3.5 text-sky-400" />
          </div>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-xl font-bold font-mono text-sky-400">
              {activeSlotStats.currentLatency}{' '}
              <span className="text-xs font-normal text-slate-400">ms</span>
            </span>
            <span className="text-[10px] font-mono text-slate-400">
              avg: {activeSlotStats.avgLatency}ms
            </span>
          </div>
        </div>

        {/* Jitter */}
        <div className="p-3 bg-slate-950/70 border border-slate-800/80 rounded-xl flex flex-col justify-between">
          <div className="flex items-center justify-between text-slate-400">
            <span className="font-medium">Jitter Variance</span>
            <span className="text-[10px] text-slate-500 font-mono">min/max</span>
          </div>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-xl font-bold font-mono text-amber-400">
              ±{activeSlotStats.jitter}{' '}
              <span className="text-xs font-normal text-slate-400">ms</span>
            </span>
            <span className="text-[10px] font-mono text-slate-400">
              {activeSlotStats.minLatency}-{activeSlotStats.maxLatency}ms
            </span>
          </div>
        </div>

        {/* Link Quality Score */}
        <div className="p-3 bg-slate-950/70 border border-slate-800/80 rounded-xl flex flex-col justify-between">
          <div className="flex items-center justify-between text-slate-400">
            <span className="font-medium">Link Reliability</span>
            <Sparkles className="w-3.5 h-3.5 text-purple-400" />
          </div>
          <div className="mt-2 flex items-baseline justify-between">
            <span className="text-xl font-bold font-mono text-emerald-400">
              {activeSlotStats.qualityScore}%
            </span>
            <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 font-semibold">
              {activeSlot.isConnected ? 'Synchronized' : 'Standby'}
            </span>
          </div>
        </div>
      </div>

      {/* Slot Visibility & Legend Toggles */}
      <div className="flex flex-wrap items-center justify-between gap-2 px-1">
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-400 font-medium flex items-center gap-1">
            <Layers className="w-3.5 h-3.5 text-slate-400" />
            <span>Active Slots:</span>
          </span>
          {slots.map((slot) => {
            const isVisible = visibleSlots[slot.slotIndex];
            const isSelected = slot.slotIndex === selectedSlot;
            const latest = latestBySlot[slot.slotIndex];
            return (
              <button
                key={slot.slotIndex}
                onClick={() => {
                  toggleSlotVisibility(slot.slotIndex);
                  onSelectSlot?.(slot.slotIndex);
                }}
                className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg border text-xs transition-all ${
                  isSelected
                    ? 'border-slate-600 bg-slate-800 font-semibold'
                    : 'border-slate-800/80 bg-slate-950/50 hover:bg-slate-800/50'
                }`}
                style={{
                  color: isVisible ? slot.colorHex : '#64748b'
                }}
              >
                <div
                  className="w-2.5 h-2.5 rounded-full"
                  style={{
                    backgroundColor: isVisible ? slot.colorHex : '#475569',
                    boxShadow: isVisible && slot.isConnected ? `0 0 8px ${slot.colorHex}` : 'none'
                  }}
                />
                <span className="text-slate-200">
                  Slot {slot.slotIndex + 1}: {slot.name.split(' ')[0]}
                </span>
                {latest && isVisible && (
                  <span className="font-mono text-[10px] text-slate-400 ml-1">
                    {mode === 'latency' ? `${latest.latencyMs}ms` : `${latest.rssi}dBm`}
                  </span>
                )}
                {isVisible ? (
                  <Eye className="w-3 h-3 text-slate-400 ml-0.5" />
                ) : (
                  <EyeOff className="w-3 h-3 text-slate-600 ml-0.5" />
                )}
              </button>
            );
          })}
        </div>

        <div className="flex items-center gap-3 text-[11px] text-slate-400 font-mono">
          <span className="flex items-center gap-1">
            <span className="w-3 h-0.5 bg-sky-400 inline-block rounded" /> RSSI (dBm, solid)
          </span>
          <span className="flex items-center gap-1">
            <span className="w-3 h-0.5 border-t border-dashed border-sky-400 inline-block" /> Latency (ms, dashed)
          </span>
        </div>
      </div>

      {/* D3 Graph Canvas Container */}
      <div
        ref={containerRef}
        className="w-full h-64 sm:h-72 bg-slate-950 rounded-xl border border-slate-800/90 relative overflow-hidden"
      >
        <svg ref={svgRef} className="w-full h-full block select-none" />

        {/* Interactive Floating Hover Tooltip */}
        {hoverData && (
          <div
            className="absolute z-20 pointer-events-none bg-slate-900/95 backdrop-blur-md border border-slate-700/80 rounded-lg p-2.5 shadow-2xl text-xs space-y-1.5 transition-transform duration-75"
            style={{
              left: `${Math.min(hoverData.x + 12, (containerRef.current?.clientWidth || 700) - 180)}px`,
              top: `${Math.max(10, Math.min(hoverData.y - 40, (containerRef.current?.clientHeight || 280) - 130))}px`
            }}
          >
            <div className="text-[10px] font-mono text-slate-400 border-b border-slate-800 pb-1">
              Time: {new Date(hoverData.timestamp).toLocaleTimeString()}.
              {String(hoverData.timestamp % 1000).padStart(3, '0')}
            </div>
            {hoverData.slotData.map(({ slot, sample }) => {
              if (!sample) return null;
              return (
                <div key={slot.slotIndex} className="flex flex-col gap-0.5">
                  <div className="flex items-center gap-1.5 font-bold" style={{ color: slot.colorHex }}>
                    <div className="w-2 h-2 rounded-full" style={{ backgroundColor: slot.colorHex }} />
                    <span>Slot {slot.slotIndex + 1} ({slot.name.split(' ')[0]})</span>
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-[11px] font-mono text-slate-300 pl-3.5">
                    <div>
                      RSSI: <span className="text-white font-semibold">{sample.rssi} dBm</span>
                    </div>
                    <div>
                      RTT: <span className="text-sky-400 font-semibold">{sample.latencyMs} ms</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
