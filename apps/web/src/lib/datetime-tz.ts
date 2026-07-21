export type DateTimeTz = "UTC" | "IST";

export const DATETIME_TZ_OPTIONS: { value: DateTimeTz; label: string }[] = [
  { value: "UTC", label: "UTC" },
  { value: "IST", label: "IST (Asia/Kolkata)" },
];

const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;

/** `datetime-local` value + tz → ISO UTC string for API. */
export function localDateTimeToIso(localValue: string, tz: DateTimeTz): string {
  if (!localValue) return "";
  const asUtc = new Date(`${localValue}:00Z`);
  if (Number.isNaN(asUtc.getTime())) return "";
  if (tz === "IST") {
    return new Date(asUtc.getTime() - IST_OFFSET_MS).toISOString();
  }
  return asUtc.toISOString();
}

/** ISO UTC string → `datetime-local` + tz for display. */
export function isoToLocalDateTime(iso: string, tz: DateTimeTz): string {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  const shifted = tz === "IST" ? new Date(d.getTime() + IST_OFFSET_MS) : d;
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${shifted.getUTCFullYear()}-${pad(shifted.getUTCMonth() + 1)}-${pad(shifted.getUTCDate())}T${pad(shifted.getUTCHours())}:${pad(shifted.getUTCMinutes())}`;
}
