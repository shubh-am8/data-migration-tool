import { render, screen } from "@testing-library/react";
import { HotColdConfig } from "./HotColdConfig";

describe("HotColdConfig", () => {
  it("renders sectioned range and chunk fields", () => {
    render(
      <HotColdConfig
        migrationMode="HOT_THEN_COLD"
        hotDays={7}
        tsColumn="created_at"
        rangeStart="2024-01-01T00:00:00.000Z"
        rangeStartTz="UTC"
        rangeEndMode="NOW"
        rangeEnd=""
        rangeEndTz="UTC"
        minChunkDurationHours={24}
        maxChunkDurationHours={168}
        onChange={jest.fn()}
      />
    );
    expect(screen.getByText("Range start")).toBeInTheDocument();
    expect(screen.getByText("Min chunk duration")).toBeInTheDocument();
    expect(screen.getByText("Max chunk duration")).toBeInTheDocument();
    expect(screen.getByText("Hot window (days)")).toBeInTheDocument();
    expect(screen.getByText("Migration mode")).toBeInTheDocument();
  });

  it("shows timestamp column for cold-only mode", () => {
    render(
      <HotColdConfig
        migrationMode="COLD_ONLY"
        hotDays={7}
        tsColumn="created_at"
        rangeStart="2024-01-01T00:00:00.000Z"
        rangeStartTz="UTC"
        rangeEndMode="NOW"
        rangeEnd=""
        rangeEndTz="UTC"
        minChunkDurationHours={24}
        maxChunkDurationHours={168}
        onChange={jest.fn()}
      />
    );
    expect(screen.getByText("Timestamp column *")).toBeInTheDocument();
    expect(screen.queryByText("Hot window (days)")).not.toBeInTheDocument();
  });
});
