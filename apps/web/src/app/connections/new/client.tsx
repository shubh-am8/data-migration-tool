"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { AppShell } from "@/components/layout/AppShell";
import { SetPageChrome } from "@/components/layout/PageChromeContext";
import { AppLoader } from "@/components/shared/AppLoader";
import { ConfigFieldDef, ConnectionForm, ConnectionFormValues } from "@/components/connectors/ConnectionForm";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Field, FieldLabel } from "@/components/ui/field";
import { apiFetch } from "@/lib/api-client";
import { notify } from "@/lib/notify";

interface Plugin {
  id: string;
  name: string;
  installed?: boolean;
  enabled?: boolean;
  configFields?: ConfigFieldDef[];
}

export default function NewConnectionClient() {
  const params = useSearchParams();
  const router = useRouter();
  const [plugins, setPlugins] = useState<Plugin[]>([]);
  const [loading, setLoading] = useState(true);
  const [pluginId, setPluginId] = useState(params.get("plugin") || "");

  useEffect(() => {
    apiFetch<Plugin[]>("/api/marketplace")
      .then((list) => {
        const installed = list.filter((p) => p.installed ?? p.enabled);
        setPlugins(installed);
        setPluginId((current) => {
          if (current && installed.some((p) => p.id === current)) return current;
          return installed[0]?.id ?? "";
        });
      })
      .catch((e: Error) => notify.error("Failed to load connectors", e.message))
      .finally(() => setLoading(false));
  }, []);

  async function handleTest(values: ConnectionFormValues) {
    if (!pluginId) return;
    const result = await apiFetch<{ success: boolean; message: string }>("/api/connections/test", {
      method: "POST",
      body: JSON.stringify({ pluginId, config: valuesToConfig(values) }),
    });
    if (result.success) notify.success(result.message);
    else notify.error(result.message);
  }

  async function handleSubmit(values: ConnectionFormValues, sandbox: boolean) {
    if (!pluginId) {
      notify.error("Select an installed connector first");
      return;
    }
    await apiFetch("/api/connections", {
      method: "POST",
      body: JSON.stringify({ pluginId, name: values.name, config: valuesToConfig(values), sandbox }),
    });
    notify.success("Connection saved");
    router.push("/connections");
  }

  if (loading) {
    return (
      <AppShell>
        <SetPageChrome title="Add Connection" />
        <AppLoader label="Loading connectors…" />
      </AppShell>
    );
  }

  if (plugins.length === 0) {
    return (
      <AppShell>
        <SetPageChrome title="Add Connection" description="Install a connector first" />
        <Card>
          <CardContent className="flex flex-col items-start gap-3 pt-6">
            <p className="text-sm text-muted-foreground">
              No connectors are installed. Install PostgreSQL (or upload a JAR) from the Marketplace.
            </p>
            <Link href="/connectors/marketplace">
              <Button variant="pill">Go to Marketplace</Button>
            </Link>
          </CardContent>
        </Card>
      </AppShell>
    );
  }

  return (
    <AppShell>
      <SetPageChrome title="Add Connection" description="Choose an installed connector, then configure it" />
      <Card>
        <CardContent className="flex flex-col gap-6 pt-6">
          <Field>
            <FieldLabel>Connector</FieldLabel>
            <Select value={pluginId} onValueChange={(v) => setPluginId(v ?? "")}>
              <SelectTrigger className="w-full max-w-md">
                <SelectValue placeholder="Select connector" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  {plugins.map((p) => (
                    <SelectItem key={p.id} value={p.id}>
                      {p.name} ({p.id})
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
          </Field>
          {pluginId ? (
            <ConnectionForm
              key={pluginId}
              fields={plugins.find((p) => p.id === pluginId)?.configFields ?? []}
              onTest={handleTest}
              onSubmit={handleSubmit}
            />
          ) : null}
        </CardContent>
      </Card>
    </AppShell>
  );
}

function valuesToConfig(v: ConnectionFormValues) {
  const { name, ...config } = v;
  return config;
}
