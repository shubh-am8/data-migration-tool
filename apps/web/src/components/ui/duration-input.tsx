"use client";

import { Field, FieldLabel } from "@/components/ui/field";
import { NumberInput } from "@/components/ui/number-input";
import { OptionSelect } from "@/components/ui/option-select";
import {
  DURATION_UNIT_OPTIONS,
  durationToHours,
  hoursToDisplay,
  type DurationUnit,
} from "@/lib/duration-units";

interface DurationInputProps {
  label: string;
  hours: number;
  onHoursChange: (hours: number) => void;
  minHours?: number;
}

export function DurationInput({ label, hours, onHoursChange, minHours = 1 }: DurationInputProps) {
  const display = hoursToDisplay(hours);

  return (
    <Field>
      <FieldLabel>{label}</FieldLabel>
      <div className="flex flex-col gap-2 sm:flex-row">
        <NumberInput
          className="min-w-0 flex-1"
          min={1}
          value={display.value}
          onValueChange={(v) => onHoursChange(Math.max(minHours, durationToHours(v, display.unit)))}
        />
        <OptionSelect
          className="sm:w-36"
          value={display.unit}
          onValueChange={(unit) => {
            const u = unit as DurationUnit;
            onHoursChange(Math.max(minHours, durationToHours(display.value, u)));
          }}
          options={[...DURATION_UNIT_OPTIONS]}
          placeholder="Unit"
        />
      </div>
    </Field>
  );
}
