"use client";

import { useEffect, useState } from "react";
import { ClockIcon } from "lucide-react";
import { cn } from "@/lib/utils";

const TIME_ZONE = "Asia/Kolkata";
const TIME_ZONE_LABEL = "Asia/Calcutta (IST)";

/** Live clock card fixed to Asia/Kolkata (labeled Calcutta/IST) — universal top bar chrome. */
export function LocalClock({ className }: { className?: string }) {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  const time = now.toLocaleTimeString("en-IN", {
    timeZone: TIME_ZONE,
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  });
  const date = now.toLocaleDateString("en-IN", {
    timeZone: TIME_ZONE,
    day: "2-digit",
    month: "short",
  });

  return (
    <div
      className={cn(
        "flex items-center gap-2 rounded-lg border-l-4 border-l-sky-600 bg-sky-500/10 px-3 py-1.5",
        className
      )}
    >
      <ClockIcon className="size-4 shrink-0 text-sky-600" aria-hidden />
      <div className="leading-tight">
        <time dateTime={now.toISOString()} aria-live="off" className="block font-mono text-sm font-semibold tabular-nums">
          {date} {time}
        </time>
        <p className="text-[0.65rem] font-medium tracking-wide text-sky-700 dark:text-sky-400">{TIME_ZONE_LABEL}</p>
      </div>
    </div>
  );
}
