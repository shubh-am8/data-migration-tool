"use client";

import { useEffect, useState } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { StatCard } from "@/components/dashboard/StatCard";
import { LocalClock } from "@/components/dashboard/LocalClock";
import { PageHeader } from "@/components/shared/PageHeader";
import { AppLoader } from "@/components/shared/AppLoader";
import { apiFetch } from "@/lib/api-client";
import { notify } from "@/lib/notify";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";
import { Line, LineChart, CartesianGrid, XAxis, YAxis } from "recharts";

interface Stats {
  activeJobs: number;
  totalConnections: number;
  workersOnline: number;
  failedJobs24h: number;
  pendingJobs: number;
  registeredUsers?: number;
  onlineUsers?: number;
  workerThreads?: number;
  appDbPoolActive?: number;
  appDbPoolMax?: number;
  apiCpu?: number;
  apiMemUsedMb?: number;
  samples?: Array<{ ts?: string; apiCpu?: number; apiMemMb?: number; t?: string; cpu?: number; memMb?: number }>;
}

const chartConfig = {
  cpu: { label: "CPU %", color: "var(--chart-1)" },
  memMb: { label: "Memory MB", color: "var(--chart-2)" },
} satisfies ChartConfig;

function mapSamples(samples?: Stats["samples"]) {
  return (samples ?? []).map((s) => ({
    t: s.t ?? (s.ts ? new Date(s.ts).toLocaleTimeString() : ""),
    cpu: s.cpu ?? (s.apiCpu != null ? Number((s.apiCpu * 100).toFixed(2)) : 0),
    memMb: s.memMb ?? (s.apiMemMb != null ? Number(s.apiMemMb.toFixed(1)) : 0),
  }));
}

export default function DashboardPage() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [jobs, setJobs] = useState<Array<{ id: string; name: string; status: string }>>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      apiFetch<Stats>("/api/dashboard/stats"),
      apiFetch<{ content?: Array<{ id: string; name: string; status: string }> } | Array<{ id: string; name: string; status: string }>>(
        "/api/jobs?page=0&size=10"
      ),
    ])
      .then(([statsData, jobsData]) => {
        setStats(statsData);
        const list = Array.isArray(jobsData) ? jobsData : jobsData.content ?? [];
        setJobs(list);
      })
      .catch((err: Error) => {
        notify.error("Could not load dashboard", err.message);
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <AppShell>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <PageHeader title="Dashboard" description="Overview of migrations, connections, and workers" />
        <div className="rounded-lg border bg-muted/30 px-4 py-2 text-right">
          <p className="text-xs text-muted-foreground">Local time</p>
          <LocalClock className="font-mono text-2xl font-semibold tracking-tight" />
        </div>
      </div>

      {loading && !stats ? (
        <AppLoader />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard title="Active Jobs" value={stats?.activeJobs ?? "—"} />
            <StatCard title="Connections" value={stats?.totalConnections ?? "—"} />
            <StatCard title="Workers Online" value={stats?.workersOnline ?? "—"} />
            <StatCard title="Failed Jobs" value={stats?.failedJobs24h ?? "—"} />
            <StatCard title="Registered Users" value={stats?.registeredUsers ?? "—"} />
            <StatCard title="Online Users" value={stats?.onlineUsers ?? "—"} />
            <StatCard title="Worker Threads" value={stats?.workerThreads ?? "—"} />
            <StatCard
              title="App DB Pool"
              value={
                stats?.appDbPoolActive != null
                  ? `${stats.appDbPoolActive}/${stats.appDbPoolMax ?? "—"}`
                  : "—"
              }
            />
            <StatCard
              title="API CPU"
              value={stats?.apiCpu != null ? `${(stats.apiCpu * 100).toFixed(1)}%` : "—"}
            />
            <StatCard
              title="API Memory"
              value={stats?.apiMemUsedMb != null ? `${Math.round(stats.apiMemUsedMb)} MB` : "—"}
            />
          </div>

          {(stats?.samples?.length ?? 0) > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>API load (recent)</CardTitle>
              </CardHeader>
              <CardContent>
                <ChartContainer config={chartConfig} className="h-56 w-full">
                  <LineChart data={mapSamples(stats!.samples)}>
                    <CartesianGrid vertical={false} />
                    <XAxis dataKey="t" tickLine={false} axisLine={false} />
                    <YAxis tickLine={false} axisLine={false} />
                    <ChartTooltip content={<ChartTooltipContent />} />
                    <Line type="monotone" dataKey="cpu" stroke="var(--color-cpu)" strokeWidth={2} dot={false} />
                    <Line type="monotone" dataKey="memMb" stroke="var(--color-memMb)" strokeWidth={2} dot={false} />
                  </LineChart>
                </ChartContainer>
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader>
              <CardTitle>Recent Jobs</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-2">
              {jobs.slice(0, 5).map((job) => (
                <div key={job.id} className="flex items-center justify-between rounded-md border p-3">
                  <span className="font-medium">{job.name}</span>
                  <span className="text-sm text-muted-foreground">{job.status}</span>
                </div>
              ))}
              {jobs.length === 0 && <p className="text-sm text-muted-foreground">No jobs yet</p>}
            </CardContent>
          </Card>
        </>
      )}
    </AppShell>
  );
}
