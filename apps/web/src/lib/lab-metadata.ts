import { LAB_SCHEMAS, LAB_SIMULATION_TABLES } from "./simulation-options";

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

/** Schemas always offered in TEST mode (matches JobRunModeGuard). */
export const LAB_SCHEMA_FALLBACKS: readonly string[] = LAB_SCHEMAS;

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
  if (!(LAB_SCHEMA_FALLBACKS as readonly string[]).includes(schema)) return [];
  return LAB_SIMULATION_TABLES.map((t) => ({ ...t }));
}

export function labColumnsForTable(table: string): LabColumnEntry[] {
  return LAB_COLUMNS[table]?.map((c) => ({ ...c })) ?? [];
}

export function isLabSchema(schema: string): boolean {
  return (LAB_SCHEMA_FALLBACKS as readonly string[]).includes(schema);
}
