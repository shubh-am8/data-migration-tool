"use client";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";

interface TableEntry {
  name: string;
  kind: string;
  partitioned: boolean;
  partitions: string[];
}

interface TablePickerProps {
  tables: TableEntry[];
  selected: string;
  usePartition: boolean;
  partitionName: string;
  onSelectTable: (table: string) => void;
  onTogglePartition: (use: boolean) => void;
  onSelectPartition: (partition: string) => void;
}

export function TablePicker({
  tables, selected, usePartition, partitionName,
  onSelectTable, onTogglePartition, onSelectPartition,
}: TablePickerProps) {
  const table = tables.find((t) => t.name === selected);

  return (
    <FieldGroup>
      <Field>
        <FieldLabel>Table / View / Materialized View</FieldLabel>
        <div className="flex flex-col gap-2">
          {tables.map((t) => (
            <Button
              key={t.name}
              type="button"
              variant={selected === t.name ? "default" : "outline"}
              className="justify-start"
              onClick={() => onSelectTable(t.name)}
            >
              {t.name}
              <Badge variant="secondary" className="ml-2">{t.kind}</Badge>
              {t.partitioned && <Badge variant="outline" className="ml-2">partitioned</Badge>}
            </Button>
          ))}
        </div>
      </Field>
      {table?.partitioned && (
        <Field>
          <FieldLabel>Partition</FieldLabel>
          <div className="flex flex-col gap-2">
            <Button type="button" variant={!usePartition ? "default" : "outline"} onClick={() => onTogglePartition(false)}>
              Parent table: {table.name}
            </Button>
            {table.partitions.map((p) => (
              <Button key={p} type="button" variant={usePartition && partitionName === p ? "default" : "outline"}
                onClick={() => { onTogglePartition(true); onSelectPartition(p); }}>
                {p}
              </Button>
            ))}
          </div>
        </Field>
      )}
    </FieldGroup>
  );
}
