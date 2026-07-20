"use client";

import { useEffect, useRef, useState } from "react";
import { cn } from "@/lib/utils";

export function LiveLogTerminal({
  lines,
  className,
}: {
  lines: string[];
  className?: string;
}) {
  const endRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [lines]);

  return (
    <div
      className={cn(
        "max-h-72 overflow-y-auto rounded-lg border bg-zinc-950 p-3 font-mono text-xs text-zinc-100",
        className
      )}
      role="log"
      aria-live="polite"
    >
      {lines.length === 0 ? (
        <p className="text-zinc-500">Waiting for output…</p>
      ) : (
        lines.map((line, i) => (
          <div key={i} className="whitespace-pre-wrap break-all">
            {line}
          </div>
        ))
      )}
      <div ref={endRef} />
    </div>
  );
}

/** Subscribe to SSE job test stream; returns lines + helpers */
export function useSseLog(url: string | null, body: unknown) {
  const [lines, setLines] = useState<string[]>([]);
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
          setLines((l) => [...l, `Error: HTTP ${res.status}`]);
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
              if (json.line) setLines((l) => [...l, json.line!]);
              if (json.status === "passed") setPassed(true);
              if (json.status === "failed" || json.status === "passed") setDone(true);
            } catch {
              setLines((l) => [...l, raw]);
            }
          }
        }
        setDone(true);
      } catch (e) {
        if ((e as Error).name !== "AbortError") {
          setLines((l) => [...l, `Error: ${(e as Error).message}`]);
          setDone(true);
        }
      }
    })();

    return () => ctrl.abort();
  }, [url, body]);

  return { lines, done, passed };
}
