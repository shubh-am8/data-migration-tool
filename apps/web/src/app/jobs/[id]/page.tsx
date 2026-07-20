"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { AppShell } from "@/components/layout/AppShell";
import { SetPageChrome } from "@/components/layout/PageChromeContext";
import { JobProgressCard } from "@/components/jobs/JobProgressCard";
import { Button } from "@/components/ui/button";
import { apiFetch } from "@/lib/api-client";
import { toast } from "sonner";

export default function JobDetailPage() {
  const params = useParams();
  const id = params.id as string;
  const [status, setStatus] = useState<Record<string, unknown> | null>(null);

  useEffect(() => {
    apiFetch<Record<string, unknown>>(`/api/jobs/${id}/status`).then(setStatus).catch(console.error);
    const t = setInterval(() => {
      apiFetch<Record<string, unknown>>(`/api/jobs/${id}/status`).then(setStatus).catch(console.error);
    }, 5000);
    return () => clearInterval(t);
  }, [id]);

  async function cmd(action: string) {
    await apiFetch(`/api/jobs/${id}/${action}`, { method: "POST" });
    toast.success(`Job ${action}`);
    setStatus(await apiFetch(`/api/jobs/${id}/status`));
  }

  return (
    <AppShell>
      <SetPageChrome
        title={(status?.name as string) || "Job"}
        description={`Status: ${status?.status || "—"}`}
        action={
          <div className="flex gap-2">
            <Button onClick={() => cmd("start")}>Start</Button>
            <Button variant="outline" onClick={() => cmd("pause")}>Pause</Button>
            <Button variant="outline" onClick={() => cmd("resume")}>Resume</Button>
            <Button variant="destructive" onClick={() => cmd("cancel")}>Cancel</Button>
          </div>
        }
      />
      {status && <JobProgressCard status={status} />}
    </AppShell>
  );
}
