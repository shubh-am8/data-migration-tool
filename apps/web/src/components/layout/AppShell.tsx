"use client";

import { AuthGuard, useAuth } from "@/components/auth/AuthGuard";
import { AppSidebar } from "@/components/layout/AppSidebar";

function ShellInner({ children }: { children: React.ReactNode }) {
  const user = useAuth();
  return (
    <div className="flex min-h-screen flex-col md:flex-row">
      <AppSidebar
        user={user}
        admin={Boolean(user?.admin)}
      />
      <main className="flex flex-1 flex-col gap-6 p-4 md:p-6">{children}</main>
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
