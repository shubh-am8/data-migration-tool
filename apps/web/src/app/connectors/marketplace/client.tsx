"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell } from "@/components/layout/AppShell";
import { SetPageChrome } from "@/components/layout/PageChromeContext";
import { AppLoader } from "@/components/shared/AppLoader";
import { ConnectorCard } from "@/components/connectors/ConnectorCard";
import { DocLink } from "@/components/shared/DocLink";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { apiFetch, API_URL } from "@/lib/api-client";
import { notify } from "@/lib/notify";

interface Plugin {
  id: string;
  name: string;
  description: string;
  version: string;
  enabled?: boolean;
  installed?: boolean;
  onClasspath?: boolean;
  builtin?: boolean;
  kind?: string;
}

type Filter = "all" | "installed" | "available";

export default function MarketplaceClient() {
  const [plugins, setPlugins] = useState<Plugin[]>([]);
  const [loading, setLoading] = useState(true);
  const [q, setQ] = useState("");
  const [filter, setFilter] = useState<Filter>("all");
  const router = useRouter();

  function load() {
    setLoading(true);
    apiFetch<Plugin[]>("/api/marketplace")
      .then(setPlugins)
      .catch((e: Error) => notify.error("Marketplace failed", e.message))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
  }, []);

  const visible = useMemo(() => {
    const query = q.trim().toLowerCase();
    return plugins.filter((p) => {
      const installed = Boolean(p.installed ?? p.enabled);
      if (filter === "installed" && !installed) return false;
      if (filter === "available" && installed) return false;
      if (!query) return true;
      return (
        p.name.toLowerCase().includes(query) ||
        p.id.toLowerCase().includes(query) ||
        (p.description || "").toLowerCase().includes(query)
      );
    });
  }, [plugins, q, filter]);

  async function install(id: string) {
    try {
      await apiFetch(`/api/marketplace/${id}/install`, { method: "POST" });
      notify.success("Connector installed");
      load();
    } catch (e) {
      notify.error("Install failed", e instanceof Error ? e.message : undefined);
    }
  }

  async function uninstall(id: string) {
    try {
      await apiFetch(`/api/marketplace/${id}/uninstall`, { method: "POST" });
      notify.success("Connector uninstalled");
      load();
    } catch (e) {
      notify.error("Uninstall failed", e instanceof Error ? e.message : undefined);
    }
  }

  async function onUpload(file: File | null) {
    if (!file) return;
    try {
      const body = new FormData();
      body.append("file", file);
      const res = await fetch(`${API_URL}/api/marketplace/upload`, {
        method: "POST",
        credentials: "include",
        body,
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || res.statusText);
      }
      notify.success("Connector uploaded and installed");
      load();
    } catch (e) {
      notify.error("Upload failed", e instanceof Error ? e.message : undefined);
    }
  }

  return (
    <AppShell>
      <SetPageChrome
        title="Connector Marketplace"
        description="Install bundled or custom connector JARs, then create connections"
        action={
          <label className="inline-flex cursor-pointer items-center">
            <input
              type="file"
              accept=".jar"
              className="sr-only"
              onChange={(e) => onUpload(e.target.files?.[0] ?? null)}
            />
            <span className="inline-flex h-8 items-center rounded-full bg-primary px-3 text-sm font-medium text-primary-foreground">
              Upload JAR
            </span>
          </label>
        }
      />
      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center gap-2">
          <Input
            className="max-w-sm"
            placeholder="Search connectors…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
          {(["all", "installed", "available"] as Filter[]).map((f) => (
            <Button
              key={f}
              size="sm"
              variant={filter === f ? "default" : "outline"}
              className="rounded-full capitalize"
              onClick={() => setFilter(f)}
            >
              {f}
            </Button>
          ))}
        </div>
        {loading ? (
          <AppLoader />
        ) : (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {visible.map((p) => (
              <ConnectorCard
                key={p.id}
                {...p}
                installed={Boolean(p.installed ?? p.enabled)}
                onClasspath={p.onClasspath}
                onInstall={() => install(p.id)}
                onUninstall={() => uninstall(p.id)}
                onAdd={() => router.push(`/connections/new?plugin=${p.id}`)}
              />
            ))}
            <Card className="flex flex-col">
              <CardHeader>
                <CardTitle>Build your own</CardTitle>
                <CardDescription>
                  Implement the ConnectorPlugin SPI, package a JAR, and upload it here.
                </CardDescription>
              </CardHeader>
              <CardContent className="flex min-w-0 flex-col gap-2">
                <DocLink slug="adding-a-connector" />
                <p className="text-sm text-muted-foreground">
                  Open the guide in-app, or read the same file in the repo under{" "}
                  <code className="break-all">docs/connectors/</code>.
                </p>
              </CardContent>
            </Card>
          </div>
        )}
      </div>
    </AppShell>
  );
}
