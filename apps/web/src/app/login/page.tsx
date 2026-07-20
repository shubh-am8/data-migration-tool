"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { AppLoader } from "@/components/shared/AppLoader";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { apiFetch, loginWithGoogle } from "@/lib/api-client";
import { postLoginDestination } from "@/lib/login-redirect";

function LoginContent() {
  const router = useRouter();
  const params = useSearchParams();
  const error = params.get("error");
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;
    apiFetch<{ authenticated: boolean }>("/api/auth/me")
      .then((me) => {
        if (cancelled) return;
        const dest = postLoginDestination(me);
        if (dest) router.replace(dest);
        else setReady(true);
      })
      .catch(() => {
        if (!cancelled) setReady(true);
      });
    return () => {
      cancelled = true;
    };
  }, [router]);

  if (!ready) return <AppLoader label="Checking session…" />;

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Data Migration Platform</CardTitle>
          <CardDescription>Sign in with your organization Google account</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {error === "domain" && (
            <p className="text-sm text-destructive">
              Only organization-domain Google accounts are allowed.
            </p>
          )}
          {error === "oauth" && (
            <p className="text-sm text-destructive">
              Google sign-in session expired or was invalid. Please try again.
            </p>
          )}
          {error === "revoked" && (
            <p className="text-sm text-destructive">
              Your account was revoked. Contact an administrator to restore access.
            </p>
          )}
          <Button className="w-full" onClick={loginWithGoogle}>
            Continue with Google
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense>
      <LoginContent />
    </Suspense>
  );
}
