"use client";

import { Field, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { OptionSelect } from "@/components/ui/option-select";
import {
  DATETIME_TZ_OPTIONS,
  type DateTimeTz,
  isoToLocalDateTime,
  localDateTimeToIso,
} from "@/lib/datetime-tz";

interface DateTimeTzFieldProps {
  label: string;
  isoValue: string;
  tz: DateTimeTz;
  onChange: (iso: string, tz: DateTimeTz) => void;
}

export function DateTimeTzField({ label, isoValue, tz, onChange }: DateTimeTzFieldProps) {
  const local = isoToLocalDateTime(isoValue, tz);

  return (
    <Field>
      <FieldLabel>{label}</FieldLabel>
      <div className="flex flex-col gap-2 sm:flex-row">
        <Input
          type="datetime-local"
          className="min-w-0 flex-1"
          value={local}
          onChange={(e) => onChange(localDateTimeToIso(e.target.value, tz), tz)}
        />
        <OptionSelect
          className="sm:w-44"
          value={tz}
          onValueChange={(next) => {
            const nextTz = next as DateTimeTz;
            if (!local) {
              onChange(isoValue, nextTz);
              return;
            }
            onChange(localDateTimeToIso(local, nextTz), nextTz);
          }}
          options={[...DATETIME_TZ_OPTIONS]}
          placeholder="Timezone"
        />
      </div>
    </Field>
  );
}
