"use client";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";

export default function DocsError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <Alert variant="destructive">
      <AlertTitle>Could not load this page</AlertTitle>
      <AlertDescription>{error.message}</AlertDescription>
      <Button size="sm" variant="outline" onClick={reset}>
        Try again
      </Button>
    </Alert>
  );
}
