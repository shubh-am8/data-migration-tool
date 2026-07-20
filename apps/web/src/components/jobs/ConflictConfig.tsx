"use client";

import { Badge } from "@/components/ui/badge";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";

interface ConflictConfigProps {
  columns: string[];
  selected: string[];
  onChange: (cols: string[]) => void;
}

export function ConflictConfig({ columns, selected, onChange }: ConflictConfigProps) {
  function toggle(col: string) {
    onChange(selected.includes(col) ? selected.filter((c) => c !== col) : [...selected, col]);
  }

  return (
    <FieldGroup>
      <Field>
        <FieldLabel>ON CONFLICT columns</FieldLabel>
        <p className="mb-2 text-xs text-muted-foreground">Hot phase: DO UPDATE. Cold phase: DO NOTHING.</p>
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
    </FieldGroup>
  );
}
