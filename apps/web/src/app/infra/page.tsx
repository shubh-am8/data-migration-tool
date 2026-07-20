"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { useRefreshToken, useSetPageChrome } from "@/components/layout/PageChromeContext";
import { AppLoader } from "@/components/shared/AppLoader";
import { StatCard } from "@/components/dashboard/StatCard";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";
import { CartesianGrid, Line, LineChart, XAxis, YAxis } from "recharts";
import { apiFetch } from "@/lib/api-client";
import { notify } from "@/lib/notify";

interface Sample {
  ts?: string;
  apiCpu?: number;
  apiMemMb?: number;
  poolActive?: number;
  poolMax?: number;
  workersOnline?: number;
}

interface InfraSnapshot {
  api?: { status?: string; cpu?: number; memUsedBytes?: number };
  worker?: { status?: string; cpu?: number; memUsedBytes?: number; error?: string };
  web?: { status?: string; note?: string; buildId?: string };
  samples?: Sample[];
}

const chartConfig = {
  cpuPct: { label: "API CPU %", color: "var(--chart-1)" },
  memMb: { label: "Memory MB", color: "var(--chart-2)" },
  poolActive: { label: "DB pool active", color: "var(--chart-3)" },
} satisfies ChartConfig;

function statusLabel(raw?: string) {
  if (!raw) return "UNKNOWN";
  const s = raw.toUpperCase();
  if (s === "UP" || s === "DOWN" || s === "UNKNOWN" || s === "UI") return s;
  if (s.includes("UP")) return "UP";
  if (s.includes("DOWN")) return "DOWN";
  return "UNKNOWN";
}

export default function InfraPage() {
  return (
    <AppShell>
      <InfraBody />
    </AppShell>
  );
}

function InfraBody() {
  useSetPageChrome({
    title: "Infra",
    description: "Actuator-backed visibility for API and Worker (last 3 hours in memory)",
  });
  const refreshToken = useRefreshToken();

  const [data, setData] = useState<InfraSnapshot | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    apiFetch<InfraSnapshot>("/api/admin/infra")
      .then(setData)
      .catch((e: Error) => notify.error("Infra load failed", e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load, refreshToken]);

  const chartData = useMemo(
    () =>
      (data?.samples ?? []).map((s) => ({
        t: s.ts ? new Date(s.ts).toLocaleTimeString() : "",
        cpuPct: s.apiCpu != null ? Number((s.apiCpu * 100).toFixed(2)) : null,
        memMb: s.apiMemMb != null ? Number(s.apiMemMb.toFixed(1)) : null,
        poolActive: s.poolActive ?? null,
      })),
    [data?.samples]
  );

  const webBuild = process.env.NEXT_PUBLIC_BUILD_ID || "dev";
  const apiStatus = statusLabel(data?.api?.status);
  const workerStatus = statusLabel(data?.worker?.status);

  return loading && !data ? (
    <AppLoader />
  ) : (
    <div className="flex flex-col gap-6">
      <div className="grid gap-4 md:grid-cols-3">
        <Card className="min-w-0">
          <CardHeader className="flex flex-row items-center justify-between gap-2">
            <CardTitle className="truncate">API</CardTitle>
            <Badge
              variant={apiStatus === "UP" ? "secondary" : "destructive"}
              className="max-w-[8rem] truncate rounded-full"
            >
              {apiStatus}
            </Badge>
          </CardHeader>
          <CardContent className="grid gap-2">
            <StatCard title="CPU" value={fmtCpu(data?.api?.cpu)} />
            <StatCard title="Memory" value={fmtMem(data?.api?.memUsedBytes)} />
          </CardContent>
        </Card>
        <Card className="min-w-0">
          <CardHeader className="flex flex-row items-center justify-between gap-2">
            <CardTitle className="truncate">Worker</CardTitle>
            <Badge
              variant={workerStatus === "UP" ? "secondary" : "destructive"}
              className="max-w-[8rem] truncate rounded-full"
            >
              {workerStatus}
            </Badge>
          </CardHeader>
          <CardContent className="grid gap-2">
            <StatCard title="CPU" value={fmtCpu(data?.worker?.cpu)} />
            <StatCard title="Memory" value={fmtMem(data?.worker?.memUsedBytes)} />
            {data?.worker?.error && (
              <p className="truncate text-sm text-destructive" title={data.worker.error}>
                {data.worker.error}
              </p>
            )}
          </CardContent>
        </Card>
        <Card className="min-w-0">
          <CardHeader className="flex flex-row items-center justify-between gap-2">
            <CardTitle className="truncate">Web (Next.js)</CardTitle>
            <Badge variant="secondary" className="rounded-full">
              {data?.web?.status ?? "UI"}
            </Badge>
          </CardHeader>
          <CardContent className="grid gap-2">
            <StatCard title="Build" value={data?.web?.buildId ?? webBuild} />
            <p className="text-sm text-muted-foreground">
              {data?.web?.note ??
                "Process CPU/RAM for Next.js is host-level only."}
            </p>
          </CardContent>
        </Card>
      </div>

      {chartData.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>API metrics (3h ring)</CardTitle>
          </CardHeader>
          <CardContent>
            <ChartContainer config={chartConfig} className="h-64 w-full">
              <LineChart data={chartData}>
                <CartesianGrid vertical={false} />
                <XAxis dataKey="t" tickLine={false} axisLine={false} minTickGap={32} />
                <YAxis tickLine={false} axisLine={false} width={40} />
                <ChartTooltip content={<ChartTooltipContent />} />
                <Line
                  type="monotone"
                  dataKey="cpuPct"
                  stroke="var(--color-cpuPct)"
                  strokeWidth={2}
                  dot={false}
                />
                <Line
                  type="monotone"
                  dataKey="memMb"
                  stroke="var(--color-memMb)"
                  strokeWidth={2}
                  dot={false}
                />
                <Line
                  type="monotone"
                  dataKey="poolActive"
                  stroke="var(--color-poolActive)"
                  strokeWidth={2}
                  dot={false}
                />
              </LineChart>
            </ChartContainer>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function fmtCpu(v?: number) {
  if (v == null) return "—";
  return `${(v * 100).toFixed(1)}%`;
}

function fmtMem(bytes?: number) {
  if (bytes == null) return "—";
  return `${Math.round(bytes / (1024 * 1024))} MB`;
}
