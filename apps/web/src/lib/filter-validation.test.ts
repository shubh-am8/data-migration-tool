import { validateFilterRows } from "./filter-validation";

const columns = [
  { name: "id", dataType: "bigint" },
  { name: "status", dataType: "varchar" },
  { name: "created_at", dataType: "timestamp with time zone" },
];

describe("validateFilterRows", () => {
  it("allows empty filters", () => {
    expect(validateFilterRows([], columns).ok).toBe(true);
  });

  it("rejects unknown column", () => {
    const r = validateFilterRows([{ column: "nope", operator: "EQ", values: ["x"] }], columns);
    expect(r.ok).toBe(false);
  });

  it("rejects LIKE on numeric column", () => {
    const r = validateFilterRows([{ column: "id", operator: "LIKE", values: ["1"] }], columns);
    expect(r.ok).toBe(false);
  });

  it("requires both values for BETWEEN", () => {
    const r = validateFilterRows(
      [{ column: "created_at", operator: "BETWEEN", values: ["2024-01-01", ""] }],
      columns
    );
    expect(r.ok).toBe(false);
  });

  it("accepts valid numeric BETWEEN", () => {
    const r = validateFilterRows(
      [{ column: "id", operator: "BETWEEN", values: ["1", "100"] }],
      columns
    );
    expect(r.ok).toBe(true);
  });
});
