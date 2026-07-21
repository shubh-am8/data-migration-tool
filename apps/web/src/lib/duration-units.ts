export type DurationUnit = "minutes" | "hours" | "days";

export const DURATION_UNIT_OPTIONS: { value: DurationUnit; label: string }[] = [
  { value: "minutes", label: "Minutes" },
  { value: "hours", label: "Hours" },
  { value: "days", label: "Days" },
];

/** Converts UI duration to hours (API storage unit). */
export function durationToHours(value: number, unit: DurationUnit): number {
  if (!Number.isFinite(value) || value < 0) return 0;
  return switchUnit(value, unit, "hours");
}

/** Picks a sensible display unit for a stored hour value. */
export function hoursToDisplay(hours: number): { value: number; unit: DurationUnit } {
  if (hours <= 0) return { value: 0, unit: "hours" };
  if (hours % 24 === 0 && hours >= 24) return { value: hours / 24, unit: "days" };
  if (hours < 1) return { value: Math.round(hours * 60), unit: "minutes" };
  return { value: hours, unit: "hours" };
}

function switchUnit(value: number, from: DurationUnit, to: DurationUnit): number {
  const minutes =
    from === "minutes" ? value : from === "hours" ? value * 60 : value * 24 * 60;
  if (to === "minutes") return minutes;
  if (to === "hours") return minutes / 60;
  return minutes / (24 * 60);
}
