"use client";

import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";

export type SelectOption<T extends string = string> = { value: T; label: string };

/**
 * Select that shows the chosen option's label in the trigger (base-ui SelectValue
 * otherwise renders the raw value, e.g. UUIDs).
 */
export function OptionSelect<T extends string>({
  value,
  onValueChange,
  options,
  placeholder = "Select…",
  className,
  disabled,
}: {
  value: T | "";
  onValueChange: (value: T) => void;
  options: SelectOption<T>[];
  placeholder?: string;
  className?: string;
  disabled?: boolean;
}) {
  const selected = options.find((o) => o.value === value);
  // ponytail: null = empty but still controlled; undefined would flip to uncontrolled (base-ui warning)
  const selectValue = value === "" ? null : value;

  return (
    <Select
      value={selectValue}
      onValueChange={(v) => v && onValueChange(v as T)}
      disabled={disabled}
    >
      <SelectTrigger className={cn("w-full", className)}>
        <span className={cn("truncate", !selected && "text-muted-foreground")}>
          {selected?.label ?? placeholder}
        </span>
      </SelectTrigger>
      <SelectContent>
        <SelectGroup>
          {options.map((o) => (
            <SelectItem key={o.value} value={o.value}>
              {o.label}
            </SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  );
}
