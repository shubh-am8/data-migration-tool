"use client";

import { useSearchParams } from "next/navigation";
import { Suspense } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { loginWithGoogle } from "@/lib/api-client";

function LoginContent() {
  const params = useSearchParams();
  const error = params.get("error");

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
