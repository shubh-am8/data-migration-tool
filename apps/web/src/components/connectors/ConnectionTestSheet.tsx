"use client";

import { useEffect, useState } from "react";
import { LiveLogTerminal } from "@/components/shared/LiveLogTerminal";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { apiFetch } from "@/lib/api-client";
import { notifyConnectionTestResult, type ConnectionTestResult } from "@/lib/connection-test";
import { linesFromConnectionTest, type LogLine } from "@/lib/log-line";

export function ConnectionTestSheet({
  open,
  onOpenChange,
  connectionId,
  connectionName,
  pluginId,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  connectionId: string | null;
  connectionName: string;
  pluginId: string;
}) {
  const [lines, setLines] = useState<LogLine[]>([]);
  const [running, setRunning] = useState(false);
  const [status, setStatus] = useState<"running" | "passed" | "failed" | "idle">("idle");

  useEffect(() => {
    if (!open || !connectionId) return;
    let cancelled = false;

    async function run() {
      setRunning(true);
      setStatus("running");
      setLines([{ text: `Connector: ${pluginId}`, level: "info" }]);
      try {
        const result = await apiFetch<ConnectionTestResult>(`/api/connections/${connectionId}/test`, {
          method: "POST",
        });
        if (cancelled) return;
        setLines((prev) => [...prev, ...linesFromConnectionTest(result)]);
        setStatus(result.success ? "passed" : "failed");
        notifyConnectionTestResult(result);
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof Error ? e.message : String(e);
        setLines((prev) => [
          ...prev,
          { text: message, level: "error" },
          { text: "✗ Test failed", level: "error" },
        ]);
        setStatus("failed");
        notifyConnectionTestResult({ success: false, message, latencyMs: 0 });
      } finally {
        if (!cancelled) setRunning(false);
      }
    }

    run();
    return () => {
      cancelled = true;
    };
  }, [open, connectionId, pluginId]);

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="flex w-full flex-col gap-4 sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Connection test</SheetTitle>
          <SheetDescription>
            {connectionName} — live output from the connector plugin
          </SheetDescription>
        </SheetHeader>
        <LiveLogTerminal lines={lines} status={running ? "running" : status} className="flex-1" />
        <Button variant="outline" onClick={() => onOpenChange(false)} disabled={running}>
          Close
        </Button>
      </SheetContent>
    </Sheet>
  );
}
