/** Mirrors connector-sdk ColumnDataType + FilterSpec operator sets. */
export type ColumnDataType = "NUMERIC" | "TEXT" | "TIMESTAMP" | "BOOLEAN" | "OTHER";

export type FilterOperator =
  | "EQ"
  | "NE"
  | "LT"
  | "LTE"
  | "GT"
  | "GTE"
  | "BETWEEN"
  | "IN"
  | "LIKE"
  | "ILIKE"
  | "IS_NULL";

export function columnDataTypeFromSql(sqlType: string | null | undefined): ColumnDataType {
  if (!sqlType) return "OTHER";
  const t = sqlType.toLowerCase();
  if (
    t.includes("int") ||
    t.includes("numeric") ||
    t.includes("decimal") ||
    t.includes("float") ||
    t.includes("double") ||
    t.includes("real") ||
    t.includes("serial")
  ) {
    return "NUMERIC";
  }
  if (t.includes("char") || t.includes("text") || t.includes("uuid")) {
    return "TEXT";
  }
  if (t.includes("timestamp") || t.includes("date") || t.includes("time")) {
    return "TIMESTAMP";
  }
  if (t.includes("bool")) {
    return "BOOLEAN";
  }
  return "OTHER";
}

export function operatorsForColumnType(type: ColumnDataType): FilterOperator[] {
  switch (type) {
    case "NUMERIC":
      return ["EQ", "NE", "LT", "LTE", "GT", "GTE", "BETWEEN", "IN", "IS_NULL"];
    case "TEXT":
      return ["EQ", "NE", "LIKE", "ILIKE", "IN", "IS_NULL"];
    case "TIMESTAMP":
      return ["EQ", "LT", "LTE", "GT", "GTE", "BETWEEN", "IS_NULL"];
    case "BOOLEAN":
      return ["EQ", "IS_NULL"];
    default:
      return ["EQ", "IS_NULL"];
  }
}

export function isOperatorValidForType(op: FilterOperator, type: ColumnDataType): boolean {
  return operatorsForColumnType(type).includes(op);
}

export function defaultOperatorForColumnType(type: ColumnDataType): FilterOperator {
  return operatorsForColumnType(type)[0] ?? "EQ";
}

export function defaultValuesForOperator(op: FilterOperator): string[] {
  if (op === "IS_NULL") return [];
  if (op === "BETWEEN") return ["", ""];
  return [""];
}
