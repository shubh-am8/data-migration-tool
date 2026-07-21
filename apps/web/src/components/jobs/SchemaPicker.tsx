"use client";

import { Button } from "@/components/ui/button";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { cn } from "@/lib/utils";

interface SchemaPickerProps {
  schemas: string[];
  value: string;
  onChange: (schema: string) => void;
  loading?: boolean;
  error?: string | null;
}

export function SchemaPicker({ schemas, value, onChange, loading, error }: SchemaPickerProps) {
  return (
    <FieldGroup>
      <Field>
        <FieldLabel>Schema</FieldLabel>
        {loading ? (
          <p className="text-sm text-muted-foreground">Loading schemas…</p>
        ) : schemas.length === 0 ? (
          <p className="text-sm text-muted-foreground">No schemas available.</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {schemas.map((s) => {
              const selected = value === s;
              return (
                <Button
                  key={s}
                  type="button"
                  variant="outline"
                  className={cn(
                    "min-w-20 font-medium",
                    selected &&
                      "border-emerald-600 bg-emerald-600 text-white shadow-sm hover:bg-emerald-600/90 hover:text-white"
                  )}
                  onClick={() => onChange(s)}
                >
                  {s}
                </Button>
              );
            })}
          </div>
        )}
        {error ? <p className="mt-2 text-sm text-amber-600 dark:text-amber-400">{error}</p> : null}
      </Field>
    </FieldGroup>
  );
}
