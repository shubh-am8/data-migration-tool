"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";

export interface PageChromeMeta {
  title?: string;
  description?: string;
  action?: React.ReactNode;
}

interface PageChromeValue extends PageChromeMeta {
  refreshToken: number;
  bumpRefresh: () => void;
  setMeta: (meta: PageChromeMeta) => void;
}

const PageChromeContext = createContext<PageChromeValue | null>(null);

export function PageChromeProvider({ children }: { children: React.ReactNode }) {
  const [meta, setMeta] = useState<PageChromeMeta>({});
  const [refreshToken, setRefreshToken] = useState(0);

  const bumpRefresh = useCallback(() => setRefreshToken((t) => t + 1), []);

  const value = useMemo<PageChromeValue>(
    () => ({ ...meta, refreshToken, bumpRefresh, setMeta }),
    [meta, refreshToken, bumpRefresh]
  );

  return <PageChromeContext.Provider value={value}>{children}</PageChromeContext.Provider>;
}

export function usePageChrome(): PageChromeValue {
  const ctx = useContext(PageChromeContext);
  if (!ctx) throw new Error("usePageChrome must be used within AppShell");
  return ctx;
}

export function useRefreshToken(): number {
  return usePageChrome().refreshToken;
}

/** Sets the AppTopBar title/description/action for the calling page. */
export function useSetPageChrome(meta: PageChromeMeta): void {
  const { setMeta } = usePageChrome();
  const { title, description, action } = meta;
  useEffect(() => {
    setMeta({ title, description, action });
  }, [title, description, action, setMeta]);
}

/**
 * Render as a child of `AppShell` to set the page chrome without splitting the
 * page into a separate inner component — `AppShell` renders its own provider,
 * so `useSetPageChrome` must run from *inside* that subtree, not from the
 * outer page component that returns `<AppShell>`.
 */
export function SetPageChrome(meta: PageChromeMeta): null {
  useSetPageChrome(meta);
  return null;
}
