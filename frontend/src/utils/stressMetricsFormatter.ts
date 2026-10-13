export function formatLatency(latencyMs: number): string {
  if (latencyMs < 1) {
    return '< 1 ms';
  }
  if (latencyMs >= 1000) {
    return `${(latencyMs / 1000).toFixed(2)} s`;
  }
  return `${latencyMs.toFixed(1)} ms`;
}

export function formatThroughput(ops: number): string {
  if (ops >= 1_000_000) {
    return `${(ops / 1_000_000).toFixed(1)}M ops/s`;
  }
  if (ops >= 1_000) {
    return `${(ops / 1_000).toFixed(1)}k ops/s`;
  }
  return `${Math.round(ops)} ops/s`;
}

export function formatContention(index: number): string {
  const clamped = Math.min(1, Math.max(0, index));
  return `${(clamped * 100).toFixed(1)}%`;
}

export interface SlaBadgeStatus {
  label: string;
  colorClass: string;
  isMet: boolean;
}

export function getSlaBadgeStatus(slaMet: boolean, locale?: string): SlaBadgeStatus {
  const isEn = locale === 'en';
  if (slaMet) {
    return {
      label: isEn ? 'SLA Met' : 'SLA Atendido',
      colorClass: 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30',
      isMet: true,
    };
  }
  return {
    label: isEn ? 'SLA Violated' : 'SLA Violado',
    colorClass: 'bg-rose-500/10 text-rose-400 border border-rose-500/30',
    isMet: false,
  };
}
