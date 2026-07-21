"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { LiveLogTerminal } from "@/components/shared/LiveLogTerminal";
import { Button, buttonVariants } from "@/components/ui/button";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { canSaveConnection, type ConnectionTestStatus } from "@/lib/connection-form-gate";
import { notifyConnectionTestResult, type ConnectionTestResult } from "@/lib/connection-test";
import { linesFromConnectionTest, type LogLine } from "@/lib/log-line";
import { cn } from "@/lib/utils";

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
  /** When true, existing connection is trusted until config fields change. */
  trustExisting?: boolean;
  cancelHref?: string;
  onSubmit: (values: ConnectionFormValues) => void | Promise<void>;
  onTest?: (values: ConnectionFormValues) => Promise<ConnectionTestResult>;
}

function buildDefaults(fields: ConfigFieldDef[], initial?: Partial<ConnectionFormValues>): ConnectionFormValues {
  const values: ConnectionFormValues = { name: "", minPoolSize: "1", maxPoolSize: "10" };
  for (const f of fields) values[f.key] = f.defaultValue ?? "";
  for (const [k, v] of Object.entries(initial ?? {})) {
    if (v !== undefined) values[k] = v;
  }
  return values;
}

function configFingerprint(values: ConnectionFormValues, fields: ConfigFieldDef[]): string {
  const keys = ["minPoolSize", "maxPoolSize", ...fields.map((f) => f.key)];
  return keys.map((k) => `${k}=${values[k] ?? ""}`).join("|");
}

/**
 * Renders connector fields driven by the plugin's `configFields()` SPI metadata.
 * ponytail: sandbox is always true server-side — not exposed in UI.
 */
export function ConnectionForm({
  fields,
  initial,
  trustExisting = false,
  cancelHref = "/connections",
  onSubmit,
  onTest,
}: ConnectionFormProps) {
  const [values, setValues] = useState<ConnectionFormValues>(() => buildDefaults(fields, initial));
  const [testing, setTesting] = useState(false);
  const [saving, setSaving] = useState(false);
  const [logLines, setLogLines] = useState<LogLine[]>([]);
  const [testStatus, setTestStatus] = useState<ConnectionTestStatus>(trustExisting ? "passed" : "idle");
  const [passedFingerprint, setPassedFingerprint] = useState<string | null>(
    trustExisting ? configFingerprint(buildDefaults(fields, initial), fields) : null
  );

  const initialFingerprint = useMemo(
    () => configFingerprint(buildDefaults(fields, initial), fields),
    [fields, initial]
  );
  const currentFingerprint = useMemo(() => configFingerprint(values, fields), [values, fields]);
  const needsTest = !trustExisting || currentFingerprint !== initialFingerprint;
  const canSave = canSaveConnection(needsTest, testStatus, passedFingerprint, currentFingerprint);

  function update(key: string, value: string) {
    setValues((v) => ({ ...v, [key]: value }));
  }

  async function runTest() {
    if (!onTest) return;
    setTesting(true);
    setTestStatus("running");
    setLogLines([{ text: "Testing connection…", level: "info" }]);
    try {
      const result = await onTest(values);
      setLogLines(linesFromConnectionTest(result));
      setTestStatus(result.success ? "passed" : "failed");
      if (result.success) setPassedFingerprint(currentFingerprint);
      notifyConnectionTestResult(result);
    } catch (e) {
      const message = e instanceof Error ? e.message : String(e);
      setLogLines([
        { text: message, level: "error" },
        { text: "✗ Test failed", level: "error" },
      ]);
      setTestStatus("failed");
    } finally {
      setTesting(false);
    }
  }

  return (
    <div className="flex min-w-0 flex-col gap-6">
      <FieldGroup className="grid grid-cols-1 gap-5 md:grid-cols-2">
        <Field className="md:col-span-2">
          <FieldLabel htmlFor="name">Connection Name</FieldLabel>
          <Input id="name" value={values.name ?? ""} onChange={(e) => update("name", e.target.value)} />
        </Field>
        {fields.map((f) => (
          <Field key={f.key} className={f.type === "password" ? "md:col-span-2" : undefined}>
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
      </FieldGroup>

      {onTest ? (
        <LiveLogTerminal
          lines={logLines}
          status={testing ? "running" : testStatus === "idle" ? undefined : testStatus}
          className="min-w-0"
        />
      ) : null}

      <div className="flex flex-wrap gap-2">
        <Link href={cancelHref} className={cn(buttonVariants({ variant: "outline" }))}>
          Cancel
        </Link>
        {onTest ? (
          <Button type="button" variant="info" disabled={testing || saving} onClick={runTest}>
            {testing ? "Testing…" : "Test Connection"}
          </Button>
        ) : null}
        <Button
          type="button"
          variant="success"
          disabled={!canSave || saving || testing}
          title={
            !canSave
              ? needsTest
                ? "Run a successful connection test before saving"
                : "Fix the failed connection test before saving"
              : undefined
          }
          onClick={async () => {
            setSaving(true);
            try {
              await onSubmit(values);
            } finally {
              setSaving(false);
            }
          }}
        >
          {saving ? "Saving…" : "Save"}
        </Button>
      </div>
    </div>
  );
}
