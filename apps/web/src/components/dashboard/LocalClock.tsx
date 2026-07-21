"use client";

import { useEffect, useState } from "react";
import { ClockIcon } from "lucide-react";
import { cn } from "@/lib/utils";

const TIME_ZONE = "Asia/Kolkata";
const TIME_ZONE_LABEL = "Asia/Calcutta (IST)";

/** Live clock fixed to Asia/Kolkata — universal top bar chrome. */
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
    year: "numeric",
  });

  return (
    <div
      className={cn(
        "flex items-center gap-2 rounded-lg border bg-muted/50 px-3 py-1.5",
        className
      )}
    >
      <ClockIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
      <div className="min-w-0 leading-tight">
        <time
          dateTime={now.toISOString()}
          aria-live="off"
          className="block truncate font-mono text-sm font-semibold tabular-nums"
        >
          {date} · {time}
        </time>
        <p className="truncate text-[0.65rem] font-medium text-muted-foreground">{TIME_ZONE_LABEL}</p>
      </div>
    </div>
  );
}
