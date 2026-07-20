"use client";

import { useCallback, useEffect, useState } from "react";
import { Activity, AlertTriangle, Cpu, Database, MemoryStick, Plug, Server, Users } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { useRefreshToken, useSetPageChrome } from "@/components/layout/PageChromeContext";
import { StatCard } from "@/components/dashboard/StatCard";
import { HttpStatusChart } from "@/components/dashboard/HttpStatusChart";
import { RouteLatencyPanel } from "@/components/dashboard/RouteLatencyPanel";
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
import type { HttpSnapshot } from "@/lib/http-types";

interface Sample {
  ts?: string;
  apiCpu?: number;
  apiMemMb?: number;
  http2xx?: number;
  http4xx?: number;
  http5xx?: number;
}

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
  http?: HttpSnapshot;
  samples?: Sample[];
}

const cpuChartConfig = {
  cpu: { label: "CPU %", color: "var(--chart-1)" },
  memMb: { label: "Memory MB", color: "var(--chart-2)" },
} satisfies ChartConfig;

function mapCpuSamples(samples?: Sample[]) {
  return (samples ?? []).map((s) => ({
    t: s.ts ? new Date(s.ts).toLocaleTimeString() : "",
    cpu: s.apiCpu != null ? Number((s.apiCpu * 100).toFixed(2)) : 0,
    memMb: s.apiMemMb != null ? Number(s.apiMemMb.toFixed(1)) : 0,
  }));
}

export default function DashboardPage() {
  return (
    <AppShell>
      <DashboardBody />
    </AppShell>
  );
}

function DashboardBody() {
  useSetPageChrome({ title: "Dashboard", description: "Overview of migrations, connections, and workers" });
  const refreshToken = useRefreshToken();

  const [stats, setStats] = useState<Stats | null>(null);
  const [jobs, setJobs] = useState<Array<{ id: string; name: string; status: string }>>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
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

  useEffect(() => {
    load();
  }, [load, refreshToken]);

  if (loading && !stats) return <AppLoader />;

  const poolPct =
    stats?.appDbPoolActive != null && stats?.appDbPoolMax
      ? stats.appDbPoolActive / stats.appDbPoolMax
      : null;

  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard title="Active Jobs" value={stats?.activeJobs ?? "—"} icon={Activity} tone="info" />
        <StatCard title="Connections" value={stats?.totalConnections ?? "—"} icon={Plug} tone="default" />
        <StatCard
          title="Workers Online"
          value={stats?.workersOnline ?? "—"}
          icon={Server}
          tone={(stats?.workersOnline ?? 0) > 0 ? "success" : "warning"}
        />
        <StatCard
          title="Failed Jobs"
          value={stats?.failedJobs24h ?? "—"}
          icon={AlertTriangle}
          tone={(stats?.failedJobs24h ?? 0) > 0 ? "danger" : "success"}
        />
        <StatCard title="Registered Users" value={stats?.registeredUsers ?? "—"} icon={Users} tone="default" />
        <StatCard title="Online Users" value={stats?.onlineUsers ?? "—"} icon={Users} tone="info" />
        <StatCard title="Worker Threads" value={stats?.workerThreads ?? "—"} icon={Cpu} tone="default" />
        <StatCard
          title="App DB Pool"
          value={
            stats?.appDbPoolActive != null
              ? `${stats.appDbPoolActive}/${stats.appDbPoolMax ?? "—"}`
              : "—"
          }
          icon={Database}
          tone={poolPct != null && poolPct > 0.8 ? "warning" : "default"}
        />
        <StatCard
          title="API CPU"
          value={stats?.apiCpu != null ? `${(stats.apiCpu * 100).toFixed(1)}%` : "—"}
          icon={Cpu}
          tone={stats?.apiCpu != null && stats.apiCpu > 0.8 ? "danger" : "default"}
        />
        <StatCard
          title="API Memory"
          value={stats?.apiMemUsedMb != null ? `${Math.round(stats.apiMemUsedMb)} MB` : "—"}
          icon={MemoryStick}
          tone="default"
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <HttpStatusChart samples={stats?.samples ?? []} />

        {(stats?.samples?.length ?? 0) > 0 && (
          <Card>
            <CardHeader>
              <CardTitle>API load (recent)</CardTitle>
            </CardHeader>
            <CardContent>
              <ChartContainer config={cpuChartConfig} className="h-56 w-full">
                <LineChart data={mapCpuSamples(stats?.samples)}>
                  <CartesianGrid vertical={false} />
                  <XAxis dataKey="t" tickLine={false} axisLine={false} minTickGap={32} />
                  <YAxis tickLine={false} axisLine={false} width={40} />
                  <ChartTooltip content={<ChartTooltipContent />} />
                  <Line type="monotone" dataKey="cpu" stroke="var(--color-cpu)" strokeWidth={2} dot={false} />
                  <Line type="monotone" dataKey="memMb" stroke="var(--color-memMb)" strokeWidth={2} dot={false} />
                </LineChart>
              </ChartContainer>
            </CardContent>
          </Card>
        )}
      </div>

      <RouteLatencyPanel http={stats?.http} />

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
  );
}
