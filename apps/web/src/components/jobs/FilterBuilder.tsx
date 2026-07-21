"use client";

import { Button } from "@/components/ui/button";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { OptionSelect } from "@/components/ui/option-select";
import { filterOperatorOptions } from "@/lib/filter-operator-labels";

export interface FilterRow {
  column: string;
  operator: string;
  values: string[];
}

const OPERATORS_BY_TYPE: Record<string, string[]> = {
  numeric: ["EQ", "NE", "LT", "LTE", "GT", "GTE", "BETWEEN", "IN", "IS_NULL"],
  text: ["EQ", "NE", "LIKE", "ILIKE", "IN", "IS_NULL"],
  timestamp: ["EQ", "LT", "LTE", "GT", "GTE", "BETWEEN", "IS_NULL"],
  boolean: ["EQ", "IS_NULL"],
};

interface FilterBuilderProps {
  columns: Array<{ name: string; dataType: string }>;
  filters: FilterRow[];
  onChange: (filters: FilterRow[]) => void;
}

export function FilterBuilder({ columns, filters, onChange }: FilterBuilderProps) {
  function addFilter() {
    onChange([...filters, { column: columns[0]?.name || "", operator: "EQ", values: [""] }]);
  }

  function updateFilter(index: number, patch: Partial<FilterRow>) {
    onChange(filters.map((f, i) => (i === index ? { ...f, ...patch } : f)));
  }

  function columnType(col: string) {
    const c = columns.find((x) => x.name === col);
    if (!c) return "text";
    const t = c.dataType.toLowerCase();
    if (t.includes("int") || t.includes("numeric") || t.includes("decimal")) return "numeric";
    if (t.includes("timestamp") || t.includes("date")) return "timestamp";
    if (t.includes("bool")) return "boolean";
    return "text";
  }

  return (
    <FieldGroup>
      <FieldLabel>Filters</FieldLabel>
      {filters.map((f, i) => (
        <div key={i} className="flex flex-wrap items-end gap-2 rounded-md border p-3">
          <Field>
            <FieldLabel>Column</FieldLabel>
            <OptionSelect
              className="w-40"
              value={f.column}
              onValueChange={(v) => updateFilter(i, { column: v, operator: "EQ" })}
              options={columns.map((c) => ({ value: c.name, label: c.name }))}
              placeholder="Column"
            />
          </Field>
          <Field>
            <FieldLabel>Operator</FieldLabel>
            <OptionSelect
              className="w-48"
              value={f.operator}
              onValueChange={(v) => updateFilter(i, { operator: v })}
              options={filterOperatorOptions(OPERATORS_BY_TYPE[columnType(f.column)] || [])}
              placeholder="Operator"
            />
          </Field>
          {f.operator !== "IS_NULL" && (
            <Field>
              <FieldLabel>Value</FieldLabel>
              <Input className="w-48" value={f.values[0] || ""} onChange={(e) => updateFilter(i, { values: [e.target.value] })} />
            </Field>
          )}
          <Button type="button" variant="ghost" onClick={() => onChange(filters.filter((_, j) => j !== i))}>Remove</Button>
        </div>
      ))}
      <Button type="button" variant="outline" onClick={addFilter}>Add Filter</Button>
    </FieldGroup>
  );
}
