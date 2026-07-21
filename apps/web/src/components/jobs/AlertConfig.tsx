"use client";

import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { NumberInput } from "@/components/ui/number-input";

interface AlertConfigProps {
  lifecycleEnabled: boolean;
  progressIntervalMin: number | "";
  webhookOverride: string;
  onChange: (patch: Partial<{ lifecycleEnabled: boolean; progressIntervalMin: number | ""; webhookOverride: string }>) => void;
}

export function AlertConfig({ lifecycleEnabled, progressIntervalMin, webhookOverride, onChange }: AlertConfigProps) {
  return (
    <FieldGroup>
      <Field>
        <FieldLabel>GSpace lifecycle alerts</FieldLabel>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={lifecycleEnabled} onChange={(e) => onChange({ lifecycleEnabled: e.target.checked })} />
          Notify on started / paused / resumed / completed / failed
        </label>
      </Field>
      <Field>
        <FieldLabel>Progress alert interval (minutes)</FieldLabel>
        <NumberInput
          min={1}
          value={typeof progressIntervalMin === "number" ? progressIntervalMin : 0}
          onValueChange={(v) => onChange({ progressIntervalMin: v > 0 ? v : "" })}
        />
      </Field>
      <Field>
        <FieldLabel>GSpace webhook override</FieldLabel>
        <Input value={webhookOverride} placeholder="Uses app config if empty"
          onChange={(e) => onChange({ webhookOverride: e.target.value })} />
      </Field>
    </FieldGroup>
  );
}
