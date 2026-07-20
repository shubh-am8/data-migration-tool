"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/shared/PageHeader";
import { DataTable, type DataTableColumn } from "@/components/shared/DataTable";
import type { PageResponse } from "@/components/shared/PaginationBar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { apiFetch } from "@/lib/api-client";
import { notify } from "@/lib/notify";

type JobRow = { id: string; name: string; status: string };

export default function JobsPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [data, setData] = useState<PageResponse<JobRow> | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    apiFetch<PageResponse<JobRow>>(`/api/jobs?page=${page}&size=${size}`)
      .then(setData)
      .catch((e: Error) => notify.error("Failed to load jobs", e.message))
      .finally(() => setLoading(false));
  }, [page, size]);

  useEffect(() => {
    load();
  }, [load]);

  const columns: DataTableColumn<JobRow>[] = [
    {
      id: "name",
      header: "Name",
      cell: (job) => (
        <Link href={`/jobs/${job.id}`} className="font-medium hover:underline">
          {job.name}
        </Link>
      ),
    },
    {
      id: "status",
      header: "Status",
      className: "w-40",
      cell: (job) => (
        <Badge variant="secondary" className="rounded-full">
          {job.status}
        </Badge>
      ),
    },
  ];

  return (
    <AppShell>
      <PageHeader
        title="Jobs"
        description="Data migration jobs"
        action={
          <Link href="/jobs/new">
            <Button variant="pill">Create Job</Button>
          </Link>
        }
      />
      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(j) => j.id}
        loading={loading}
        empty={<p className="text-sm text-muted-foreground">No jobs yet.</p>}
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
