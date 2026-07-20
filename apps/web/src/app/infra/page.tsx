"use client";

import { useCallback, useEffect, useState } from "react";
import { Cpu, Globe, Server } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { useRefreshToken, useSetPageChrome } from "@/components/layout/PageChromeContext";
import { AppLoader } from "@/components/shared/AppLoader";
import { ServiceHealthCard } from "@/components/infra/ServiceHealthCard";
import { HttpStatusChart } from "@/components/dashboard/HttpStatusChart";
import { RouteLatencyPanel } from "@/components/dashboard/RouteLatencyPanel";
import { apiFetch } from "@/lib/api-client";
import { notify } from "@/lib/notify";
import type { HttpSnapshot } from "@/lib/http-types";

interface Sample {
  ts?: string;
  apiCpu?: number;
  apiMemMb?: number;
  http2xx?: number;
  http4xx?: number;
  http5xx?: number;
}

interface InfraSnapshot {
  api?: { status?: string; cpu?: number; memUsedBytes?: number };
  worker?: { status?: string; cpu?: number; memUsedBytes?: number; error?: string };
  web?: { status?: string; note?: string; buildId?: string };
  http?: HttpSnapshot;
  samples?: Sample[];
}

function statusLabel(raw?: string) {
  if (!raw) return "UNKNOWN";
  const s = raw.toUpperCase();
  if (s === "UP" || s === "DOWN" || s === "UNKNOWN" || s === "UI") return s;
  if (s.includes("UP")) return "UP";
  if (s.includes("DOWN")) return "DOWN";
  return "UNKNOWN";
}

function fmtCpu(v?: number) {
  if (v == null) return "—";
  return `${(v * 100).toFixed(1)}%`;
}

function fmtMem(bytes?: number) {
  if (bytes == null) return "—";
  return `${Math.round(bytes / (1024 * 1024))} MB`;
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

  if (loading && !data) return <AppLoader />;

  const webBuild = process.env.NEXT_PUBLIC_BUILD_ID || "dev";
  const apiStatus = statusLabel(data?.api?.status);
  const workerStatus = statusLabel(data?.worker?.status);

  return (
    <>
      <div className="grid gap-4 md:grid-cols-3">
        <ServiceHealthCard
          name="API"
          status={apiStatus}
          icon={Server}
          stats={[
            { title: "CPU", value: fmtCpu(data?.api?.cpu) },
            { title: "Memory", value: fmtMem(data?.api?.memUsedBytes) },
          ]}
        />
        <ServiceHealthCard
          name="Worker"
          status={workerStatus}
          icon={Cpu}
          stats={[
            { title: "CPU", value: fmtCpu(data?.worker?.cpu) },
            { title: "Memory", value: fmtMem(data?.worker?.memUsedBytes) },
          ]}
          note={data?.worker?.error}
        />
        <ServiceHealthCard
          name="Web (Next.js)"
          status={data?.web?.status ?? "UI"}
          icon={Globe}
          stats={[{ title: "Build", value: data?.web?.buildId ?? webBuild }]}
          note={data?.web?.note ?? "Process CPU/RAM for Next.js is host-level only."}
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <HttpStatusChart samples={data?.samples ?? []} />
      </div>

      <RouteLatencyPanel http={data?.http} />
    </>
  );
}
