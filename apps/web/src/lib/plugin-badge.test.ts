import { pluginBadgeVariant } from "./plugin-badge";

describe("pluginBadgeVariant", () => {
  it("colors postgresql as info", () => {
    expect(pluginBadgeVariant("postgresql")).toBe("info");
  });

  it("colors mysql as success", () => {
    expect(pluginBadgeVariant("mysql")).toBe("success");
  });

  it("falls back to secondary", () => {
    expect(pluginBadgeVariant("custom-connector")).toBe("secondary");
  });
});
