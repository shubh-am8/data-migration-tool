import { LAB_SCHEMAS, LAB_SIMULATION_TABLES, LAB_SOURCE_SCHEMA } from "./simulation-options";

export type LabTableEntry = {
  name: string;
  kind: string;
  partitioned: boolean;
  partitions: string[];
};

export type LabColumnEntry = {
  name: string;
  dataType: string;
};

/** Schemas offered in TEST wizard (source only). */
export const LAB_SCHEMA_FALLBACKS: readonly string[] = LAB_SCHEMAS;

export { LAB_SOURCE_SCHEMA, LAB_DEST_SCHEMA } from "./simulation-options";

const ORDERS_COLD_COLUMNS: LabColumnEntry[] = [
  { name: "id", dataType: "bigint" },
  { name: "order_code", dataType: "text" },
  { name: "amount_cents", dataType: "integer" },
  { name: "created_at", dataType: "timestamp with time zone" },
];

const ORDERS_HOT_COLD_COLUMNS: LabColumnEntry[] = [
  { name: "id", dataType: "bigint" },
  { name: "order_code", dataType: "text" },
  { name: "amount_cents", dataType: "integer" },
  { name: "updated_at", dataType: "timestamp with time zone" },
];

const LAB_COLUMNS: Record<string, LabColumnEntry[]> = {
  orders_cold: ORDERS_COLD_COLUMNS,
  orders_hot_cold: ORDERS_HOT_COLD_COLUMNS,
};

export function labTablesForSchema(schema: string): LabTableEntry[] {
  if (schema !== LAB_SOURCE_SCHEMA) return [];
  return LAB_SIMULATION_TABLES.map((t) => ({ ...t }));
}

export function labColumnsForTable(table: string): LabColumnEntry[] {
  return LAB_COLUMNS[table]?.map((c) => ({ ...c })) ?? [];
}

export function isLabSchema(schema: string): boolean {
  return schema === LAB_SOURCE_SCHEMA;
}
