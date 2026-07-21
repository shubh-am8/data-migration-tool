export type SimulationScenario = "COLD_ONLY" | "HOT_ONLY" | "HOT_THEN_COLD" | "COLD_THEN_HOT";

export const SIMULATION_SCENARIO_OPTIONS: { value: SimulationScenario; label: string }[] = [
  { value: "COLD_ONLY", label: "Cold data only" },
  { value: "HOT_ONLY", label: "Hot data only" },
  { value: "HOT_THEN_COLD", label: "Hot then cold" },
  { value: "COLD_THEN_HOT", label: "Cold then hot" },
];

export const LAB_SCHEMAS = ["app", "test"] as const;

export const LAB_SIMULATION_TABLES = [
  { name: "orders_cold", kind: "table", partitioned: false, partitions: [] as string[] },
  { name: "orders_hot_cold", kind: "table", partitioned: false, partitions: [] as string[] },
];

export function simulationPreset(scenario: SimulationScenario) {
  const usesHotColdTable = scenario === "HOT_THEN_COLD" || scenario === "COLD_THEN_HOT" || scenario === "HOT_ONLY";
  return {
    schema: "app" as const,
    table: usesHotColdTable ? "orders_hot_cold" : "orders_cold",
    configJson: {
      kind: "SIMULATE" as const,
      scenario,
      schema: "app",
      table: usesHotColdTable ? "orders_hot_cold" : "orders_cold",
      rows: 100,
      updateRatio: scenario === "HOT_THEN_COLD" || scenario === "COLD_THEN_HOT" ? 0.2 : 0,
    },
  };
}

export function simulationScenarioLabel(scenario: string): string {
  return SIMULATION_SCENARIO_OPTIONS.find((o) => o.value === scenario)?.label ?? scenario;
}
