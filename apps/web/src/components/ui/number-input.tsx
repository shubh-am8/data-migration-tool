"use client";

import * as React from "react";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

export interface NumberInputProps extends Omit<React.ComponentProps<typeof Input>, "type" | "value" | "onChange"> {
  value: number;
  onValueChange: (value: number) => void;
  min?: number;
  max?: number;
}

/** Controlled numeric input — avoids leading-zero quirks from native number fields. */
export function NumberInput({
  value,
  onValueChange,
  min,
  max,
  className,
  onBlur,
  ...props
}: NumberInputProps) {
  const [text, setText] = React.useState(String(value));

  React.useEffect(() => {
    setText(String(value));
  }, [value]);

  function commit(raw: string) {
    const trimmed = raw.trim();
    if (trimmed === "" || trimmed === "-") {
      setText(String(value));
      return;
    }
    let n = Number(trimmed);
    if (!Number.isFinite(n)) {
      setText(String(value));
      return;
    }
    if (min !== undefined) n = Math.max(min, n);
    if (max !== undefined) n = Math.min(max, n);
    onValueChange(n);
    setText(String(n));
  }

  return (
    <Input
      {...props}
      type="text"
      inputMode="numeric"
      className={cn(className)}
      value={text}
      onChange={(e) => {
        const next = e.target.value;
        if (/^-?\d*$/.test(next)) setText(next);
      }}
      onBlur={(e) => {
        commit(e.target.value);
        onBlur?.(e);
      }}
    />
  );
}
