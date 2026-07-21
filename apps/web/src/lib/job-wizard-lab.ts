import { apiFetch } from "./api-client";
import {
  isLabSchema,
  labColumnsForTable,
  labTablesForSchema,
  LAB_SCHEMA_FALLBACKS,
  type LabColumnEntry,
  type LabTableEntry,
} from "./lab-metadata";

export type LabFetchResult<T> = {
  data: T;
  fromFallback: boolean;
  error?: string;
};

export async function fetchLabSchemas(): Promise<LabFetchResult<string[]>> {
  try {
    const r = await apiFetch<{ schemas: string[] }>("/api/lab/schemas");
    const schemas = r.schemas ?? [];
    if (schemas.length > 0) {
      return { data: schemas, fromFallback: false };
    }
    return {
      data: [...LAB_SCHEMA_FALLBACKS],
      fromFallback: true,
      error: "Lab schemas not found in database — showing defaults. Install Lab Dev Tools from the marketplace.",
    };
  } catch (e) {
    return {
      data: [...LAB_SCHEMA_FALLBACKS],
      fromFallback: true,
      error: e instanceof Error ? e.message : "Lab database unavailable",
    };
  }
}

export async function fetchLabTables(schema: string): Promise<LabFetchResult<LabTableEntry[]>> {
  if (!isLabSchema(schema)) {
    return { data: [], fromFallback: false, error: `Not a lab schema: ${schema}` };
  }
  try {
    const r = await apiFetch<{ tables: LabTableEntry[] }>(
      `/api/lab/schemas/${encodeURIComponent(schema)}/tables`
    );
    const tables = r.tables ?? [];
    if (tables.length > 0) {
      return { data: tables, fromFallback: false };
    }
    const fallback = labTablesForSchema(schema);
    return {
      data: fallback,
      fromFallback: true,
      error:
        fallback.length > 0
          ? `No tables in schema "${schema}" yet — showing lab defaults. Install Lab Dev Tools to create them.`
          : `No tables in schema "${schema}".`,
    };
  } catch (e) {
    const fallback = labTablesForSchema(schema);
    return {
      data: fallback,
      fromFallback: true,
      error: e instanceof Error ? e.message : "Failed to load lab tables",
    };
  }
}

export async function fetchLabColumns(
  schema: string,
  table: string
): Promise<LabFetchResult<LabColumnEntry[]>> {
  if (!isLabSchema(schema)) {
    return { data: [], fromFallback: false };
  }
  try {
    const r = await apiFetch<{ columns: LabColumnEntry[] }>(
      `/api/lab/schemas/${encodeURIComponent(schema)}/tables/${encodeURIComponent(table)}/columns`
    );
    const columns = r.columns ?? [];
    if (columns.length > 0) {
      return { data: columns, fromFallback: false };
    }
    const fallback = labColumnsForTable(table);
    return {
      data: fallback,
      fromFallback: true,
      error:
        fallback.length > 0
          ? `Using default column list for ${table} — lab DB may be empty.`
          : undefined,
    };
  } catch (e) {
    const fallback = labColumnsForTable(table);
    return {
      data: fallback,
      fromFallback: true,
      error: e instanceof Error ? e.message : "Failed to load lab columns",
    };
  }
}
