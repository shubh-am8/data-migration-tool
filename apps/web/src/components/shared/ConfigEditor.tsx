"use client";

import { Badge } from "@/components/ui/badge";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

export interface ConfigEntry {
  value: string;
  source: string;
  updatedAt?: string;
  sensitive: boolean;
  masked: boolean;
  restartRequired: boolean;
}

interface ConfigEditorProps {
  config: Record<string, ConfigEntry>;
  onChange: (key: string, value: string) => void;
  onSave: () => void;
  onReveal: (key: string) => void;
  saveError?: string | null;
}

const LABELS: Record<string, string> = {
  min_threads_per_job: "Min threads per job",
  max_threads_per_job: "Max threads per job",
  gspace_webhook_url: "GSpace webhook URL",
  google_client_id: "Google Client ID",
  google_client_secret: "Google Client Secret",
  allowed_email_domain: "Allowed email domain",
};

function sourceLabel(source: string) {
  switch (source) {
    case "DASHBOARD": return "dashboard";
    case "ENV": return "env";
    default: return "default";
  }
}

export function ConfigEditor({ config, onChange, onSave, onReveal, saveError }: ConfigEditorProps) {
  return (
    <FieldGroup>
      {Object.entries(config).map(([key, entry]) => (
        <Field key={key}>
          <div className="flex items-center gap-2">
            <FieldLabel>{LABELS[key] || key}</FieldLabel>
            <Badge variant="secondary">{sourceLabel(entry.source)}</Badge>
            {entry.sensitive && (
              <Button type="button" variant="ghost" onClick={() => onReveal(key)}>
                {entry.masked ? "Show" : "Hide"}
              </Button>
            )}
            {entry.restartRequired && <Badge variant="outline">Restart required</Badge>}
          </div>
          <Input value={entry.value} onChange={(e) => onChange(key, e.target.value)} />
        </Field>
      ))}
      {saveError && <p className="text-sm text-destructive">{saveError}</p>}
      <Button onClick={onSave}>Save Configuration</Button>
    </FieldGroup>
  );
}
