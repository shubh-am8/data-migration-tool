"use client";

import { useCallback, useEffect, useState } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/shared/PageHeader";
import { DataTable, type DataTableColumn } from "@/components/shared/DataTable";
import type { PageResponse } from "@/components/shared/PaginationBar";
import { Badge } from "@/components/ui/badge";
import { apiFetch } from "@/lib/api-client";
import { notify } from "@/lib/notify";

type WorkerRow = Record<string, unknown>;

export default function WorkersPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [data, setData] = useState<PageResponse<WorkerRow> | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    apiFetch<PageResponse<WorkerRow>>(`/api/workers?page=${page}&size=${size}`)
      .then((r) => {
        setData(r);
        setLoading(false);
      })
      .catch((e: Error) => {
        notify.error("Failed to load workers", e.message);
        setLoading(false);
      });
  }, [page, size]);

  useEffect(() => {
    load();
    const t = setInterval(load, 5000);
    return () => clearInterval(t);
  }, [load]);

  const columns: DataTableColumn<WorkerRow>[] = [
    {
      id: "workerId",
      header: "Worker ID",
      cell: (w) => String(w.workerId ?? ""),
    },
    {
      id: "threads",
      header: "Active Threads",
      cell: (w) => String(w.activeThreads ?? 0),
    },
    {
      id: "job",
      header: "Current Job",
      cell: (w) => String(w.currentJobId || "—"),
    },
    {
      id: "seen",
      header: "Last Seen",
      cell: (w) => String(w.lastSeen ?? "—"),
    },
    {
      id: "status",
      header: "Status",
      cell: (w) => (
        <Badge variant={w.online ? "default" : "secondary"} className="rounded-full">
          {w.online ? "online" : "offline"}
        </Badge>
      ),
    },
  ];

  return (
    <AppShell>
      <PageHeader title="Workers" description="Live worker and thread status" />
      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(w) => String(w.workerId)}
        loading={loading}
        empty={<p className="text-sm text-muted-foreground">No workers online.</p>}
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
