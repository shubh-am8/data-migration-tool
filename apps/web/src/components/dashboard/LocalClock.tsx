"use client";

import { useEffect, useState } from "react";

/** Live clock in the browser's local timezone — hh:mm:ss */
export function LocalClock({ className }: { className?: string }) {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  const text = now.toLocaleTimeString(undefined, {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  });

  return (
    <time dateTime={now.toISOString()} className={className} aria-live="off">
      {text}
    </time>
  );
}
