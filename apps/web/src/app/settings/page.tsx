"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/shared/PageHeader";
import { ConfigEditor, ConfigEntry } from "@/components/shared/ConfigEditor";
import { Card, CardContent } from "@/components/ui/card";
import { apiFetch } from "@/lib/api-client";

export default function SettingsPage() {
  const [config, setConfig] = useState<Record<string, ConfigEntry>>({});
  const [saveError, setSaveError] = useState<string | null>(null);

  useEffect(() => {
    fetchConfig();
  }, []);

  async function fetchConfig() {
    setConfig(await apiFetch<Record<string, ConfigEntry>>("/api/config"));
  }

  async function save() {
    setSaveError(null);
    const payload = Object.fromEntries(
      Object.entries(config)
        .filter(([, v]) => !(v.sensitive && v.masked))
        .map(([k, v]) => [k, v.value])
    );
    await apiFetch("/api/config", { method: "PUT", body: JSON.stringify(payload) });
    toast.success("Configuration saved");
    await fetchConfig();
  }

  async function reveal(key: string) {
    const response = await apiFetch<{ value: string }>(`/api/config/${key}/reveal`);
    const revealedValue = response.value;
    setConfig((c) => ({
      ...c,
      [key]: { ...c[key], value: revealedValue, masked: false },
    }));
  }

  return (
    <AppShell>
      <PageHeader title="Settings" description="App configuration — dashboard edits apply immediately where supported." />
      <Card>
        <CardContent className="pt-6">
          <ConfigEditor
            config={config}
            saveError={saveError}
            onChange={(key, value) =>
              setConfig((c) => ({ ...c, [key]: { ...c[key], value, source: "DASHBOARD" } }))
            }
            onSave={save}
            onReveal={reveal}
          />
        </CardContent>
      </Card>
    </AppShell>
  );
}
