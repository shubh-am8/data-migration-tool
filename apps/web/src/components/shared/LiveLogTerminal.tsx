"use client";

import { useEffect, useRef, useState } from "react";
import { inferLevelFromText, lineClassForLevel, normalizeLogLines, type LogLine } from "@/lib/log-line";
import { cn } from "@/lib/utils";

export function LiveLogTerminal({
  lines,
  className,
  status,
}: {
  lines: LogLine[] | string[];
  className?: string;
  status?: "running" | "passed" | "failed" | "idle";
}) {
  const endRef = useRef<HTMLDivElement>(null);
  const normalized = normalizeLogLines(lines);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [normalized]);

  const statusLabel =
    status === "running"
      ? "Running…"
      : status === "passed"
        ? "Passed"
        : status === "failed"
          ? "Failed"
          : null;

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      {statusLabel ? (
        <div
          className={cn(
            "text-xs font-medium",
            status === "passed" && "text-emerald-600 dark:text-emerald-400",
            status === "failed" && "text-red-600 dark:text-red-400",
            status === "running" && "text-sky-600 dark:text-sky-400"
          )}
        >
          {statusLabel}
        </div>
      ) : null}
      <div
        className="max-h-72 overflow-y-auto rounded-lg border border-zinc-800 bg-zinc-950 p-3 font-mono text-xs"
        role="log"
        aria-live="polite"
      >
        {normalized.length === 0 ? (
          <p className="text-zinc-500">Waiting for output…</p>
        ) : (
          normalized.map((line, i) => (
            <div key={i} className={cn("whitespace-pre-wrap break-all", lineClassForLevel(line.level))}>
              {line.text}
            </div>
          ))
        )}
        <div ref={endRef} />
      </div>
    </div>
  );
}

function inferLevelFromSseLine(text: string): LogLine["level"] {
  return inferLevelFromText(text);
}

/** Subscribe to SSE job test stream; returns colored lines + helpers */
export function useSseLog(url: string | null, body: unknown) {
  const [lines, setLines] = useState<LogLine[]>([]);
  const [done, setDone] = useState(false);
  const [passed, setPassed] = useState(false);

  useEffect(() => {
    if (!url) return;
    setLines([]);
    setDone(false);
    setPassed(false);
    const ctrl = new AbortController();
    const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

    (async () => {
      try {
        const res = await fetch(`${API_URL}${url}`, {
          method: "POST",
          credentials: "include",
          headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
          body: JSON.stringify(body),
          signal: ctrl.signal,
        });
        if (!res.ok || !res.body) {
          setLines((l) => [...l, { text: `Error: HTTP ${res.status}`, level: "error" }]);
          setDone(true);
          return;
        }
        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buf = "";
        while (true) {
          const { value, done: streamDone } = await reader.read();
          if (streamDone) break;
          buf += decoder.decode(value, { stream: true });
          const parts = buf.split("\n\n");
          buf = parts.pop() || "";
          for (const part of parts) {
            const dataLine = part.split("\n").find((l) => l.startsWith("data:"));
            if (!dataLine) continue;
            const raw = dataLine.slice(5).trim();
            try {
              const json = JSON.parse(raw) as { line?: string; status?: string };
              if (json.line) {
                const level =
                  json.status === "failed"
                    ? "error"
                    : json.status === "passed"
                      ? "success"
                      : inferLevelFromSseLine(json.line);
                setLines((l) => [...l, { text: json.line!, level }]);
              }
              if (json.status === "passed") setPassed(true);
              if (json.status === "failed" || json.status === "passed") setDone(true);
            } catch {
              setLines((l) => [...l, { text: raw, level: "info" }]);
            }
          }
        }
        setDone(true);
      } catch (e) {
        if ((e as Error).name !== "AbortError") {
          setLines((l) => [...l, { text: `Error: ${(e as Error).message}`, level: "error" }]);
          setDone(true);
        }
      }
    })();

    return () => ctrl.abort();
  }, [url, body]);

  return { lines, done, passed };
}
