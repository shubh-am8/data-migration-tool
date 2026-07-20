"use client";

import { useCallback, useEffect, useState } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { SetPageChrome } from "@/components/layout/PageChromeContext";
import { EmailConfirmDialog } from "@/components/shared/EmailConfirmDialog";
import { DataTable, type DataTableColumn } from "@/components/shared/DataTable";
import type { PageResponse } from "@/components/shared/PaginationBar";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { apiFetch } from "@/lib/api-client";
import { notify } from "@/lib/notify";
import {
  presenceBadgeVariant,
  revokedBadgeVariant,
} from "@/lib/user-status-badge";

interface AdminUser {
  id: string;
  email: string;
  name: string;
  pictureUrl?: string | null;
  lastLoginAt?: string | null;
  lastSeenAt?: string | null;
  online: boolean;
  revoked: boolean;
  admin: boolean;
}

type Pending =
  | { type: "revoke"; user: AdminUser }
  | { type: "delete"; user: AdminUser }
  | null;

function initials(name: string, email: string) {
  const base = (name || email || "?").trim();
  const parts = base.split(/\s+/).filter(Boolean);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return base.slice(0, 2).toUpperCase();
}

export default function UsersPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [data, setData] = useState<PageResponse<AdminUser> | null>(null);
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState<Pending>(null);

  const load = useCallback(() => {
    setLoading(true);
    apiFetch<PageResponse<AdminUser>>(`/api/admin/users?page=${page}&size=${size}`)
      .then(setData)
      .catch((e: Error) => notify.error("Failed to load users", e.message))
      .finally(() => setLoading(false));
  }, [page, size]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleConfirm() {
    if (!pending) return;
    const { type, user } = pending;
    try {
      if (type === "revoke") {
        await apiFetch(`/api/admin/users/${user.id}/revoke`, { method: "POST" });
        notify.success("Login revoked");
      } else {
        await apiFetch(`/api/admin/users/${user.id}`, { method: "DELETE" });
        notify.success("User deleted");
      }
      setPending(null);
      load();
    } catch (e) {
      const action = type === "revoke" ? "Revoke" : "Delete";
      notify.error(`${action} failed`, e instanceof Error ? e.message : undefined);
    }
  }

  const columns: DataTableColumn<AdminUser>[] = [
    {
      id: "user",
      header: "User",
      cell: (u) => (
        <div className="flex items-center gap-3">
          <Avatar className="size-9">
            {u.pictureUrl ? <AvatarImage src={u.pictureUrl} alt="" /> : null}
            <AvatarFallback>{initials(u.name, u.email)}</AvatarFallback>
          </Avatar>
          <div className="min-w-0">
            <p className="truncate font-medium">{u.name || "—"}</p>
            <p className="truncate text-sm text-muted-foreground">{u.email}</p>
          </div>
        </div>
      ),
    },
    {
      id: "status",
      header: "Status",
      cell: (u) => (
        <div className="flex flex-wrap gap-1">
          <Badge variant={presenceBadgeVariant(u.online)} className="rounded-full">
            {u.online ? "Online" : "Offline"}
          </Badge>
          {u.revoked && (
            <Badge variant={revokedBadgeVariant()} className="rounded-full">
              Revoked
            </Badge>
          )}
          {u.admin && <Badge className="rounded-full">Admin</Badge>}
        </div>
      ),
    },
    {
      id: "login",
      header: "Last login",
      cell: (u) => (
        <span className="text-sm text-muted-foreground">
          {u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleString() : "—"}
        </span>
      ),
    },
    {
      id: "seen",
      header: "Last seen",
      cell: (u) => (
        <span className="text-sm text-muted-foreground">
          {u.lastSeenAt ? new Date(u.lastSeenAt).toLocaleString() : "—"}
        </span>
      ),
    },
    {
      id: "actions",
      header: "Actions",
      className: "text-right",
      cell: (u) => (
        <div className="flex justify-end gap-2">
          <Button
            variant="warning"
            size="sm"
            onClick={() => setPending({ type: "revoke", user: u })}
          >
            Revoke
          </Button>
          <Button
            variant="danger"
            size="sm"
            onClick={() => setPending({ type: "delete", user: u })}
          >
            Delete
          </Button>
        </div>
      ),
    },
  ];

  return (
    <AppShell>
      <SetPageChrome title="Users" description="Domain admins can revoke sessions or delete users" />
      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(u) => u.id}
        loading={loading}
        empty={<p className="text-sm text-muted-foreground">No users yet</p>}
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
      {pending ? (
        <EmailConfirmDialog
          open
          onOpenChange={(open) => {
            if (!open) setPending(null);
          }}
          title={pending.type === "revoke" ? "Revoke login" : "Delete user"}
          description={
            pending.type === "revoke"
              ? "This user will be signed out and unable to log in until an admin restores access."
              : "This permanently removes the user and cannot be undone."
          }
          confirmLabel={pending.type === "revoke" ? "Revoke" : "Delete"}
          confirmVariant={pending.type === "revoke" ? "warning" : "danger"}
          subject={pending.user}
          onConfirm={handleConfirm}
        />
      ) : null}
    </AppShell>
  );
}
