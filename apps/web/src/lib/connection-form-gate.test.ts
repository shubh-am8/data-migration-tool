import { canSaveConnection } from "./connection-form-gate";

describe("canSaveConnection", () => {
  const fpA = "host=localhost|port=5432";
  const fpB = "host=other|port=5432";

  it("blocks new connection until test passes on current config", () => {
    expect(canSaveConnection(true, "idle", null, fpA)).toBe(false);
    expect(canSaveConnection(true, "passed", fpA, fpA)).toBe(true);
    expect(canSaveConnection(true, "passed", fpA, fpB)).toBe(false);
  });

  it("blocks save when last test failed", () => {
    expect(canSaveConnection(false, "failed", fpA, fpA)).toBe(false);
  });

  it("allows save on edit when config unchanged", () => {
    expect(canSaveConnection(false, "idle", null, fpA)).toBe(true);
  });

  it("requires retest after config change on edit", () => {
    expect(canSaveConnection(true, "idle", fpA, fpB)).toBe(false);
    expect(canSaveConnection(true, "passed", fpB, fpB)).toBe(true);
  });
});
