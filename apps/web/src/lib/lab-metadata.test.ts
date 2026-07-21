import { LAB_DEST_SCHEMA, LAB_SCHEMAS, LAB_SOURCE_SCHEMA } from "./simulation-options";
import { labColumnsForTable, labTablesForSchema, LAB_SCHEMA_FALLBACKS } from "./lab-metadata";

describe("lab-metadata", () => {
  it("exposes test_source for wizard", () => {
    expect(LAB_SCHEMA_FALLBACKS).toEqual([LAB_SOURCE_SCHEMA]);
    expect(LAB_SCHEMAS).toEqual(["test_source"]);
  });

  it("returns lab tables for test_source schema", () => {
    const tables = labTablesForSchema(LAB_SOURCE_SCHEMA);
    expect(tables.map((t) => t.name)).toEqual(["orders_cold", "orders_hot_cold"]);
  });

  it("returns empty tables for test_destination", () => {
    expect(labTablesForSchema(LAB_DEST_SCHEMA)).toEqual([]);
  });

  it("returns columns for orders_cold", () => {
    const cols = labColumnsForTable("orders_cold");
    expect(cols.some((c) => c.name === "created_at")).toBe(true);
  });
});
