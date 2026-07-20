"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ConnectorCard } from "@/components/connectors/ConnectorCard";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { apiFetch } from "@/lib/api-client";

interface Plugin {
  id: string;
  name: string;
  description: string;
  version: string;
}

export function MarketplaceSection() {
  const [plugins, setPlugins] = useState<Plugin[]>([]);
  const router = useRouter();

  useEffect(() => {
    apiFetch<Plugin[]>("/api/marketplace").then(setPlugins).catch(console.error);
  }, []);

  if (plugins.length === 0) return null;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Connector Marketplace</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid gap-4 md:grid-cols-2">
          {plugins.map((p) => (
            <ConnectorCard
              key={p.id}
              {...p}
              onAdd={() => router.push(`/connections/new?plugin=${p.id}`)}
            />
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
