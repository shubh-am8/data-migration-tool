"use client";

import { AuthGuard, useAuth } from "@/components/auth/AuthGuard";
import { AppSidebar } from "@/components/layout/AppSidebar";
import { shellMainClass, shellRootClass } from "@/lib/shell-layout";

function ShellInner({ children }: { children: React.ReactNode }) {
  const user = useAuth();
  return (
    <div className={shellRootClass}>
      <AppSidebar
        user={user}
        admin={Boolean(user?.admin)}
      />
      <main className={shellMainClass}>{children}</main>
    </div>
  );
}

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <AuthGuard>
      <ShellInner>{children}</ShellInner>
    </AuthGuard>
  );
}
