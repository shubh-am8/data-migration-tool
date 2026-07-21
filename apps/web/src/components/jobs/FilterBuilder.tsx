"use client";

import { Button } from "@/components/ui/button";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { OptionSelect } from "@/components/ui/option-select";
import {
  columnDataTypeFromSql,
  defaultOperatorForColumnType,
  defaultValuesForOperator,
  isOperatorValidForType,
  operatorsForColumnType,
  type FilterOperator,
} from "@/lib/column-data-type";
import { filterOperatorOptions } from "@/lib/filter-operator-labels";
import { columnTypeForName, type FilterRow } from "@/lib/filter-validation";

export type { FilterRow };

interface FilterBuilderProps {
  columns: Array<{ name: string; dataType: string }>;
  filters: FilterRow[];
  onChange: (filters: FilterRow[]) => void;
  loading?: boolean;
}

function columnLabel(c: { name: string; dataType: string }) {
  return `${c.name} (${c.dataType})`;
}

export function FilterBuilder({ columns, filters, onChange, loading }: FilterBuilderProps) {
  function addFilter() {
    const first = columns[0];
    if (!first) return;
    const type = columnDataTypeFromSql(first.dataType);
    const operator = defaultOperatorForColumnType(type);
    onChange([...filters, { column: first.name, operator, values: defaultValuesForOperator(operator) }]);
  }

  function updateFilter(index: number, patch: Partial<FilterRow>) {
    onChange(filters.map((f, i) => (i === index ? { ...f, ...patch } : f)));
  }

  function onColumnChange(index: number, column: string) {
    const type = columnTypeForName(columns, column);
    const operator = defaultOperatorForColumnType(type);
    updateFilter(index, { column, operator, values: defaultValuesForOperator(operator) });
  }

  function onOperatorChange(index: number, operator: FilterOperator) {
    const row = filters[index];
    if (!row) return;
    const values =
      operator === row.operator
        ? row.values
        : defaultValuesForOperator(operator);
    updateFilter(index, { operator, values });
  }

  function setValue(index: number, valueIndex: number, value: string) {
    const row = filters[index];
    if (!row) return;
    const values = [...row.values];
    values[valueIndex] = value;
    updateFilter(index, { values });
  }

  if (loading) {
    return (
      <FieldGroup>
        <FieldLabel>Filters</FieldLabel>
        <p className="text-sm text-muted-foreground">Loading columns for the selected table…</p>
      </FieldGroup>
    );
  }

  if (columns.length === 0) {
    return (
      <FieldGroup>
        <FieldLabel>Filters</FieldLabel>
        <p className="text-sm text-muted-foreground">
          No columns loaded. Go back to Schema &amp; Table, select a source connection, schema, and table, then return
          here.
        </p>
      </FieldGroup>
    );
  }

  return (
    <FieldGroup>
      <FieldLabel>Filters</FieldLabel>
      <p className="text-sm text-muted-foreground">
        Optional row filters for <strong>{columns.length}</strong> column{columns.length === 1 ? "" : "s"} from the
        selected table. Operators depend on each column&apos;s data type.
      </p>
      {filters.map((f, i) => {
        const type = columnTypeForName(columns, f.column);
        const ops = operatorsForColumnType(type);
        const operator = isOperatorValidForType(f.operator as FilterOperator, type)
          ? (f.operator as FilterOperator)
          : defaultOperatorForColumnType(type);

        return (
          <div key={i} className="flex flex-wrap items-end gap-2 rounded-md border p-3">
            <Field>
              <FieldLabel>Column</FieldLabel>
              <OptionSelect
                className="min-w-48"
                value={f.column}
                onValueChange={(v) => onColumnChange(i, v)}
                options={columns.map((c) => ({ value: c.name, label: columnLabel(c) }))}
                placeholder="Select column"
              />
            </Field>
            <Field>
              <FieldLabel>Operator</FieldLabel>
              <OptionSelect
                className="min-w-52"
                value={operator}
                onValueChange={(v) => onOperatorChange(i, v as FilterOperator)}
                options={filterOperatorOptions(ops)}
                placeholder="Select operator"
              />
            </Field>
            {operator === "IS_NULL" ? null : operator === "BETWEEN" ? (
              <>
                <Field>
                  <FieldLabel>From</FieldLabel>
                  <Input
                    className="w-40"
                    value={f.values[0] || ""}
                    onChange={(e) => setValue(i, 0, e.target.value)}
                  />
                </Field>
                <Field>
                  <FieldLabel>To</FieldLabel>
                  <Input
                    className="w-40"
                    value={f.values[1] || ""}
                    onChange={(e) => setValue(i, 1, e.target.value)}
                  />
                </Field>
              </>
            ) : operator === "IN" ? (
              <Field>
                <FieldLabel>Values (comma-separated)</FieldLabel>
                <Input
                  className="min-w-48"
                  value={f.values.join(", ")}
                  onChange={(e) => updateFilter(i, { values: [e.target.value] })}
                  placeholder="value1, value2"
                />
              </Field>
            ) : (
              <Field>
                <FieldLabel>Value</FieldLabel>
                <Input
                  className="w-48"
                  value={f.values[0] || ""}
                  onChange={(e) => setValue(i, 0, e.target.value)}
                />
              </Field>
            )}
            <Button type="button" variant="ghost" onClick={() => onChange(filters.filter((_, j) => j !== i))}>
              Remove
            </Button>
          </div>
        );
      })}
      <Button type="button" variant="outline" onClick={addFilter}>
        Add Filter
      </Button>
    </FieldGroup>
  );
}
