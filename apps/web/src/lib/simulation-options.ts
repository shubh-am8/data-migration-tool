export type SimulationScenario = "COLD_ONLY" | "HOT_ONLY" | "HOT_THEN_COLD" | "COLD_THEN_HOT";

export const SIMULATION_SCENARIO_OPTIONS: { value: SimulationScenario; label: string }[] = [
  { value: "COLD_ONLY", label: "Cold data only" },
  { value: "HOT_ONLY", label: "Hot data only" },
  { value: "HOT_THEN_COLD", label: "Hot then cold" },
  { value: "COLD_THEN_HOT", label: "Cold then hot" },
];

export const LAB_SOURCE_SCHEMA = "test_source" as const;
export const LAB_DEST_SCHEMA = "test_destination" as const;

/** Schemas offered in TEST job wizard (source only). */
export const LAB_SCHEMAS = [LAB_SOURCE_SCHEMA] as const;

export const LAB_SIMULATION_TABLES = [
  { name: "orders_cold", kind: "table", partitioned: false, partitions: [] as string[] },
  { name: "orders_hot_cold", kind: "table", partitioned: false, partitions: [] as string[] },
];

export function simulationPreset(scenario: SimulationScenario) {
  const usesHotColdTable = scenario === "HOT_THEN_COLD" || scenario === "COLD_THEN_HOT" || scenario === "HOT_ONLY";
  return {
    schema: LAB_SOURCE_SCHEMA,
    table: usesHotColdTable ? "orders_hot_cold" : "orders_cold",
    configJson: {
      kind: "SIMULATE" as const,
      scenario,
      schema: LAB_SOURCE_SCHEMA,
      table: usesHotColdTable ? "orders_hot_cold" : "orders_cold",
      rows: 100,
      updateRatio: scenario === "HOT_THEN_COLD" || scenario === "COLD_THEN_HOT" ? 0.2 : 0,
    },
  };
}

export function buildSimulationConfigJson(
  scenario: SimulationScenario,
  schema: string,
  table: string
) {
  const preset = simulationPreset(scenario);
  return { ...preset.configJson, schema, table };
}
