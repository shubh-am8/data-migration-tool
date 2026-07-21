"use client";

import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { LiveLogTerminal } from "@/components/shared/LiveLogTerminal";
import { apiFetch } from "@/lib/api-client";
import { notify } from "@/lib/notify";
import type { LogLine } from "@/lib/log-line";

interface ConflictConfigProps {
  columns: string[];
  selected: string[];
  onChange: (cols: string[]) => void;
  sourceConnectionId: string;
  schemaName: string;
  sourceTable: string;
  verified: boolean;
  onVerifiedChange: (verified: boolean) => void;
}

export function ConflictConfig({
  columns,
  selected,
  onChange,
  sourceConnectionId,
  schemaName,
  sourceTable,
  verified,
  onVerifiedChange,
}: ConflictConfigProps) {
  const [verifying, setVerifying] = useState(false);
  const [logLines, setLogLines] = useState<LogLine[]>([]);

  function toggle(col: string) {
    onVerifiedChange(false);
    onChange(selected.includes(col) ? selected.filter((c) => c !== col) : [...selected, col]);
  }

  async function verify() {
    if (!sourceConnectionId || !schemaName || !sourceTable) {
      notify.warning("Select source connection, schema, and table first");
      return;
    }
    if (selected.length === 0) {
      notify.warning("Select at least one ON CONFLICT column");
      return;
    }
    setVerifying(true);
    setLogLines([]);
    try {
      const result = await apiFetch<{
        verified: boolean;
        matchedIndex?: string;
        logs?: string[];
        suggestedSql?: string;
        error?: string;
      }>("/api/jobs/verify-conflict-index", {
        method: "POST",
        body: JSON.stringify({
          sourceConnectionId,
          schemaName,
          sourceTable,
          conflictColumns: selected,
        }),
      });
      const lines: LogLine[] = (result.logs ?? []).map((text) => ({ text, level: "info" as const }));
      if (result.error) lines.push({ text: result.error, level: "error" });
      if (result.verified && result.matchedIndex) {
        lines.push({ text: `Verified — using index ${result.matchedIndex}`, level: "success" });
      }
      setLogLines(lines);
      onVerifiedChange(Boolean(result.verified));
      if (result.verified) {
        notify.success("Unique index verified");
      } else {
        notify.warning("No matching unique index — see log for SQL");
      }
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Verification failed";
      setLogLines([{ text: msg, level: "error" }]);
      onVerifiedChange(false);
      notify.error("Verification failed", msg);
    } finally {
      setVerifying(false);
    }
  }

  return (
    <FieldGroup>
      <Field>
        <FieldLabel>ON CONFLICT columns</FieldLabel>
        <p className="mb-2 text-xs text-muted-foreground">
          Hot phase: DO UPDATE. Cold phase: DO NOTHING. Verify a matching unique index exists before continuing.
        </p>
        <div className="flex flex-wrap gap-2">
          {columns.map((col) => (
            <Badge
              key={col}
              variant={selected.includes(col) ? "default" : "outline"}
              className="cursor-pointer"
              onClick={() => toggle(col)}
            >
              {col}
            </Badge>
          ))}
        </div>
      </Field>
      <div className="flex flex-wrap items-center gap-2">
        <Button type="button" variant="outline" onClick={() => void verify()} disabled={verifying}>
          {verifying ? "Verifying…" : "Verify unique index"}
        </Button>
        {verified ? (
          <Badge variant="secondary" className="bg-emerald-600/15 text-emerald-700">
            Verified
          </Badge>
        ) : (
          <Badge variant="secondary">Not verified</Badge>
        )}
      </div>
      {logLines.length > 0 && (
        <LiveLogTerminal lines={logLines} status={verified ? "passed" : verifying ? "running" : "failed"} />
      )}
    </FieldGroup>
  );
}
