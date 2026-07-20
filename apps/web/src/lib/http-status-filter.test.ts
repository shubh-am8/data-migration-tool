import { seriesKeys } from "./http-status-filter";

describe("seriesKeys", () => {
  it("returns all three series for 'all'", () => {
    expect(seriesKeys("all")).toEqual(["http2xx", "http4xx", "http5xx"]);
  });

  it("returns only the matching series for 2xx/4xx/5xx", () => {
    expect(seriesKeys("2xx")).toEqual(["http2xx"]);
    expect(seriesKeys("4xx")).toEqual(["http4xx"]);
    expect(seriesKeys("5xx")).toEqual(["http5xx"]);
  });
});
