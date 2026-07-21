"use client";

import { useCallback, useEffect, useState } from "react";
import { Database, RefreshCw, Trash2, Eraser, Wrench, Play, Pause, Square } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { useSetPageChrome } from "@/components/layout/PageChromeContext";
import { AppLoader } from "@/components/shared/AppLoader";
import { apiFetch } from "@/lib/api-client";
import { notify } from "@/lib/notify";
import { LAB_DEST_SCHEMA, LAB_SOURCE_SCHEMA } from "@/lib/simulation-options";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

type SeedSession = {
  jobId: string;
  jobName: string;
  schemaName: string;
  tableName: string;
  scenario: string;
  insertsPerMinute: number;
  updatesPerMinute: number;
  status: "PAUSED" | "RUNNING" | "STOPPED";
  rowsInserted: number;
  rowsUpdated: number;
  lastTickAt?: string;
};

type LabTableStat = {
  name: string;
  kind: string;
  rowCount: number;
  sizeBytes: number;
};

type SchemaStats = {
  schema: string;
  tables: LabTableStat[];
};

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

function SchemaPanel({
  schemaName,
  stats,
  loading,
  loadError,
  onRefresh,
  onTruncate,
  onDrop,
  mutable,
}: {
  schemaName: string;
  stats: SchemaStats | null;
  loading: boolean;
  loadError: string | null;
  onRefresh: () => void;
  onTruncate: (table: string) => void;
  onDrop: (table: string) => void;
  mutable: boolean;
}) {
  const isSource = schemaName === LAB_SOURCE_SCHEMA;
  const accent = isSource ? "emerald" : "sky";

  return (
    <Card>
      <CardHeader className="flex flex-row items-start justify-between gap-4">
        <div>
          <CardTitle className="flex items-center gap-2">
            <Database className="size-5" />
            <span className={cn(isSource ? "text-emerald-600" : "text-sky-600")}>
              {stats?.schema ?? schemaName}
            </span>
          </CardTitle>
          <CardDescription>
            {isSource
              ? "Pre-seeded hot/cold sample tables for TEST job sources."
              : "Per-job migration targets — truncate or drop to verify job impact."}
          </CardDescription>
        </div>
        <Button type="button" variant="outline" size="sm" onClick={onRefresh} disabled={loading}>
          <RefreshCw className={cn("size-4", loading && "animate-spin")} />
          Refresh
        </Button>
      </CardHeader>
      <CardContent className="flex flex-col gap-2">
        {loadError ? (
          <p className="text-sm text-destructive">{loadError}</p>
        ) : loading && !stats ? (
          <p className="text-sm text-muted-foreground">Loading tables…</p>
        ) : (stats?.tables.length ?? 0) === 0 ? (
          <p className="text-sm text-muted-foreground">
            {isSource
              ? "No tables — install Lab Dev Tools from the marketplace or use Repair lab schemas."
              : "No destination tables yet. Create a TEST job to provision one."}
          </p>
        ) : (
          stats?.tables.map((t) => (
            <div
              key={t.name}
              className={cn(
                "flex flex-wrap items-center justify-between gap-2 rounded-lg border p-3",
                accent === "emerald" && "border-emerald-600/30 bg-emerald-600/5",
                accent === "sky" && "border-sky-600/30 bg-sky-600/5"
              )}
            >
              <div className="flex flex-col gap-1">
                <span className="font-medium">{t.name}</span>
                <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                  <Badge variant="secondary">{t.kind}</Badge>
                  <span>{t.rowCount.toLocaleString()} rows</span>
                  <span>{formatBytes(t.sizeBytes)}</span>
                </div>
              </div>
              {mutable ? (
                <div className="flex flex-wrap gap-2">
                  <Button type="button" variant="outline" size="sm" onClick={() => onTruncate(t.name)}>
                    <Eraser className="size-3.5" />
                    Truncate
                  </Button>
                  <Button type="button" variant="danger" size="sm" onClick={() => onDrop(t.name)}>
                    <Trash2 className="size-3.5" />
                    Drop
                  </Button>
                </div>
              ) : null}
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}

function SeedSessionsCard({
  sessions,
  loading,
  onRefresh,
  onAction,
  onRateChange,
}: {
  sessions: SeedSession[];
  loading: boolean;
  onRefresh: () => void;
  onAction: (jobId: string, action: "start" | "pause" | "resume" | "stop") => void;
  onRateChange: (jobId: string, inserts: number, updates: number) => void;
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-start justify-between gap-4">
        <div>
          <CardTitle>Seeding workers</CardTitle>
          <CardDescription>
            Job-linked continuous seeding into test_source. Start from here after creating a TEST migration job.
          </CardDescription>
        </div>
        <Button type="button" variant="outline" size="sm" onClick={onRefresh} disabled={loading}>
          <RefreshCw className={cn("size-4", loading && "animate-spin")} />
          Refresh
        </Button>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {sessions.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            No seed sessions yet. Create a TEST migration job (not the one-shot simulate checkbox) to get a worker.
          </p>
        ) : (
          sessions.map((s) => (
            <div key={s.jobId} className="rounded-lg border border-violet-600/30 bg-violet-600/5 p-3 flex flex-col gap-3">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <p className="font-medium">{s.jobName}</p>
                  <p className="text-xs text-muted-foreground">
                    {s.schemaName}.{s.tableName} · {s.scenario}
                  </p>
                </div>
                <Badge
                  variant="secondary"
                  className={cn(
                    s.status === "RUNNING" && "bg-emerald-600/20 text-emerald-700",
                    s.status === "PAUSED" && "bg-amber-600/20 text-amber-700",
                    s.status === "STOPPED" && "bg-muted text-muted-foreground"
                  )}
                >
                  {s.status}
                </Badge>
              </div>
              <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
                <span>{s.rowsInserted.toLocaleString()} inserted</span>
                <span>{s.rowsUpdated.toLocaleString()} updated</span>
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                <label className="flex flex-col gap-1 text-xs">
                  Inserts / min
                  <Input
                    type="number"
                    min={0}
                    max={10000}
                    defaultValue={s.insertsPerMinute}
                    onBlur={(e) =>
                      onRateChange(s.jobId, Number(e.target.value), s.updatesPerMinute)
                    }
                  />
                </label>
                <label className="flex flex-col gap-1 text-xs">
                  Updates / min
                  <Input
                    type="number"
                    min={0}
                    max={10000}
                    defaultValue={s.updatesPerMinute}
                    onBlur={(e) =>
                      onRateChange(s.jobId, s.insertsPerMinute, Number(e.target.value))
                    }
                  />
                </label>
              </div>
              <div className="flex flex-wrap gap-2">
                {s.status !== "RUNNING" && s.status !== "STOPPED" && (
                  <Button type="button" size="sm" variant="outline" onClick={() => onAction(s.jobId, "start")}>
                    <Play className="size-3.5" />
                    Start
                  </Button>
                )}
                {s.status === "RUNNING" && (
                  <Button type="button" size="sm" variant="outline" onClick={() => onAction(s.jobId, "pause")}>
                    <Pause className="size-3.5" />
                    Pause
                  </Button>
                )}
                {s.status === "PAUSED" && (
                  <Button type="button" size="sm" variant="outline" onClick={() => onAction(s.jobId, "resume")}>
                    <Play className="size-3.5" />
                    Resume
                  </Button>
                )}
                {s.status !== "STOPPED" && (
                  <Button type="button" size="sm" variant="danger" onClick={() => onAction(s.jobId, "stop")}>
                    <Square className="size-3.5" />
                    Stop
                  </Button>
                )}
              </div>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}

function LabPlaygroundBody() {
  useSetPageChrome({
    title: "Lab Playground",
    description: "Inspect test_source sample data and test_destination job tables on migration_lab",
  });

  const [sourceStats, setSourceStats] = useState<SchemaStats | null>(null);
  const [destStats, setDestStats] = useState<SchemaStats | null>(null);
  const [sourceError, setSourceError] = useState<string | null>(null);
  const [destError, setDestError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [repairing, setRepairing] = useState(false);
  const [seedSessions, setSeedSessions] = useState<SeedSession[]>([]);
  const [seedsLoading, setSeedsLoading] = useState(false);

  const loadSeeds = useCallback(async () => {
    setSeedsLoading(true);
    try {
      const sessions = await apiFetch<SeedSession[]>("/api/lab/seed-sessions");
      setSeedSessions(sessions);
    } catch (e) {
      notify.error("Could not load seed sessions", e instanceof Error ? e.message : "Unknown error");
    } finally {
      setSeedsLoading(false);
    }
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setSourceError(null);
    setDestError(null);
    const [srcResult, destResult] = await Promise.allSettled([
      apiFetch<SchemaStats>(`/api/lab/stats/${LAB_SOURCE_SCHEMA}`),
      apiFetch<SchemaStats>(`/api/lab/stats/${LAB_DEST_SCHEMA}`),
    ]);

    if (srcResult.status === "fulfilled") {
      setSourceStats(srcResult.value);
    } else {
      setSourceStats(null);
      const msg = srcResult.reason instanceof Error ? srcResult.reason.message : "Unknown error";
      setSourceError(msg);
    }

    if (destResult.status === "fulfilled") {
      setDestStats(destResult.value);
    } else {
      setDestStats(null);
      const msg = destResult.reason instanceof Error ? destResult.reason.message : "Unknown error";
      setDestError(msg);
    }

    if (srcResult.status === "rejected" || destResult.status === "rejected") {
      notify.error("Could not load all lab stats");
    }
    setLoading(false);
    void loadSeeds();
  }, [loadSeeds]);

  useEffect(() => {
    void load();
  }, [load]);

  async function seedAction(jobId: string, action: "start" | "pause" | "resume" | "stop") {
    try {
      await apiFetch(`/api/lab/seed-sessions/${jobId}/${action}`, { method: "POST" });
      notify.success(`Seed worker ${action}`);
      await loadSeeds();
      await load();
    } catch (e) {
      notify.error(`Seed ${action} failed`, e instanceof Error ? e.message : "Unknown error");
    }
  }

  async function seedRates(jobId: string, insertsPerMinute: number, updatesPerMinute: number) {
    try {
      await apiFetch(`/api/lab/seed-sessions/${jobId}`, {
        method: "PATCH",
        body: JSON.stringify({ insertsPerMinute, updatesPerMinute }),
      });
      await loadSeeds();
    } catch (e) {
      notify.error("Rate update failed", e instanceof Error ? e.message : "Unknown error");
    }
  }

  async function repairSchemas() {
    setRepairing(true);
    try {
      const r = await apiFetch<{ sourceTableCount: number; destinationTableCount: number }>(
        "/api/lab/repair",
        { method: "POST" }
      );
      notify.success(
        `Lab schemas repaired — test_source: ${r.sourceTableCount} table(s), test_destination: ${r.destinationTableCount} table(s)`
      );
      await load();
    } catch (e) {
      notify.error("Repair failed", e instanceof Error ? e.message : "Unknown error");
    } finally {
      setRepairing(false);
    }
  }

  async function truncateTable(table: string) {
    try {
      await apiFetch(`/api/lab/schemas/${LAB_DEST_SCHEMA}/tables/${encodeURIComponent(table)}/truncate`, {
        method: "POST",
      });
      notify.success(`Truncated ${table}`);
      await load();
    } catch (e) {
      notify.error("Truncate failed", e instanceof Error ? e.message : "Unknown error");
    }
  }

  async function dropTable(table: string) {
    if (!window.confirm(`Drop table ${LAB_DEST_SCHEMA}.${table}? This cannot be undone.`)) return;
    try {
      await apiFetch(`/api/lab/schemas/${LAB_DEST_SCHEMA}/tables/${encodeURIComponent(table)}/drop`, {
        method: "POST",
      });
      notify.success(`Dropped ${table}`);
      await load();
    } catch (e) {
      notify.error("Drop failed", e instanceof Error ? e.message : "Unknown error");
    }
  }

  async function bulkAction(action: "truncate-all" | "drop-all") {
    const label = action === "truncate-all" ? "truncate all destination tables" : "drop all destination tables";
    if (!window.confirm(`Really ${label}?`)) return;
    try {
      const r = await apiFetch<{ tablesAffected: number }>(`/api/lab/destination/${action}`, { method: "POST" });
      notify.success(`${action === "truncate-all" ? "Truncated" : "Dropped"} ${r.tablesAffected} table(s)`);
      await load();
    } catch (e) {
      notify.error("Bulk action failed", e instanceof Error ? e.message : "Unknown error");
    }
  }

  const hasLoadError = Boolean(sourceError || destError);
  const initialLoad = loading && !sourceStats && !destStats && !hasLoadError;
  if (initialLoad) return <AppLoader />;

  return (
    <div className="flex flex-col gap-6">
      {hasLoadError && (
        <Alert variant="destructive">
          <AlertTitle>Lab database unavailable or schemas missing</AlertTitle>
          <AlertDescription className="flex flex-col gap-2">
            {sourceError && <span>test_source: {sourceError}</span>}
            {destError && <span>test_destination: {destError}</span>}
            <span>
              Ensure <code className="text-xs">labdb</code> is running on port 5433, then install or repair Lab Dev
              Tools.
            </span>
          </AlertDescription>
        </Alert>
      )}

      <div className="flex flex-wrap gap-2">
        <Button type="button" variant="outline" onClick={() => void load()} disabled={loading}>
          <RefreshCw className={cn("size-4", loading && "animate-spin")} />
          Refresh all
        </Button>
        <Button type="button" variant="outline" onClick={() => void repairSchemas()} disabled={repairing}>
          <Wrench className={cn("size-4", repairing && "animate-spin")} />
          Repair lab schemas
        </Button>
        <Button type="button" variant="warning" onClick={() => void bulkAction("truncate-all")}>
          <Eraser className="size-4" />
          Truncate all destination
        </Button>
        <Button type="button" variant="danger" onClick={() => void bulkAction("drop-all")}>
          <Trash2 className="size-4" />
          Drop all destination
        </Button>
      </div>

      <SeedSessionsCard
        sessions={seedSessions}
        loading={seedsLoading}
        onRefresh={() => void loadSeeds()}
        onAction={(jobId, action) => void seedAction(jobId, action)}
        onRateChange={(jobId, inserts, updates) => void seedRates(jobId, inserts, updates)}
      />

      <div className="grid gap-4 lg:grid-cols-2">
        <SchemaPanel
          schemaName={LAB_SOURCE_SCHEMA}
          stats={sourceStats}
          loading={loading}
          loadError={sourceError}
          onRefresh={() => void load()}
          onTruncate={truncateTable}
          onDrop={dropTable}
          mutable={false}
        />
        <SchemaPanel
          schemaName={LAB_DEST_SCHEMA}
          stats={destStats}
          loading={loading}
          loadError={destError}
          onRefresh={() => void load()}
          onTruncate={truncateTable}
          onDrop={dropTable}
          mutable
        />
      </div>
    </div>
  );
}

export default function LabPage() {
  return (
    <AppShell>
      <LabPlaygroundBody />
    </AppShell>
  );
}
