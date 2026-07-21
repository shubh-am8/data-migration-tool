"use client";

import { useCallback, useEffect, useState } from "react";
import { Database, RefreshCw, Trash2, Eraser } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { useSetPageChrome } from "@/components/layout/PageChromeContext";
import { AppLoader } from "@/components/shared/AppLoader";
import { apiFetch } from "@/lib/api-client";
import { notify } from "@/lib/notify";
import { LAB_DEST_SCHEMA, LAB_SOURCE_SCHEMA } from "@/lib/simulation-options";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

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
  stats,
  loading,
  onRefresh,
  onTruncate,
  onDrop,
  mutable,
}: {
  stats: SchemaStats | null;
  loading: boolean;
  onRefresh: () => void;
  onTruncate: (table: string) => void;
  onDrop: (table: string) => void;
  mutable: boolean;
}) {
  const isSource = stats?.schema === LAB_SOURCE_SCHEMA;
  const accent = isSource ? "emerald" : "sky";

  return (
    <Card>
      <CardHeader className="flex flex-row items-start justify-between gap-4">
        <div>
          <CardTitle className="flex items-center gap-2">
            <Database className="size-5" />
            <span className={cn(isSource ? "text-emerald-600" : "text-sky-600")}>{stats?.schema ?? "…"}</span>
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
        {loading && !stats ? (
          <p className="text-sm text-muted-foreground">Loading tables…</p>
        ) : (stats?.tables.length ?? 0) === 0 ? (
          <p className="text-sm text-muted-foreground">
            {isSource
              ? "No tables — install Lab Dev Tools from the marketplace."
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

function LabPlaygroundBody() {
  useSetPageChrome({
    title: "Lab Playground",
    description: "Inspect test_source sample data and test_destination job tables on migration_lab",
  });

  const [sourceStats, setSourceStats] = useState<SchemaStats | null>(null);
  const [destStats, setDestStats] = useState<SchemaStats | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [src, dest] = await Promise.all([
        apiFetch<SchemaStats>(`/api/lab/stats/${LAB_SOURCE_SCHEMA}`),
        apiFetch<SchemaStats>(`/api/lab/stats/${LAB_DEST_SCHEMA}`),
      ]);
      setSourceStats(src);
      setDestStats(dest);
    } catch (e) {
      notify.error("Could not load lab stats", e instanceof Error ? e.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

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

  if (loading && !sourceStats && !destStats) return <AppLoader />;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap gap-2">
        <Button type="button" variant="outline" onClick={() => void load()} disabled={loading}>
          <RefreshCw className={cn("size-4", loading && "animate-spin")} />
          Refresh all
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

      <div className="grid gap-4 lg:grid-cols-2">
        <SchemaPanel
          stats={sourceStats}
          loading={loading}
          onRefresh={() => void load()}
          onTruncate={truncateTable}
          onDrop={dropTable}
          mutable={false}
        />
        <SchemaPanel
          stats={destStats}
          loading={loading}
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
