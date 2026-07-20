"use client";

import { useRouter } from "next/navigation";
import { AppShell } from "@/components/layout/AppShell";
import { SetPageChrome } from "@/components/layout/PageChromeContext";
import { JobWizard } from "@/components/jobs/JobWizard";
import { Card, CardContent } from "@/components/ui/card";

export default function NewJobPage() {
  const router = useRouter();
  return (
    <AppShell>
      <SetPageChrome title="Create Job" description="Configure a new data migration job" />
      <Card><CardContent className="pt-6"><JobWizard onComplete={() => router.push("/jobs")} /></CardContent></Card>
    </AppShell>
  );
}
