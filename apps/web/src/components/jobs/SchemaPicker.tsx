"use client";

import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface SchemaPickerProps {
  schemas: string[];
  value: string;
  onChange: (schema: string) => void;
}

export function SchemaPicker({ schemas, value, onChange }: SchemaPickerProps) {
  return (
    <FieldGroup>
      <Field>
        <FieldLabel>Schema</FieldLabel>
        <Select value={value} onValueChange={(v) => v && onChange(v)}>
          <SelectTrigger><SelectValue placeholder="Select schema" /></SelectTrigger>
          <SelectContent>
            {schemas.map((s) => (
              <SelectItem key={s} value={s}>{s}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </Field>
    </FieldGroup>
  );
}
