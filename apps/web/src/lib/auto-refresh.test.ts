import { AUTO_REFRESH_OPTIONS_MS, clampAutoRefreshMs, formatLastRefreshed } from "./auto-refresh";

describe("AUTO_REFRESH_OPTIONS_MS", () => {
  it("is Off + 10s..5m", () => {
    expect(AUTO_REFRESH_OPTIONS_MS).toEqual([0, 10_000, 30_000, 60_000, 120_000, 300_000]);
  });
});

describe("clampAutoRefreshMs", () => {
  it("passes through a valid option", () => {
    expect(clampAutoRefreshMs(30_000)).toBe(30_000);
  });

  it("falls back to 0 for values not in the option list", () => {
    expect(clampAutoRefreshMs(15_000)).toBe(0);
    expect(clampAutoRefreshMs(-5)).toBe(0);
    expect(clampAutoRefreshMs(NaN)).toBe(0);
  });
});

describe("formatLastRefreshed", () => {
  const now = new Date("2026-07-20T12:00:00.000Z");

  it("shows 'Just now' under a second old", () => {
    expect(formatLastRefreshed(new Date("2026-07-20T11:59:59.600Z"), now)).toBe("Just now");
  });

  it("shows seconds ago under a minute", () => {
    expect(formatLastRefreshed(new Date("2026-07-20T11:59:42.000Z"), now)).toBe("18s ago");
  });

  it("shows minutes ago under an hour", () => {
    expect(formatLastRefreshed(new Date("2026-07-20T11:45:00.000Z"), now)).toBe("15m ago");
  });

  it("falls back to a locale time string an hour or more ago", () => {
    const old = new Date("2026-07-20T09:00:00.000Z");
    expect(formatLastRefreshed(old, now)).toBe(old.toLocaleTimeString());
  });
});
