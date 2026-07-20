"use client";

import { AuthGuard, useAuth } from "@/components/auth/AuthGuard";
import { AppSidebar } from "@/components/layout/AppSidebar";
import { AppTopBar } from "@/components/layout/AppTopBar";
import { PageChromeProvider } from "@/components/layout/PageChromeContext";
import { shellMainClass, shellRootClass } from "@/lib/shell-layout";

function ShellInner({ children }: { children: React.ReactNode }) {
  const user = useAuth();
  return (
    <div className={shellRootClass}>
      <AppSidebar
        user={user}
        admin={Boolean(user?.admin)}
      />
      <div className="flex min-h-0 min-w-0 flex-1 flex-col">
        <AppTopBar />
        <main className={shellMainClass}>{children}</main>
      </div>
    </div>
  );
}

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <AuthGuard>
      <PageChromeProvider>
        <ShellInner>{children}</ShellInner>
      </PageChromeProvider>
    </AuthGuard>
  );
}
