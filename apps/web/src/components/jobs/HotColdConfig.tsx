"use client";

import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { OptionSelect } from "@/components/ui/option-select";
import { MIGRATION_MODE_OPTIONS, RANGE_END_MODE_OPTIONS } from "@/lib/migration-mode-options";

export interface HotColdConfigPatch {
  migrationMode?: string;
  hotDays?: number;
  tsColumn?: string;
  rangeStart?: string;
  rangeEndMode?: string;
  rangeEnd?: string;
  minChunkDurationHours?: number;
  maxChunkDurationHours?: number;
}

interface HotColdConfigProps {
  migrationMode: string;
  hotDays: number;
  tsColumn: string;
  rangeStart: string;
  rangeEndMode: string;
  rangeEnd: string;
  minChunkDurationHours: number;
  maxChunkDurationHours: number;
  onChange: (patch: HotColdConfigPatch) => void;
}

export function HotColdConfig({
  migrationMode,
  hotDays,
  tsColumn,
  rangeStart,
  rangeEndMode,
  rangeEnd,
  minChunkDurationHours,
  maxChunkDurationHours,
  onChange,
}: HotColdConfigProps) {
  const showRange = migrationMode !== "HOT_ONLY";
  const showHot = migrationMode !== "COLD_ONLY";

  return (
    <FieldGroup>
      <Field>
        <FieldLabel>Migration Mode</FieldLabel>
        <OptionSelect
          value={migrationMode}
          onValueChange={(v) => onChange({ migrationMode: v })}
          options={[...MIGRATION_MODE_OPTIONS]}
          placeholder="Select migration mode"
        />
      </Field>
      {showRange && (
        <>
          <Field>
            <FieldLabel>Range start</FieldLabel>
            <Input
              type="datetime-local"
              value={rangeStart}
              onChange={(e) => onChange({ rangeStart: e.target.value })}
            />
          </Field>
          <Field>
            <FieldLabel>End mode</FieldLabel>
            <OptionSelect
              value={rangeEndMode}
              onValueChange={(v) => onChange({ rangeEndMode: v })}
              options={[...RANGE_END_MODE_OPTIONS]}
              placeholder="Select end mode"
            />
          </Field>
          {rangeEndMode === "FIXED" && (
            <Field>
              <FieldLabel>Range end</FieldLabel>
              <Input
                type="datetime-local"
                value={rangeEnd}
                onChange={(e) => onChange({ rangeEnd: e.target.value })}
              />
            </Field>
          )}
        </>
      )}
      {showHot && (
        <Field>
          <FieldLabel>Hot window (days)</FieldLabel>
          <Input type="number" value={hotDays} onChange={(e) => onChange({ hotDays: Number(e.target.value) })} />
        </Field>
      )}
      <Field>
        <FieldLabel>Timestamp column *</FieldLabel>
        <Input required value={tsColumn} onChange={(e) => onChange({ tsColumn: e.target.value })} />
      </Field>
      <Field>
        <FieldLabel>Min chunk duration (hours)</FieldLabel>
        <Input
          type="number"
          value={minChunkDurationHours}
          onChange={(e) => onChange({ minChunkDurationHours: Number(e.target.value) })}
        />
      </Field>
      <Field>
        <FieldLabel>Max chunk duration (hours)</FieldLabel>
        <Input
          type="number"
          value={maxChunkDurationHours}
          onChange={(e) => onChange({ maxChunkDurationHours: Number(e.target.value) })}
        />
      </Field>
    </FieldGroup>
  );
}
