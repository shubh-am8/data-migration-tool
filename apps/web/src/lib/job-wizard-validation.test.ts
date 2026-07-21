import { canOpenJobWizardStep, validateJobWizardStep } from "./job-wizard-validation";

const base = {
  name: "My job",
  runMode: "TEST" as const,
  simulate: false,
  simulationScenario: "COLD_ONLY" as const,
  sourceId: "a",
  destId: "b",
  schema: "app",
  table: "orders_cold",
  tsColumn: "created_at",
  migrationMode: "COLD_ONLY",
  rangeEndMode: "NOW",
  rangeEnd: "",
  rangeStart: "2024-01-01T00:00",
};

describe("validateJobWizardStep", () => {
  it("requires name and connections on step 1", () => {
    expect(validateJobWizardStep("1", { ...base, name: "" }).ok).toBe(false);
    expect(validateJobWizardStep("1", { ...base, sourceId: "" }).ok).toBe(false);
  });

  it("skips schema when simulating", () => {
    expect(validateJobWizardStep("2", { ...base, simulate: true, schema: "", table: "" }).ok).toBe(true);
  });

  it("blocks step 3 when step 1 invalid", () => {
    const r = canOpenJobWizardStep("3", { ...base, name: "" });
    expect(r.ok).toBe(false);
  });
});
