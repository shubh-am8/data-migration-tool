"use client";

import { Button } from "@/components/ui/button";
import { Field, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { PlusIcon, Trash2Icon } from "lucide-react";

export interface IpWhitelistEntry {
  label: string;
  ip: string;
}

export function parseIpWhitelist(raw: string): IpWhitelistEntry[] {
  if (!raw || !raw.trim()) return [];
  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return [];
    return parsed.map((item) => {
      if (typeof item === "string") return { label: "", ip: item };
      if (item && typeof item === "object") {
        const o = item as Record<string, unknown>;
        return {
          label: String(o.label ?? ""),
          ip: String(o.ip ?? o.cidr ?? ""),
        };
      }
      return { label: "", ip: "" };
    }).filter((e) => e.ip.trim() !== "");
  } catch {
    return [];
  }
}

export function serializeIpWhitelist(entries: IpWhitelistEntry[]): string {
  return JSON.stringify(
    entries
      .filter((e) => e.ip.trim() !== "")
      .map((e) => ({ label: e.label.trim(), ip: e.ip.trim() }))
  );
}

interface IpWhitelistEditorProps {
  value: string;
  onChange: (json: string) => void;
}

export function IpWhitelistEditor({ value, onChange }: IpWhitelistEditorProps) {
  const entries = parseIpWhitelist(value);

  function update(next: IpWhitelistEntry[]) {
    onChange(serializeIpWhitelist(next));
  }

  return (
    <div className="flex flex-col gap-3">
      {entries.length === 0 && (
        <p className="text-sm text-muted-foreground">
          No IPs yet. Add a labeled address or CIDR to allow access.
        </p>
      )}
      {entries.map((entry, index) => (
        <div key={index} className="flex flex-wrap items-end gap-2">
          <Field className="min-w-[10rem] flex-1">
            <FieldLabel>Label</FieldLabel>
            <Input
              value={entry.label}
              placeholder="Office VPN"
              onChange={(e) => {
                const next = [...entries];
                next[index] = { ...entry, label: e.target.value };
                update(next);
              }}
            />
          </Field>
          <Field className="min-w-[12rem] flex-1">
            <FieldLabel>IP / CIDR</FieldLabel>
            <Input
              value={entry.ip}
              placeholder="203.0.113.9 or 10.0.0.0/8"
              onChange={(e) => {
                const next = [...entries];
                next[index] = { ...entry, ip: e.target.value };
                update(next);
              }}
            />
          </Field>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            aria-label="Remove IP"
            onClick={() => update(entries.filter((_, i) => i !== index))}
          >
            <Trash2Icon data-icon="inline-start" />
            Remove
          </Button>
        </div>
      ))}
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="w-fit"
        onClick={() => update([...entries, { label: "", ip: "" }])}
      >
        <PlusIcon data-icon="inline-start" />
        Add IP
      </Button>
    </div>
  );
}
