"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/shared/PageHeader";
import { DataTable, type DataTableColumn } from "@/components/shared/DataTable";
import type { PageResponse } from "@/components/shared/PaginationBar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { apiFetch } from "@/lib/api-client";
import { notify } from "@/lib/notify";

type ConnectionRow = { id: string; name: string; pluginId: string };

export default function ConnectionsPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [data, setData] = useState<PageResponse<ConnectionRow> | null>(null);
  const [loading, setLoading] = useState(true);
  const [hasInstalledConnector, setHasInstalledConnector] = useState<boolean | null>(null);
  const [pluginFilter, setPluginFilter] = useState<string>("all");

  useEffect(() => {
    apiFetch<{ id: string; installed?: boolean; enabled?: boolean }[]>("/api/marketplace")
      .then((list) => {
        setHasInstalledConnector(list.some((p) => p.installed ?? p.enabled));
      })
      .catch((e: Error) => {
        notify.error("Failed to load connectors", e.message);
        setHasInstalledConnector(false);
      });
  }, []);

  const load = useCallback(() => {
    setLoading(true);
    apiFetch<PageResponse<ConnectionRow>>(`/api/connections?page=${page}&size=${size}`)
      .then(setData)
      .catch((e: Error) => notify.error("Failed to load connections", e.message))
      .finally(() => setLoading(false));
  }, [page, size]);

  useEffect(() => {
    load();
  }, [load]);

  const plugins = useMemo(() => {
    const set = new Set((data?.content ?? []).map((c) => c.pluginId));
    return Array.from(set).sort();
  }, [data]);

  const rows = useMemo(() => {
    const all = data?.content ?? [];
    if (pluginFilter === "all") return all;
    return all.filter((c) => c.pluginId === pluginFilter);
  }, [data, pluginFilter]);

  const columns: DataTableColumn<ConnectionRow>[] = [
    {
      id: "name",
      header: "Name",
      cell: (c) => <span className="font-medium">{c.name}</span>,
    },
    {
      id: "plugin",
      header: "Connector",
      cell: (c) => (
        <Badge variant="outline" className="rounded-full">
          {c.pluginId}
        </Badge>
      ),
    },
    {
      id: "actions",
      header: "Actions",
      className: "text-right",
      cell: (c) => (
        <Button
          variant="outline"
          size="sm"
          onClick={async () => {
            try {
              await apiFetch(`/api/connections/${c.id}/test`, { method: "POST" });
              notify.success("Connection OK");
            } catch (e) {
              notify.error(
                "Connection test failed",
                e instanceof Error ? e.message : String(e)
              );
            }
          }}
        >
          Test
        </Button>
      ),
    },
  ];

  return (
    <AppShell>
      <PageHeader
        title="Connections"
        description="Saved database connections by connector"
        action={
          <Link href="/connections/new">
            <Button variant="pill">Add Connection</Button>
          </Link>
        }
      />
      {plugins.length > 0 && (
        <div className="flex flex-wrap gap-2">
          <Button
            size="sm"
            variant={pluginFilter === "all" ? "default" : "outline"}
            className="rounded-full"
            onClick={() => setPluginFilter("all")}
          >
            All
          </Button>
          {plugins.map((p) => (
            <Button
              key={p}
              size="sm"
              variant={pluginFilter === p ? "default" : "outline"}
              className="rounded-full"
              onClick={() => setPluginFilter(p)}
            >
              {p}
            </Button>
          ))}
        </div>
      )}
      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(c) => c.id}
        loading={loading}
        empty={
          !loading && (data?.totalElements ?? 0) === 0 ? (
            <div className="flex flex-col items-center gap-3">
              <p className="text-sm text-muted-foreground">No connections yet.</p>
              {hasInstalledConnector ? (
                <Link href="/connections/new">
                  <Button variant="outline" size="sm">
                    Add a connection
                  </Button>
                </Link>
              ) : (
                <Link href="/connectors/marketplace">
                  <Button variant="outline" size="sm">
                    Install a connector first
                  </Button>
                </Link>
              )}
            </div>
          ) : pluginFilter !== "all" ? (
            <p className="text-sm text-muted-foreground">No connections match this filter.</p>
          ) : undefined
        }
        page={page}
        size={size}
        totalElements={data?.totalElements ?? 0}
        totalPages={data?.totalPages ?? 0}
        onPageChange={setPage}
        onSizeChange={(s) => {
          setSize(s);
          setPage(0);
        }}
      />
    </AppShell>
  );
}
