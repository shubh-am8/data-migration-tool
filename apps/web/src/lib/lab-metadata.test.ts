import { labColumnsForTable, labTablesForSchema, LAB_SCHEMA_FALLBACKS } from "./lab-metadata";

describe("lab-metadata", () => {
  it("exposes app and test schemas", () => {
    expect(LAB_SCHEMA_FALLBACKS).toEqual(["app", "test"]);
  });

  it("returns lab tables for app schema", () => {
    const tables = labTablesForSchema("app");
    expect(tables.map((t) => t.name)).toEqual(["orders_cold", "orders_hot_cold"]);
  });

  it("returns columns for orders_cold", () => {
    const cols = labColumnsForTable("orders_cold");
    expect(cols.some((c) => c.name === "created_at")).toBe(true);
  });
});
