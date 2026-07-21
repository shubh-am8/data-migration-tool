import type { ConnectionTestResult } from "./connection-test";

export type LogLevel = "info" | "success" | "warn" | "error";

export type LogLine = { text: string; level: LogLevel };

export function normalizeLogLines(lines: LogLine[] | string[]): LogLine[] {
  return lines.map((line) =>
    typeof line === "string" ? { text: line, level: "info" as const } : line
  );
}

export function lineClassForLevel(level: LogLevel): string {
  switch (level) {
    case "success":
      return "text-emerald-400";
    case "error":
      return "text-red-400";
    case "warn":
      return "text-amber-400";
    default:
      return "text-zinc-300";
  }
}

export function inferLevelFromText(text: string): LogLevel {
  const lower = text.toLowerCase();
  if (lower.includes("error") || lower.includes("failed") || lower.includes("fail")) return "error";
  if (lower.includes("warn")) return "warn";
  if (lower.includes("passed") || lower.includes("success") || lower.includes("ok")) return "success";
  return "info";
}

export function linesFromConnectionTest(result: ConnectionTestResult): LogLine[] {
  const lines: LogLine[] = [
    { text: `Testing connection…`, level: "info" },
    { text: `Latency: ${result.latencyMs}ms`, level: "info" },
  ];
  const messageLines = result.message.split(/\r?\n/).filter(Boolean);
  if (messageLines.length === 0) {
    lines.push({
      text: result.success ? "Connection OK" : "Connection failed",
      level: result.success ? "success" : "error",
    });
  } else {
    for (const text of messageLines) {
      lines.push({
        text,
        level: result.success ? inferLevelFromText(text) : "error",
      });
    }
  }
  lines.push({
    text: result.success ? "✓ Test passed" : "✗ Test failed",
    level: result.success ? "success" : "error",
  });
  return lines;
}
