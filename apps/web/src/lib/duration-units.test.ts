import { durationToHours, hoursToDisplay } from "./duration-units";
import { isoToLocalDateTime, localDateTimeToIso } from "./datetime-tz";

describe("duration-units", () => {
  it("converts days to hours", () => {
    expect(durationToHours(2, "days")).toBe(48);
  });

  it("displays 48 hours as 2 days", () => {
    expect(hoursToDisplay(48)).toEqual({ value: 2, unit: "days" });
  });
});

describe("datetime-tz", () => {
  it("round-trips UTC local datetime", () => {
    const iso = localDateTimeToIso("2024-06-01T12:30", "UTC");
    expect(iso).toContain("2024-06-01T12:30:00");
    expect(isoToLocalDateTime(iso, "UTC")).toBe("2024-06-01T12:30");
  });
});
