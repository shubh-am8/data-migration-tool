import { isAutoRefreshRoute } from "./auto-refresh-routes";

describe("isAutoRefreshRoute", () => {
  it("allows dashboard and infra", () => {
    expect(isAutoRefreshRoute("/dashboard")).toBe(true);
    expect(isAutoRefreshRoute("/infra")).toBe(true);
  });

  it("denies workers, jobs, connections", () => {
    expect(isAutoRefreshRoute("/workers")).toBe(false);
    expect(isAutoRefreshRoute("/jobs")).toBe(false);
    expect(isAutoRefreshRoute("/jobs/new")).toBe(false);
    expect(isAutoRefreshRoute("/connections")).toBe(false);
  });
});
