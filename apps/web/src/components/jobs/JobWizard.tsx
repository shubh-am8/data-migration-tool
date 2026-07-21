"use client";

import { useState, useEffect, useMemo, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { OptionSelect } from "@/components/ui/option-select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { SchemaPicker } from "./SchemaPicker";
import { TablePicker } from "./TablePicker";
import { FilterBuilder, FilterRow } from "./FilterBuilder";
import { HotColdConfig } from "./HotColdConfig";
import { ConflictConfig } from "./ConflictConfig";
import { AlertConfig } from "./AlertConfig";
import { apiFetch } from "@/lib/api-client";
import { notify } from "@/lib/notify";
import { LiveLogTerminal } from "@/components/shared/LiveLogTerminal";
import { inferLevelFromText, type LogLine } from "@/lib/log-line";
import {
  canOpenJobWizardStep,
  validateJobWizardStep,
  type JobWizardState,
} from "@/lib/job-wizard-validation";
import {
  LAB_SCHEMAS,
  LAB_SIMULATION_TABLES,
  SIMULATION_SCENARIO_OPTIONS,
  simulationPreset,
  type SimulationScenario,
} from "@/lib/simulation-options";
import type { PageResponse } from "@/components/shared/PaginationBar";

interface JobWizardProps {
  jobId?: string;
  onComplete: () => void;
}

type TableEntry = { name: string; kind: string; partitioned: boolean; partitions: string[] };

export function JobWizard({ jobId, onComplete }: JobWizardProps) {
  const [step, setStep] = useState("1");
  const [stepError, setStepError] = useState<string | null>(null);
  const [connections, setConnections] = useState<Array<{ id: string; name: string }>>([]);
  const [name, setName] = useState("");
  const [runMode, setRunMode] = useState<"TEST" | "PRODUCTION">("TEST");
  const [simulate, setSimulate] = useState(false);
  const [simulationScenario, setSimulationScenario] = useState<SimulationScenario>("COLD_ONLY");
  const [sourceId, setSourceId] = useState("");
  const [destId, setDestId] = useState("");
  const [apiSchemas, setApiSchemas] = useState<string[]>([]);
  const [schema, setSchema] = useState("");
  const [tables, setTables] = useState<TableEntry[]>([]);
  const [table, setTable] = useState("");
  const [isPartition, setIsPartition] = useState(false);
  const [partitionName, setPartitionName] = useState("");
  const [columns, setColumns] = useState<Array<{ name: string; dataType: string }>>([]);
  const [filters, setFilters] = useState<FilterRow[]>([]);
  const [migrationMode, setMigrationMode] = useState("HOT_THEN_COLD");
  const [hotDays, setHotDays] = useState(7);
  const [tsColumn, setTsColumn] = useState("");
  const [rangeStart, setRangeStart] = useState("");
  const [rangeEndMode, setRangeEndMode] = useState("NOW");
  const [rangeEnd, setRangeEnd] = useState("");
  const [minChunkDurationHours, setMinChunkDurationHours] = useState(24);
  const [maxChunkDurationHours, setMaxChunkDurationHours] = useState(168);
  const [conflictColumns, setConflictColumns] = useState<string[]>([]);
  const [threadCount, setThreadCount] = useState(2);
  const [lifecycleAlertsEnabled, setLifecycleAlertsEnabled] = useState(true);
  const [progressIntervalMin, setProgressIntervalMin] = useState<number | "">(30);
  const [webhookOverride, setWebhookOverride] = useState("");
  const [preflight, setPreflight] = useState<{ recommendations?: Array<{ reason: string }> } | null>(null);
  const [testLines, setTestLines] = useState<LogLine[]>([]);
  const [testPassed, setTestPassed] = useState(false);
  const [testing, setTesting] = useState(false);

  const wizardState: JobWizardState = useMemo(
    () => ({
      name,
      runMode,
      simulate,
      simulationScenario,
      sourceId,
      destId,
      schema,
      table,
      tsColumn,
      migrationMode,
      rangeEndMode,
      rangeEnd,
      rangeStart,
    }),
    [
      name,
      runMode,
      simulate,
      simulationScenario,
      sourceId,
      destId,
      schema,
      table,
      tsColumn,
      migrationMode,
      rangeEndMode,
      rangeEnd,
      rangeStart,
    ]
  );

  const schemas = useMemo(() => {
    if (simulate && runMode === "TEST") {
      const merged = new Set([...apiSchemas, ...LAB_SCHEMAS]);
      return [...merged];
    }
    return apiSchemas;
  }, [apiSchemas, simulate, runMode]);

  const connectionOptions = useMemo(
    () => connections.map((c) => ({ value: c.id, label: c.name })),
    [connections]
  );

  const applySimulationPreset = useCallback((scenario: SimulationScenario) => {
    const preset = simulationPreset(scenario);
    setSchema(preset.schema);
    setTable(preset.table);
    setTables(LAB_SIMULATION_TABLES);
    setTsColumn(preset.table === "orders_cold" ? "created_at" : "updated_at");
  }, []);

  useEffect(() => {
    apiFetch<PageResponse<{ id: string; name: string }> | Array<{ id: string; name: string }>>(
      "/api/connections?page=0&size=100"
    )
      .then((r) => setConnections(Array.isArray(r) ? r : r.content ?? []))
      .catch((e: Error) => notify.error("Failed to load connections", e.message));
  }, []);

  useEffect(() => {
    if (!sourceId) {
      setApiSchemas([]);
      return;
    }
    apiFetch<{ schemas: string[] }>(`/api/connections/${sourceId}/schemas`)
      .then((r) => setApiSchemas(r.schemas))
      .catch(() => setApiSchemas([]));
  }, [sourceId]);

  useEffect(() => {
    if (simulate && runMode === "TEST") {
      applySimulationPreset(simulationScenario);
      return;
    }
    if (!sourceId || !schema) return;
    apiFetch<{ tables: TableEntry[] }>(`/api/connections/${sourceId}/schemas/${schema}/tables`)
      .then((r) => setTables(r.tables))
      .catch(() => setTables([]));
  }, [sourceId, schema, simulate, runMode, simulationScenario, applySimulationPreset]);

  useEffect(() => {
    if (simulate && runMode === "TEST") return;
    if (!sourceId || !schema || !table) return;
    apiFetch<{ columns: typeof columns }>(
      `/api/connections/${sourceId}/schemas/${schema}/tables/${table}/columns`
    )
      .then((r) => {
        setColumns(r.columns);
        setConflictColumns(
          r.columns.filter((c) => c.name.endsWith("_id") || c.name === "id").map((c) => c.name)
        );
      })
      .catch(console.error);
  }, [sourceId, schema, table, simulate, runMode]);

  function attemptStepChange(next: string) {
    const gate = canOpenJobWizardStep(next, wizardState);
    if (!gate.ok) {
      setStepError(gate.message);
      notify.warning("Complete earlier steps first", gate.message);
      return;
    }
    setStepError(null);
    setStep(next);
  }

  function goNext() {
    const gate = validateJobWizardStep(step, wizardState);
    if (!gate.ok) {
      setStepError(gate.message);
      notify.warning("Complete this step first", gate.message);
      return;
    }
    setStepError(null);
    setStep(String(Number(step) + 1));
  }

  function toInstant(value: string): string | null {
    if (!value) return null;
    return new Date(value).toISOString();
  }

  function simulationConfigJson() {
    if (!simulate || runMode !== "TEST") return undefined;
    return simulationPreset(simulationScenario).configJson;
  }

  async function saveJob() {
    const gate = validateJobWizardStep("4", wizardState);
    if (!gate.ok) {
      notify.warning("Complete required fields", gate.message);
      return;
    }
    const configJson = simulationConfigJson();
    const body = {
      name,
      runMode,
      sourceConnectionId: sourceId,
      destConnectionId: destId,
      migrationMode,
      threadCount,
      hotDays,
      tsColumn,
      schemaName: schema,
      sourceTable: table,
      isPartition,
      partitionName,
      conflictColumns,
      filters,
      rangeStart: toInstant(rangeStart),
      rangeEndMode,
      rangeEnd: rangeEndMode === "FIXED" ? toInstant(rangeEnd) : null,
      minChunkDurationHours,
      maxChunkDurationHours,
      lifecycleAlertsEnabled,
      progressIntervalMin: progressIntervalMin || null,
      gspaceWebhookOverride: webhookOverride || null,
      ...(configJson ? { configJson } : {}),
    };
    if (jobId) {
      await apiFetch(`/api/jobs/${jobId}`, { method: "PUT", body: JSON.stringify(body) });
    } else {
      await apiFetch("/api/jobs", { method: "POST", body: JSON.stringify(body) });
    }
    notify.success("Job saved");
    onComplete();
  }

  async function runTestJob() {
    if (!sourceId || !destId || !schema || !table) {
      notify.warning("Select source, destination, schema, and table first");
      return;
    }
    setTesting(true);
    setTestPassed(false);
    setTestLines([]);
    const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
    try {
      const res = await fetch(`${API_URL}/api/jobs/test`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
        body: JSON.stringify({
          sourceConnectionId: sourceId,
          destConnectionId: destId,
          schema,
          table: isPartition && partitionName ? partitionName : table,
          limit: 5,
        }),
      });
      if (!res.ok || !res.body) {
        notify.error("Test failed", `HTTP ${res.status}`);
        return;
      }
      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let buf = "";
      while (true) {
        const { value, done } = await reader.read();
        if (done) break;
        buf += decoder.decode(value, { stream: true });
        const parts = buf.split("\n\n");
        buf = parts.pop() || "";
        for (const part of parts) {
          const dataLine = part.split("\n").find((l) => l.startsWith("data:"));
          if (!dataLine) continue;
          try {
            const json = JSON.parse(dataLine.slice(5).trim()) as { line?: string; status?: string };
            if (json.line) {
              const level =
                json.status === "failed"
                  ? "error"
                  : json.status === "passed"
                    ? "success"
                    : inferLevelFromText(json.line);
              setTestLines((l) => [...l, { text: json.line!, level }]);
            }
            if (json.status === "passed") {
              setTestPassed(true);
              notify.success("Test passed — truncate test rows before creating the job");
            }
            if (json.status === "failed") notify.error("Test failed");
          } catch {
            /* ignore parse errors */
          }
        }
      }
    } catch (e) {
      notify.error("Test error", e instanceof Error ? e.message : undefined);
    } finally {
      setTesting(false);
    }
  }

  async function runPreflight(id: string) {
    const result = await apiFetch<{ recommendations: Array<{ reason: string }> }>(
      `/api/jobs/${id}/preflight`,
      { method: "POST" }
    );
    setPreflight(result);
  }

  return (
    <Tabs value={step} onValueChange={attemptStepChange}>
      <TabsList className="flex h-auto flex-wrap">
        <TabsTrigger value="1">Source & Dest</TabsTrigger>
        <TabsTrigger value="2">Schema & Table</TabsTrigger>
        <TabsTrigger value="3">Filters</TabsTrigger>
        <TabsTrigger value="4">Hot/Cold & Conflict</TabsTrigger>
        <TabsTrigger value="5">Alerts & Preflight</TabsTrigger>
      </TabsList>

      {stepError ? (
        <Alert variant="destructive" className="mt-4">
          <AlertTitle>Step incomplete</AlertTitle>
          <AlertDescription>{stepError}</AlertDescription>
        </Alert>
      ) : null}

      <TabsContent value="1" className="flex flex-col gap-4 pt-4">
        <FieldGroup>
          <Field>
            <FieldLabel>Job Name</FieldLabel>
            <Input value={name} onChange={(e) => setName(e.target.value)} />
          </Field>
          <Field>
            <FieldLabel>Run Mode</FieldLabel>
            <ToggleGroup
              variant="outline"
              value={[runMode]}
              onValueChange={(value) => {
                const next = value[0];
                if (next === "TEST" || next === "PRODUCTION") {
                  setRunMode(next);
                  if (next === "PRODUCTION") setSimulate(false);
                }
              }}
            >
              <ToggleGroupItem value="TEST">Test</ToggleGroupItem>
              <ToggleGroupItem value="PRODUCTION">Production</ToggleGroupItem>
            </ToggleGroup>
          </Field>
          {runMode === "PRODUCTION" && (
            <Alert variant="destructive">
              <AlertTitle>This job will run against production data</AlertTitle>
              <AlertDescription>
                Source and destination must be non-sandbox connections. Simulation seeding is unavailable in Production.
              </AlertDescription>
            </Alert>
          )}
          {runMode === "TEST" && (
            <Field>
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={simulate}
                  onChange={(e) => {
                    const on = e.target.checked;
                    setSimulate(on);
                    if (on) applySimulationPreset(simulationScenario);
                  }}
                />
                Seed sample data job
              </label>
              {simulate && (
                <>
                  <OptionSelect
                    value={simulationScenario}
                    onValueChange={(v) => {
                      setSimulationScenario(v);
                      applySimulationPreset(v);
                    }}
                    options={SIMULATION_SCENARIO_OPTIONS}
                    placeholder="Select seeding pattern"
                  />
                  <FieldDescription>
                    Uses lab schemas ({LAB_SCHEMAS.join(", ")}) and sample tables from Lab Dev Tools.
                  </FieldDescription>
                </>
              )}
            </Field>
          )}
          <Field>
            <FieldLabel>Source</FieldLabel>
            <OptionSelect
              value={sourceId}
              onValueChange={setSourceId}
              options={connectionOptions}
              placeholder="Select source"
            />
          </Field>
          <Field>
            <FieldLabel>Destination</FieldLabel>
            <OptionSelect
              value={destId}
              onValueChange={setDestId}
              options={connectionOptions}
              placeholder="Select destination"
            />
          </Field>
          <Field>
            <FieldLabel>Threads</FieldLabel>
            <Input type="number" value={threadCount} onChange={(e) => setThreadCount(Number(e.target.value))} />
          </Field>
        </FieldGroup>
        <Button onClick={goNext}>Next</Button>
      </TabsContent>

      <TabsContent value="2" className="flex flex-col gap-4 pt-4">
        {simulate && runMode === "TEST" ? (
          <Alert>
            <AlertTitle>Simulation job</AlertTitle>
            <AlertDescription>
              Schema <strong>{schema}</strong> and table <strong>{table}</strong> are set from your seeding pattern.
              Install Lab Dev Tools from the Marketplace if tables are missing.
            </AlertDescription>
          </Alert>
        ) : (
          <>
            <SchemaPicker schemas={schemas} value={schema} onChange={setSchema} />
            <TablePicker
              tables={tables}
              selected={table}
              usePartition={isPartition}
              partitionName={partitionName}
              onSelectTable={setTable}
              onTogglePartition={setIsPartition}
              onSelectPartition={setPartitionName}
            />
          </>
        )}
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => attemptStepChange("1")}>
            Back
          </Button>
          <Button onClick={goNext}>Next</Button>
        </div>
      </TabsContent>

      <TabsContent value="3" className="flex flex-col gap-4 pt-4">
        <FilterBuilder columns={columns} filters={filters} onChange={setFilters} />
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => attemptStepChange("2")}>
            Back
          </Button>
          <Button onClick={goNext}>Next</Button>
        </div>
      </TabsContent>

      <TabsContent value="4" className="flex flex-col gap-4 pt-4">
        <HotColdConfig
          migrationMode={migrationMode}
          hotDays={hotDays}
          tsColumn={tsColumn}
          rangeStart={rangeStart}
          rangeEndMode={rangeEndMode}
          rangeEnd={rangeEnd}
          minChunkDurationHours={minChunkDurationHours}
          maxChunkDurationHours={maxChunkDurationHours}
          onChange={(p) => {
            if (p.migrationMode !== undefined) setMigrationMode(p.migrationMode);
            if (p.hotDays !== undefined) setHotDays(p.hotDays);
            if (p.tsColumn !== undefined) setTsColumn(p.tsColumn);
            if (p.rangeStart !== undefined) setRangeStart(p.rangeStart);
            if (p.rangeEndMode !== undefined) setRangeEndMode(p.rangeEndMode);
            if (p.rangeEnd !== undefined) setRangeEnd(p.rangeEnd);
            if (p.minChunkDurationHours !== undefined) setMinChunkDurationHours(p.minChunkDurationHours);
            if (p.maxChunkDurationHours !== undefined) setMaxChunkDurationHours(p.maxChunkDurationHours);
          }}
        />
        <ConflictConfig
          columns={columns.map((c) => c.name)}
          selected={conflictColumns}
          onChange={setConflictColumns}
        />
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => attemptStepChange("3")}>
            Back
          </Button>
          <Button onClick={goNext}>Next</Button>
        </div>
      </TabsContent>

      <TabsContent value="5" className="flex flex-col gap-4 pt-4">
        <AlertConfig
          lifecycleEnabled={lifecycleAlertsEnabled}
          progressIntervalMin={progressIntervalMin}
          webhookOverride={webhookOverride}
          onChange={(p) => {
            if (p.lifecycleEnabled !== undefined) setLifecycleAlertsEnabled(p.lifecycleEnabled);
            if (p.progressIntervalMin !== undefined) setProgressIntervalMin(p.progressIntervalMin);
            if (p.webhookOverride !== undefined) setWebhookOverride(p.webhookOverride);
          }}
        />
        {preflight?.recommendations?.map((r, i) => (
          <Alert key={i}>
            <AlertTitle>Index recommendation</AlertTitle>
            <AlertDescription>{r.reason}</AlertDescription>
          </Alert>
        ))}
        <div className="flex flex-col gap-2">
          <Button variant="info" disabled={testing} onClick={runTestJob}>
            {testing ? "Testing…" : "Test Job"}
          </Button>
          <LiveLogTerminal
            lines={testLines}
            status={testing ? "running" : testPassed ? "passed" : testLines.length ? "idle" : "idle"}
          />
          {testPassed && (
            <Alert>
              <AlertTitle>Delete test rows before create</AlertTitle>
              <AlertDescription>
                Truncate or delete any sandboxed test rows on the destination table, then save the job.
              </AlertDescription>
            </Alert>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => attemptStepChange("4")}>
            Back
          </Button>
          <Button variant="success" onClick={saveJob} disabled={!jobId && !testPassed}>
            {jobId ? "Save Job" : "Add Job"}
          </Button>
          {jobId && (
            <Button variant="outline" onClick={() => runPreflight(jobId)}>
              Run Preflight
            </Button>
          )}
        </div>
      </TabsContent>
    </Tabs>
  );
}
