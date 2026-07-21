"use client";

import { useCallback, useEffect, useState } from "react";
import { RefreshCwIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  AUTO_REFRESH_OPTIONS_MS,
  AUTO_REFRESH_STORAGE_KEY,
  autoRefreshLabel,
  clampAutoRefreshMs,
  formatLastRefreshed,
  type AutoRefreshMs,
} from "@/lib/auto-refresh";
import { cn } from "@/lib/utils";

const TICK_MS = 250;

/**
 * Owns the auto-refresh interval preference (persisted to localStorage) and the
 * ticking clock; calls `onTick` each time the chosen interval elapses. Returns
 * elapsed/interval so the parent (AppTopBar) can render its own progress bar.
 */
export function useAutoRefresh(onTick: () => void, enabled = true) {
  const [ms, setMsState] = useState<AutoRefreshMs>(() =>
    typeof window === "undefined"
      ? 0
      : clampAutoRefreshMs(Number(window.localStorage.getItem(AUTO_REFRESH_STORAGE_KEY)))
  );
  const [elapsedMs, setElapsedMs] = useState(0);
  const [lastRefreshedAt, setLastRefreshedAt] = useState<Date | null>(null);

  const setMs = useCallback((next: AutoRefreshMs) => {
    setMsState(next);
    setElapsedMs(0);
    window.localStorage.setItem(AUTO_REFRESH_STORAGE_KEY, String(next));
  }, []);

  useEffect(() => {
    if (!ms || !enabled) return;
    // Plain counter (not React state) so the tick/refresh side effects run once per
    // interval, directly in the timer callback — never inside a setState updater,
    // which React may invoke while rendering an unrelated component.
    let elapsed = 0;
    const id = setInterval(() => {
      elapsed += TICK_MS;
      if (elapsed < ms) {
        setElapsedMs(elapsed);
        return;
      }
      elapsed = 0;
      setElapsedMs(0);
      onTick();
      setLastRefreshedAt(new Date());
    }, TICK_MS);
    return () => clearInterval(id);
  }, [ms, onTick, enabled]);

  return { ms, setMs, progress: ms ? elapsedMs / ms : 0, lastRefreshedAt };
}

interface AutoRefreshControlProps {
  ms: AutoRefreshMs;
  onChange: (ms: AutoRefreshMs) => void;
  lastRefreshedAt: Date | null;
  className?: string;
}

/** DropdownMenu trigger for picking the auto-refresh interval; shows last-refreshed time. */
export function AutoRefreshControl({ ms, onChange, lastRefreshedAt, className }: AutoRefreshControlProps) {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  return (
    <div className={cn("flex items-center gap-2", className)}>
      {lastRefreshedAt && (
        <span className="hidden text-xs text-muted-foreground sm:inline">
          Refreshed {formatLastRefreshed(lastRefreshedAt, now)}
        </span>
      )}
      <DropdownMenu>
        <DropdownMenuTrigger
          render={
            <Button variant="outline" size="sm" className="gap-1.5">
              <RefreshCwIcon className={cn("size-3.5", ms > 0 && "text-sky-600")} />
              {autoRefreshLabel(ms)}
            </Button>
          }
        />
        <DropdownMenuContent align="end" className="w-36">
          <DropdownMenuGroup>
            {AUTO_REFRESH_OPTIONS_MS.map((option) => (
              <DropdownMenuItem
                key={option}
                onClick={() => onChange(option)}
                className={cn(option === ms && "bg-muted font-medium")}
              >
                {autoRefreshLabel(option)}
              </DropdownMenuItem>
            ))}
          </DropdownMenuGroup>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}
