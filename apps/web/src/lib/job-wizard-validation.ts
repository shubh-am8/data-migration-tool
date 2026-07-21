import type { SimulationScenario } from "./simulation-options";

export type JobWizardState = {
  name: string;
  runMode: "TEST" | "PRODUCTION";
  simulate: boolean;
  simulationScenario: SimulationScenario;
  sourceId: string;
  destId: string;
  schema: string;
  table: string;
  tsColumn: string;
  migrationMode: string;
  rangeEndMode: string;
  rangeEnd: string;
  rangeStart: string;
};

export type StepValidation = { ok: true } | { ok: false; message: string };

export function validateJobWizardStep(step: string, s: JobWizardState): StepValidation {
  switch (step) {
    case "1": {
      if (!s.name.trim()) return { ok: false, message: "Job name is required" };
      if (!s.sourceId) return { ok: false, message: "Select a source connection" };
      if (!s.destId) return { ok: false, message: "Select a destination connection" };
      if (s.sourceId === s.destId) return { ok: false, message: "Source and destination must differ" };
      return { ok: true };
    }
    case "2": {
      if (s.simulate && s.runMode === "TEST") return { ok: true };
      if (!s.schema) return { ok: false, message: "Select a schema" };
      if (!s.table) return { ok: false, message: "Select a table" };
      return { ok: true };
    }
    case "3":
      return { ok: true };
    case "4": {
      if (!s.tsColumn.trim()) return { ok: false, message: "Timestamp column is required" };
      if (s.migrationMode !== "HOT_ONLY" && !s.rangeStart) {
        return { ok: false, message: "Range start is required for this migration mode" };
      }
      if (s.rangeEndMode === "FIXED" && !s.rangeEnd) {
        return { ok: false, message: "Range end is required when end mode is fixed" };
      }
      return { ok: true };
    }
    case "5":
      return { ok: true };
    default:
      return { ok: true };
  }
}

/** Block jumping ahead: every prior step must be valid. */
export function canOpenJobWizardStep(targetStep: string, s: JobWizardState): StepValidation {
  const order = ["1", "2", "3", "4", "5"];
  const idx = order.indexOf(targetStep);
  if (idx < 0) return { ok: true };
  for (let i = 0; i < idx; i++) {
    const result = validateJobWizardStep(order[i]!, s);
    if (!result.ok) return result;
  }
  return { ok: true };
}
