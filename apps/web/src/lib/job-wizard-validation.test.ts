import { canOpenJobWizardStep, validateJobWizardStep } from "./job-wizard-validation";

const base = {
  name: "My job",
  runMode: "TEST" as const,
  simulate: false,
  simulationScenario: "COLD_ONLY" as const,
  sourceId: "a",
  destId: "b",
  schema: "test_source",
  table: "orders_cold",
  tsColumn: "created_at",
  migrationMode: "COLD_ONLY",
  rangeEndMode: "NOW",
  rangeEnd: "",
  rangeStart: "2024-01-01T00:00",
  filters: [] as { column: string; operator: string; values: string[] }[],
  columns: [{ name: "created_at", dataType: "timestamp with time zone" }],
};

describe("validateJobWizardStep", () => {
  it("requires name and connections on step 1", () => {
    expect(validateJobWizardStep("1", { ...base, name: "" }).ok).toBe(false);
    expect(validateJobWizardStep("1", { ...base, sourceId: "" }).ok).toBe(false);
  });

  it("allows same source and destination connection", () => {
    expect(validateJobWizardStep("1", { ...base, sourceId: "a", destId: "a" }).ok).toBe(true);
  });

  it("requires schema and table on step 2 even when simulating", () => {
    expect(validateJobWizardStep("2", { ...base, simulate: true, schema: "", table: "" }).ok).toBe(false);
    expect(validateJobWizardStep("2", { ...base, simulate: true }).ok).toBe(true);
  });

  it("rejects public schema for TEST run mode", () => {
    expect(validateJobWizardStep("2", { ...base, schema: "public", table: "connections" }).ok).toBe(false);
    expect(validateJobWizardStep("2", { ...base, schema: "test_destination", table: "job_abc" }).ok).toBe(false);
  });

  it("blocks step 3 when step 1 invalid", () => {
    const r = canOpenJobWizardStep("3", { ...base, name: "" });
    expect(r.ok).toBe(false);
  });

  it("validates filter rows on step 3", () => {
    const cols = [
      { name: "id", dataType: "bigint" },
      { name: "status", dataType: "varchar" },
    ];
    expect(
      validateJobWizardStep("3", {
        ...base,
        columns: cols,
        filters: [{ column: "id", operator: "LIKE", values: ["1"] }],
      }).ok
    ).toBe(false);
    expect(
      validateJobWizardStep("3", {
        ...base,
        columns: cols,
        filters: [{ column: "id", operator: "BETWEEN", values: ["1", "10"] }],
      }).ok
    ).toBe(true);
  });
});
