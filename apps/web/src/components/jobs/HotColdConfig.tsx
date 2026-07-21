"use client";

import { Field, FieldGroup, FieldLabel, FieldLegend, FieldSet } from "@/components/ui/field";
import { DateTimeTzField } from "@/components/ui/datetime-tz-field";
import { DurationInput } from "@/components/ui/duration-input";
import { Input } from "@/components/ui/input";
import { NumberInput } from "@/components/ui/number-input";
import { OptionSelect } from "@/components/ui/option-select";
import { Separator } from "@/components/ui/separator";
import type { DateTimeTz } from "@/lib/datetime-tz";
import { MIGRATION_MODE_OPTIONS, RANGE_END_MODE_OPTIONS } from "@/lib/migration-mode-options";

export interface HotColdConfigPatch {
  migrationMode?: string;
  hotDays?: number;
  tsColumn?: string;
  rangeStart?: string;
  rangeStartTz?: DateTimeTz;
  rangeEndMode?: string;
  rangeEnd?: string;
  rangeEndTz?: DateTimeTz;
  minChunkDurationHours?: number;
  maxChunkDurationHours?: number;
}

interface HotColdConfigProps {
  migrationMode: string;
  hotDays: number;
  tsColumn: string;
  timestampColumns?: string[];
  rangeStart: string;
  rangeStartTz: DateTimeTz;
  rangeEndMode: string;
  rangeEnd: string;
  rangeEndTz: DateTimeTz;
  minChunkDurationHours: number;
  maxChunkDurationHours: number;
  onChange: (patch: HotColdConfigPatch) => void;
}

export function HotColdConfig({
  migrationMode,
  hotDays,
  tsColumn,
  timestampColumns = [],
  rangeStart,
  rangeStartTz,
  rangeEndMode,
  rangeEnd,
  rangeEndTz,
  minChunkDurationHours,
  maxChunkDurationHours,
  onChange,
}: HotColdConfigProps) {
  const showRange = migrationMode !== "HOT_ONLY";
  const showHot = migrationMode !== "COLD_ONLY";

  return (
    <div className="flex flex-col gap-6">
      <FieldSet>
        <FieldLegend>Migration mode</FieldLegend>
        <FieldGroup>
          <Field>
            <FieldLabel>Mode</FieldLabel>
            <OptionSelect
              value={migrationMode}
              onValueChange={(v) => onChange({ migrationMode: v })}
              options={[...MIGRATION_MODE_OPTIONS]}
              placeholder="Select migration mode"
            />
          </Field>
          {showHot && (
            <Field>
              <FieldLabel>Hot window (days)</FieldLabel>
              <NumberInput min={0} value={hotDays} onValueChange={(v) => onChange({ hotDays: v })} />
            </Field>
          )}
        </FieldGroup>
      </FieldSet>

      {showRange && (
        <>
          <Separator />
          <FieldSet>
            <FieldLegend>Range</FieldLegend>
            <FieldGroup>
              <DateTimeTzField
                label="Range start"
                isoValue={rangeStart}
                tz={rangeStartTz}
                onChange={(iso, tz) => onChange({ rangeStart: iso, rangeStartTz: tz })}
              />
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
                <DateTimeTzField
                  label="Range end"
                  isoValue={rangeEnd}
                  tz={rangeEndTz}
                  onChange={(iso, tz) => onChange({ rangeEnd: iso, rangeEndTz: tz })}
                />
              )}
            </FieldGroup>
          </FieldSet>
        </>
      )}

      <Separator />
      <FieldSet>
        <FieldLegend>Timestamp</FieldLegend>
        <FieldGroup>
          <Field>
            <FieldLabel>Timestamp column *</FieldLabel>
            {timestampColumns.length > 0 ? (
              <OptionSelect
                value={tsColumn}
                onValueChange={(v) => onChange({ tsColumn: v })}
                options={timestampColumns.map((c) => ({ value: c, label: c }))}
                placeholder="Select timestamp column"
              />
            ) : (
              <Input required value={tsColumn} onChange={(e) => onChange({ tsColumn: e.target.value })} />
            )}
          </Field>
        </FieldGroup>
      </FieldSet>

      <Separator />
      <FieldSet>
        <FieldLegend>Chunk config</FieldLegend>
        <FieldGroup>
          <DurationInput
            label="Min chunk duration"
            hours={minChunkDurationHours}
            onHoursChange={(h) => onChange({ minChunkDurationHours: h })}
          />
          <DurationInput
            label="Max chunk duration"
            hours={maxChunkDurationHours}
            onHoursChange={(h) => onChange({ maxChunkDurationHours: h })}
          />
        </FieldGroup>
      </FieldSet>
    </div>
  );
}
