import {
  columnDataTypeFromSql,
  defaultOperatorForColumnType,
  isOperatorValidForType,
  type FilterOperator,
} from "./column-data-type";

export type FilterRow = {
  column: string;
  operator: string;
  values: string[];
};

export type ColumnMeta = { name: string; dataType: string };

export type FilterValidation = { ok: true } | { ok: false; message: string };

export function columnTypeForName(columns: ColumnMeta[], column: string) {
  const meta = columns.find((c) => c.name === column);
  return columnDataTypeFromSql(meta?.dataType);
}

export function normalizeFilterRow(row: FilterRow, columns: ColumnMeta[]): FilterRow {
  const type = columnTypeForName(columns, row.column);
  const operator = isOperatorValidForType(row.operator as FilterOperator, type)
    ? (row.operator as FilterOperator)
    : defaultOperatorForColumnType(type);
  if (operator === "IS_NULL") {
    return { ...row, operator, values: [] };
  }
  if (operator === "BETWEEN") {
    return { ...row, operator, values: [row.values[0] ?? "", row.values[1] ?? ""] };
  }
  if (operator === "IN") {
    const raw = row.values[0] ?? "";
    const values = raw
      .split(",")
      .map((v) => v.trim())
      .filter(Boolean);
    return { ...row, operator, values: values.length ? values : [""] };
  }
  return { ...row, operator, values: [row.values[0] ?? ""] };
}

export function validateFilterRows(filters: FilterRow[], columns: ColumnMeta[]): FilterValidation {
  if (filters.length === 0) return { ok: true };
  if (columns.length === 0) {
    return { ok: false, message: "Columns are not loaded — go back and select a table" };
  }
  const names = new Set(columns.map((c) => c.name));
  for (let i = 0; i < filters.length; i++) {
    const f = filters[i]!;
    if (!f.column || !names.has(f.column)) {
      return { ok: false, message: `Filter ${i + 1}: select a column from the table` };
    }
    const type = columnTypeForName(columns, f.column);
    if (!isOperatorValidForType(f.operator as FilterOperator, type)) {
      return { ok: false, message: `Filter ${i + 1}: operator is not valid for column type` };
    }
    if (f.operator === "IS_NULL") continue;
    if (f.operator === "BETWEEN") {
      if (!f.values[0]?.trim() || !f.values[1]?.trim()) {
        return { ok: false, message: `Filter ${i + 1}: Between requires start and end values` };
      }
      continue;
    }
    if (f.operator === "IN") {
      const parts = (f.values[0] ?? "")
        .split(",")
        .map((v) => v.trim())
        .filter(Boolean);
      if (parts.length === 0) {
        return { ok: false, message: `Filter ${i + 1}: In list requires at least one value` };
      }
      continue;
    }
    if (!f.values[0]?.trim()) {
      return { ok: false, message: `Filter ${i + 1}: value is required` };
    }
  }
  return { ok: true };
}
