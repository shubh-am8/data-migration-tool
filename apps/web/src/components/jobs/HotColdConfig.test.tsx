import { render, screen } from "@testing-library/react";
import { HotColdConfig } from "./HotColdConfig";

describe("HotColdConfig", () => {
  it("renders range and chunk duration fields", () => {
    render(
      <HotColdConfig
        migrationMode="HOT_THEN_COLD"
        hotDays={7}
        tsColumn="created_at"
        rangeStart="2024-01-01T00:00"
        rangeEndMode="NOW"
        rangeEnd=""
        minChunkDurationHours={24}
        maxChunkDurationHours={168}
        onChange={jest.fn()}
      />
    );
    expect(screen.getByText("Range start")).toBeInTheDocument();
    expect(screen.getByText("Min chunk duration (hours)")).toBeInTheDocument();
    expect(screen.getByText("Max chunk duration (hours)")).toBeInTheDocument();
    expect(screen.getByText("Hot window (days)")).toBeInTheDocument();
  });
});
