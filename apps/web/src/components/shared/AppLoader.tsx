"use client";

import { Spinner } from "@/components/ui/spinner";
import { cn } from "@/lib/utils";

interface AppLoaderProps {
  label?: string;
  className?: string;
  /** Full-area overlay over a relative parent */
  overlay?: boolean;
}

export function AppLoader({ label = "Loading…", className, overlay }: AppLoaderProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={cn(
        "flex flex-col items-center justify-center gap-2 text-muted-foreground",
        overlay && "absolute inset-0 z-10 bg-background/60",
        !overlay && "py-12",
        className
      )}
    >
      <Spinner />
      <span className="text-sm">{label}</span>
    </div>
  );
}
