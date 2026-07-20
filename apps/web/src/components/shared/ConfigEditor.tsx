"use client";

import { Badge } from "@/components/ui/badge";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { IpWhitelistEditor } from "@/components/shared/IpWhitelistEditor";

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
  onHide: (key: string) => void;
  saveError?: string | null;
}

const LABELS: Record<string, string> = {
  ip_whitelist_mode: "IP whitelist mode",
  ip_whitelist: "Allowed IPs",
  min_threads_per_job: "Min threads per job",
  max_threads_per_job: "Max threads per job",
  gspace_webhook_url: "GSpace webhook URL",
  google_client_id: "Google Client ID",
  google_client_secret: "Google Client Secret",
  allowed_email_domain: "Allowed email domain",
};

function sourceLabel(source: string) {
  switch (source) {
    case "DASHBOARD":
      return "dashboard";
    case "ENV":
      return "env";
    default:
      return "default";
  }
}

function isRestricted(config: Record<string, ConfigEntry>) {
  const mode = (config.ip_whitelist_mode?.value ?? "OPEN").trim().toUpperCase();
  return mode === "RESTRICTED";
}

export function ConfigEditor({
  config,
  onChange,
  onSave,
  onReveal,
  onHide,
  saveError,
}: ConfigEditorProps) {
  const restricted = isRestricted(config);

  return (
    <FieldGroup>
      {Object.entries(config).map(([key, entry]) => {
        if (key === "ip_whitelist" && !restricted) return null;

        return (
          <Field key={key}>
            <div className="flex flex-wrap items-center gap-2">
              <FieldLabel>{LABELS[key] || key}</FieldLabel>
              <Badge variant="secondary" className="rounded-full">
                {sourceLabel(entry.source)}
              </Badge>
              {entry.sensitive && (
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => (entry.masked ? onReveal(key) : onHide(key))}
                >
                  {entry.masked ? "Show" : "Hide"}
                </Button>
              )}
              {entry.restartRequired && (
                <Badge variant="outline" className="rounded-full">
                  Restart required
                </Badge>
              )}
            </div>

            {key === "ip_whitelist_mode" ? (
              <Select
                value={(entry.value || "OPEN").toUpperCase()}
                onValueChange={(v) => onChange(key, v ?? "OPEN")}
              >
                <SelectTrigger className="w-full max-w-xs">
                  <SelectValue placeholder="Mode" />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    <SelectItem value="OPEN">OPEN — allow all IPs</SelectItem>
                    <SelectItem value="RESTRICTED">RESTRICTED — allow listed IPs</SelectItem>
                  </SelectGroup>
                </SelectContent>
              </Select>
            ) : key === "ip_whitelist" ? (
              <IpWhitelistEditor value={entry.value || "[]"} onChange={(v) => onChange(key, v)} />
            ) : (
              <Input
                type={entry.sensitive && entry.masked ? "password" : "text"}
                value={entry.value}
                onChange={(e) => onChange(key, e.target.value)}
                autoComplete="off"
              />
            )}
          </Field>
        );
      })}
      {saveError && <p className="text-sm text-destructive">{saveError}</p>}
      <Button onClick={onSave} variant="pill">
        Save Configuration
      </Button>
    </FieldGroup>
  );
}
