"use client";

import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { OptionSelect } from "@/components/ui/option-select";

interface SchemaPickerProps {
  schemas: string[];
  value: string;
  onChange: (schema: string) => void;
}

export function SchemaPicker({ schemas, value, onChange }: SchemaPickerProps) {
  const options = schemas.map((s) => ({ value: s, label: s }));

  return (
    <FieldGroup>
      <Field>
        <FieldLabel>Schema</FieldLabel>
        <OptionSelect
          value={value}
          onValueChange={onChange}
          options={options}
          placeholder="Select schema"
        />
      </Field>
    </FieldGroup>
  );
}
