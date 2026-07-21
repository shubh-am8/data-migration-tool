import {
  columnDataTypeFromSql,
  isOperatorValidForType,
  operatorsForColumnType,
} from "./column-data-type";

describe("columnDataTypeFromSql", () => {
  it("classifies numeric types", () => {
    expect(columnDataTypeFromSql("bigint")).toBe("NUMERIC");
    expect(columnDataTypeFromSql("double precision")).toBe("NUMERIC");
  });

  it("classifies text and uuid", () => {
    expect(columnDataTypeFromSql("varchar")).toBe("TEXT");
    expect(columnDataTypeFromSql("uuid")).toBe("TEXT");
  });

  it("classifies timestamps", () => {
    expect(columnDataTypeFromSql("timestamp with time zone")).toBe("TIMESTAMP");
    expect(columnDataTypeFromSql("date")).toBe("TIMESTAMP");
  });
});

describe("operatorsForColumnType", () => {
  it("allows BETWEEN for numeric and timestamp only", () => {
    expect(operatorsForColumnType("NUMERIC")).toContain("BETWEEN");
    expect(operatorsForColumnType("TIMESTAMP")).toContain("BETWEEN");
    expect(operatorsForColumnType("TEXT")).not.toContain("BETWEEN");
  });

  it("matches FilterSpec rules", () => {
    expect(isOperatorValidForType("GTE", "NUMERIC")).toBe(true);
    expect(isOperatorValidForType("LIKE", "NUMERIC")).toBe(false);
    expect(isOperatorValidForType("ILIKE", "TEXT")).toBe(true);
    expect(isOperatorValidForType("IN", "TIMESTAMP")).toBe(false);
  });
});
