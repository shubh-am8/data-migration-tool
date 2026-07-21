"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { AppShell } from "@/components/layout/AppShell";
import { SetPageChrome } from "@/components/layout/PageChromeContext";
import { AppLoader } from "@/components/shared/AppLoader";
import { ConfigFieldDef, ConnectionForm, ConnectionFormValues } from "@/components/connectors/ConnectionForm";
import { Card, CardContent } from "@/components/ui/card";
import { apiFetch } from "@/lib/api-client";
import type { ConnectionTestResult } from "@/lib/connection-test";
import { notify } from "@/lib/notify";

interface Plugin {
  id: string;
  name: string;
  configFields?: ConfigFieldDef[];
}

type ConnectionEdit = {
  id: string;
  name: string;
  pluginId: string;
  sandbox: boolean;
  config: Record<string, string>;
};

export default function EditConnectionPage() {
  const params = useParams();
  const router = useRouter();
  const id = params.id as string;
  const [loading, setLoading] = useState(true);
  const [connection, setConnection] = useState<ConnectionEdit | null>(null);
  const [fields, setFields] = useState<ConfigFieldDef[]>([]);

  useEffect(() => {
    Promise.all([
      apiFetch<ConnectionEdit>(`/api/connections/${id}`),
      apiFetch<Plugin[]>("/api/marketplace"),
    ])
      .then(([conn, plugins]) => {
        setConnection(conn);
        const plugin = plugins.find((p) => p.id === conn.pluginId);
        setFields(plugin?.configFields ?? []);
      })
      .catch((e: Error) => notify.error("Failed to load connection", e.message))
      .finally(() => setLoading(false));
  }, [id]);

  async function handleTest(values: ConnectionFormValues): Promise<ConnectionTestResult> {
    if (!connection) {
      return { success: false, message: "Connection not loaded", latencyMs: 0 };
    }
    return apiFetch<ConnectionTestResult>("/api/connections/test", {
      method: "POST",
      body: JSON.stringify({ pluginId: connection.pluginId, config: valuesToConfig(values) }),
    });
  }

  async function handleSubmit(values: ConnectionFormValues) {
    try {
      await apiFetch(`/api/connections/${id}`, {
        method: "PUT",
        body: JSON.stringify({
          name: values.name,
          config: valuesToConfig(values),
          sandbox: true,
        }),
      });
      notify.success("Connection updated");
      router.push("/connections");
    } catch (e) {
      notify.error("Failed to update connection", e instanceof Error ? e.message : String(e));
    }
  }

  if (loading) {
    return (
      <AppShell>
        <SetPageChrome title="Edit Connection" />
        <AppLoader label="Loading connection…" />
      </AppShell>
    );
  }

  if (!connection) {
    return (
      <AppShell>
        <SetPageChrome title="Edit Connection" />
        <p className="text-sm text-muted-foreground">Connection not found.</p>
      </AppShell>
    );
  }

  const initial: ConnectionFormValues = {
    name: connection.name,
    ...Object.fromEntries(
      Object.entries(connection.config ?? {}).map(([k, v]) => [k, v == null ? "" : String(v)])
    ),
  };

  return (
    <AppShell>
      <SetPageChrome
        title="Edit Connection"
        description={`Update settings for ${connection.name}`}
      />
      <Card className="mx-auto w-full max-w-3xl">
        <CardContent className="pt-6">
          <ConnectionForm
            key={connection.id}
            fields={fields}
            initial={initial}
            trustExisting
            onTest={handleTest}
            onSubmit={handleSubmit}
          />
        </CardContent>
      </Card>
    </AppShell>
  );
}

function valuesToConfig(v: ConnectionFormValues) {
  const { name, ...config } = v;
  return config;
}
