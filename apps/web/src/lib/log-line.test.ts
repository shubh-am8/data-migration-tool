import { inferLevelFromText, lineClassForLevel, linesFromConnectionTest } from "./log-line";

describe("log-line", () => {
  it("maps levels to terminal colors", () => {
    expect(lineClassForLevel("success")).toContain("emerald");
    expect(lineClassForLevel("error")).toContain("red");
    expect(lineClassForLevel("warn")).toContain("amber");
  });

  it("builds lines from failed connection test", () => {
    const lines = linesFromConnectionTest({
      success: false,
      message: "Connection refused",
      latencyMs: 42,
    });
    expect(lines.some((l) => l.level === "error")).toBe(true);
    expect(lines[1].text).toContain("42ms");
  });

  it("infers warn level from message text", () => {
    expect(inferLevelFromText("SSL warn: certificate")).toBe("warn");
  });
});
