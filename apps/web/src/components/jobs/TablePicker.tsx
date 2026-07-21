"use client";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { cn } from "@/lib/utils";

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
  loading?: boolean;
  error?: string | null;
  schemaSelected?: boolean;
}

export function TablePicker({
  tables,
  selected,
  usePartition,
  partitionName,
  onSelectTable,
  onTogglePartition,
  onSelectPartition,
  loading,
  error,
  schemaSelected = true,
}: TablePickerProps) {
  const table = tables.find((t) => t.name === selected);

  return (
    <FieldGroup>
      <Field>
        <FieldLabel>Table / View / Materialized View</FieldLabel>
        {!schemaSelected ? (
          <p className="text-sm text-muted-foreground">Select a schema first.</p>
        ) : loading ? (
          <p className="text-sm text-muted-foreground">Loading tables…</p>
        ) : tables.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            No tables found. Install Lab Dev Tools from the marketplace, then restart the API.
          </p>
        ) : (
          <div className="flex flex-col gap-2">
            {tables.map((t) => {
              const isSelected = selected === t.name;
              return (
                <Button
                  key={t.name}
                  type="button"
                  variant="outline"
                  className={cn(
                    "justify-start",
                    isSelected &&
                      "border-sky-600 bg-sky-600 text-white shadow-sm hover:bg-sky-600/90 hover:text-white"
                  )}
                  onClick={() => onSelectTable(t.name)}
                >
                  {t.name}
                  <Badge
                    variant="secondary"
                    className={cn("ml-2", isSelected && "bg-sky-500/30 text-white")}
                  >
                    {t.kind}
                  </Badge>
                  {t.partitioned && (
                    <Badge variant="outline" className={cn("ml-2", isSelected && "border-white/40 text-white")}>
                      partitioned
                    </Badge>
                  )}
                </Button>
              );
            })}
          </div>
        )}
        {error ? <p className="mt-2 text-sm text-amber-600 dark:text-amber-400">{error}</p> : null}
      </Field>
      {table?.partitioned && (
        <Field>
          <FieldLabel>Partition</FieldLabel>
          <div className="flex flex-col gap-2">
            <Button
              type="button"
              variant="outline"
              className={cn(
                !usePartition &&
                  "border-violet-600 bg-violet-600 text-white hover:bg-violet-600/90 hover:text-white"
              )}
              onClick={() => onTogglePartition(false)}
            >
              Parent table: {table.name}
            </Button>
            {table.partitions.map((p) => {
              const partSelected = usePartition && partitionName === p;
              return (
                <Button
                  key={p}
                  type="button"
                  variant="outline"
                  className={cn(
                    partSelected &&
                      "border-violet-600 bg-violet-600 text-white hover:bg-violet-600/90 hover:text-white"
                  )}
                  onClick={() => {
                    onTogglePartition(true);
                    onSelectPartition(p);
                  }}
                >
                  {p}
                </Button>
              );
            })}
          </div>
        </Field>
      )}
    </FieldGroup>
  );
}
