"use client";

import { useEffect, useState } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/shared/PageHeader";
import { ConfigEditor, ConfigEntry } from "@/components/shared/ConfigEditor";
import { Card, CardContent } from "@/components/ui/card";
import { apiFetch } from "@/lib/api-client";
import { notify } from "@/lib/notify";

export default function SettingsPage() {
  const [config, setConfig] = useState<Record<string, ConfigEntry>>({});
  const [saveError, setSaveError] = useState<string | null>(null);

  useEffect(() => {
    fetchConfig().catch((e: Error) => notify.error("Failed to load settings", e.message));
  }, []);

  async function fetchConfig() {
    setConfig(await apiFetch<Record<string, ConfigEntry>>("/api/config"));
  }

  async function save() {
    setSaveError(null);
    try {
      const payload = Object.fromEntries(
        Object.entries(config)
          .filter(([, v]) => !(v.sensitive && v.masked))
          .map(([k, v]) => [k, v.value])
      );
      await apiFetch("/api/config", { method: "PUT", body: JSON.stringify(payload) });
      notify.success("Configuration saved");
      await fetchConfig();
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      setSaveError(msg);
      notify.error("Save failed", msg);
    }
  }

  async function reveal(key: string) {
    try {
      const response = await apiFetch<{ value: string }>(`/api/config/${key}/reveal`);
      setConfig((c) => ({
        ...c,
        [key]: { ...c[key], value: response.value, masked: false },
      }));
    } catch (e) {
      notify.error("Reveal failed", e instanceof Error ? e.message : String(e));
    }
  }

  function hide(key: string) {
    setConfig((c) => ({
      ...c,
      [key]: { ...c[key], value: "********", masked: true },
    }));
  }

  return (
    <AppShell>
      <PageHeader
        title="Settings"
        description="App configuration — dashboard edits apply immediately where supported."
      />
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
            onHide={hide}
          />
        </CardContent>
      </Card>
    </AppShell>
  );
}
