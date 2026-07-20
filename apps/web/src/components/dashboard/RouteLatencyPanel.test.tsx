import { render, screen } from "@testing-library/react";

import { RouteLatencyPanel } from "./RouteLatencyPanel";
import type { HttpSnapshot } from "@/lib/http-types";

const route = (uri: string, meanMs: number) => ({
  method: "GET",
  uri,
  count: 10,
  meanMs,
  maxMs: meanMs * 2,
});

describe("RouteLatencyPanel", () => {
  it("shows an empty state without an alert when there is no choking route", () => {
    render(<RouteLatencyPanel />);
    expect(screen.getAllByText("No data yet")).toHaveLength(2);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("renders a destructive alert when choking routes are present", () => {
    const http: HttpSnapshot = {
      status: { "2xx": 1, "4xx": 0, "5xx": 0 },
      routes: [],
      slowest: [route("/api/slow", 800)],
      fastest: [route("/api/fast", 2)],
      choking: [route("/api/slow", 800)],
    };
    render(<RouteLatencyPanel http={http} />);
    expect(screen.getByRole("alert")).toHaveTextContent("1 route choking");
    expect(screen.getAllByText("/api/slow").length).toBeGreaterThan(0);
    expect(screen.getByText("/api/fast")).toBeInTheDocument();
  });
});
