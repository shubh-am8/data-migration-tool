export type StatTone = "default" | "success" | "warning" | "danger" | "info";

export function statToneClasses(tone: StatTone): string {
  switch (tone) {
    case "success":
      return "border-l-4 border-l-emerald-600";
    case "warning":
      return "border-l-4 border-l-amber-500";
    case "danger":
      return "border-l-4 border-l-red-600";
    case "info":
      return "border-l-4 border-l-sky-600";
    default:
      return "";
  }
}
