"use client";

import { usePathname } from "next/navigation";
import { NAV_ITEMS } from "@/components/layout/AppSidebar";
import { usePageChrome } from "@/components/layout/PageChromeContext";
import { LocalClock } from "@/components/dashboard/LocalClock";
import { AutoRefreshControl, useAutoRefresh } from "@/components/shared/AutoRefreshControl";
import { Progress } from "@/components/ui/progress";
import { cn } from "@/lib/utils";

function fallbackTitle(pathname: string): string {
  const match = [...NAV_ITEMS]
    .filter((item) => pathname.startsWith(item.href))
    .sort((a, b) => b.href.length - a.href.length)[0];
  return match?.label ?? "Migration Tool";
}

/** Universal chrome above every AppShell page: title (from PageChromeContext, else nav fallback), Calcutta clock, auto-refresh. */
export function AppTopBar() {
  const pathname = usePathname();
  const { title, description, action, bumpRefresh } = usePageChrome();
  const { ms, setMs, progress, lastRefreshedAt } = useAutoRefresh(bumpRefresh);

  return (
    <div className="relative flex-none border-b bg-background">
      <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 md:px-6">
        <div className="flex min-w-0 flex-col gap-0.5">
          <h1 className="truncate text-xl font-semibold tracking-tight">{title || fallbackTitle(pathname)}</h1>
          {description && <p className="truncate text-sm text-muted-foreground">{description}</p>}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {action}
          <LocalClock />
          <AutoRefreshControl ms={ms} onChange={setMs} lastRefreshedAt={lastRefreshedAt} />
        </div>
      </div>
      <Progress
        value={ms > 0 ? progress * 100 : 0}
        aria-label="Time until next auto-refresh"
        className={cn(
          "absolute inset-x-0 -bottom-px h-auto gap-0 [&>[data-slot=progress-track]]:h-[3px] [&>[data-slot=progress-track]]:rounded-none [&>[data-slot=progress-track]]:bg-transparent [&_[data-slot=progress-indicator]]:bg-sky-600",
          ms === 0 && "hidden"
        )}
      />
    </div>
  );
}
