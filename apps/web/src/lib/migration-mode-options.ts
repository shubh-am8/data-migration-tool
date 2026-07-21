export const MIGRATION_MODE_OPTIONS = [
  { value: "HOT_ONLY", label: "Hot only" },
  { value: "COLD_ONLY", label: "Cold only" },
  { value: "HOT_THEN_COLD", label: "Hot then cold" },
  { value: "COLD_THEN_HOT", label: "Cold then hot" },
] as const;

export const RANGE_END_MODE_OPTIONS = [
  { value: "NOW", label: "Always now" },
  { value: "FIXED", label: "Fixed end date" },
] as const;
