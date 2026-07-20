"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api-client";
import { AppLoader } from "@/components/shared/AppLoader";

export interface AuthUser {
  authenticated: boolean;
  admin?: boolean;
  email?: string;
  name?: string;
  pictureUrl?: string;
  lastLoginAt?: string;
}

const AuthContext = createContext<AuthUser | null>(null);

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<AuthUser | null>(null);

  useEffect(() => {
    apiFetch<AuthUser>("/api/auth/me")
      .then((me) => {
        if (!me.authenticated) router.replace("/login");
        else setUser(me);
      })
      .catch(() => router.replace("/login"));
  }, [router]);

  if (!user) return <AppLoader label="Checking session…" />;
  return <AuthContext.Provider value={user}>{children}</AuthContext.Provider>;
}
