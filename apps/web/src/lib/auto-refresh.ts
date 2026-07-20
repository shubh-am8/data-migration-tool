export const AUTO_REFRESH_OPTIONS_MS = [0, 10_000, 30_000, 60_000, 120_000, 300_000] as const;

export type AutoRefreshMs = (typeof AUTO_REFRESH_OPTIONS_MS)[number];

export const AUTO_REFRESH_STORAGE_KEY = "migration.autoRefreshMs";

const AUTO_REFRESH_LABELS: Record<AutoRefreshMs, string> = {
  0: "Off",
  10_000: "10s",
  30_000: "30s",
  60_000: "1m",
  120_000: "2m",
  300_000: "5m",
};

export function autoRefreshLabel(ms: AutoRefreshMs): string {
  return AUTO_REFRESH_LABELS[ms];
}

export function clampAutoRefreshMs(ms: number): AutoRefreshMs {
  return AUTO_REFRESH_OPTIONS_MS.includes(ms as AutoRefreshMs) ? (ms as AutoRefreshMs) : 0;
}

export function formatLastRefreshed(d: Date, now: Date = new Date()): string {
  const seconds = Math.floor((now.getTime() - d.getTime()) / 1000);
  if (seconds < 1) return "Just now";
  if (seconds < 60) return `${seconds}s ago`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  return d.toLocaleTimeString();
}
