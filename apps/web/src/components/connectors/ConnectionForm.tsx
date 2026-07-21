"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Field, FieldContent, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";

export interface ConfigFieldDef {
  key: string;
  label: string;
  type: string;
  required: boolean;
  defaultValue?: string | null;
}

export type ConnectionFormValues = Record<string, string>;

interface ConnectionFormProps {
  fields: ConfigFieldDef[];
  initial?: Partial<ConnectionFormValues>;
  initialSandbox?: boolean;
  onSubmit: (values: ConnectionFormValues, sandbox: boolean) => void;
  onTest?: (values: ConnectionFormValues) => Promise<void>;
}

function buildDefaults(fields: ConfigFieldDef[], initial?: Partial<ConnectionFormValues>): ConnectionFormValues {
  const values: ConnectionFormValues = { name: "", minPoolSize: "1", maxPoolSize: "10" };
  for (const f of fields) values[f.key] = f.defaultValue ?? "";
  for (const [k, v] of Object.entries(initial ?? {})) {
    if (v !== undefined) values[k] = v;
  }
  return values;
}

/**
 * Renders connector fields driven by the plugin's `configFields()` SPI metadata (host, port,
 * credentials, etc. differ per connector) plus the generic pool-size fields every connector
 * supports. Pass `key={pluginId}` from the caller so switching connectors resets the form.
 *
 * ponytail: the SDK `ConfigField` has no enum `options` yet, so `type: "select"` (e.g. sslmode)
 * renders as free text. Upgrade path: add `options: string[]` to ConfigField and render a Select.
 */
export function ConnectionForm({ fields, initial, initialSandbox, onSubmit, onTest }: ConnectionFormProps) {
  const [values, setValues] = useState<ConnectionFormValues>(() => buildDefaults(fields, initial));
  const [sandbox, setSandbox] = useState(initialSandbox ?? false);
  const [testing, setTesting] = useState(false);

  function update(key: string, value: string) {
    setValues((v) => ({ ...v, [key]: value }));
  }

  return (
    <FieldGroup>
      <Field>
        <FieldLabel htmlFor="name">Connection Name</FieldLabel>
        <Input id="name" value={values.name ?? ""} onChange={(e) => update("name", e.target.value)} />
      </Field>
      {fields.map((f) => (
        <Field key={f.key}>
          <FieldLabel htmlFor={f.key}>
            {f.label}
            {f.required ? " *" : ""}
          </FieldLabel>
          <Input
            id={f.key}
            type={f.type === "password" ? "password" : f.type === "number" ? "number" : "text"}
            required={f.required}
            value={values[f.key] ?? ""}
            onChange={(e) => update(f.key, e.target.value)}
          />
        </Field>
      ))}
      <Field>
        <FieldLabel htmlFor="minPoolSize">Min pool size</FieldLabel>
        <Input
          id="minPoolSize"
          type="number"
          min={1}
          value={values.minPoolSize ?? "1"}
          onChange={(e) => update("minPoolSize", e.target.value)}
        />
      </Field>
      <Field>
        <FieldLabel htmlFor="maxPoolSize">Max pool size (default 10)</FieldLabel>
        <Input
          id="maxPoolSize"
          type="number"
          min={1}
          max={100}
          value={values.maxPoolSize ?? "10"}
          onChange={(e) => update("maxPoolSize", e.target.value)}
        />
      </Field>
      <Field orientation="horizontal">
        <FieldContent>
          <FieldLabel htmlFor="sandbox">Sandbox connection</FieldLabel>
          <FieldDescription>Required for Test-mode migration jobs.</FieldDescription>
        </FieldContent>
        <Switch id="sandbox" checked={sandbox} onCheckedChange={(checked) => setSandbox(Boolean(checked))} />
      </Field>
      <div className="flex gap-2">
        {onTest && (
          <Button
            type="button"
            variant="info"
            disabled={testing}
            onClick={async () => {
              setTesting(true);
              try {
                await onTest(values);
              } finally {
                setTesting(false);
              }
            }}
          >
            {testing ? "Testing…" : "Test Connection"}
          </Button>
        )}
        <Button type="button" variant="success" onClick={() => onSubmit(values, sandbox)}>
          Save
        </Button>
      </div>
    </FieldGroup>
  );
}
