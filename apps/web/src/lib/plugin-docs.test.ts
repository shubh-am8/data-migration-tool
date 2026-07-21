import { pluginDocSlug } from "./plugin-docs";

describe("pluginDocSlug", () => {
  it("maps postgresql to connectors-overview", () => {
    expect(pluginDocSlug("postgresql")).toBe("connectors-overview");
  });

  it("maps lab-devtools to development", () => {
    expect(pluginDocSlug("lab-devtools")).toBe("development");
  });

  it("falls back to marketplace", () => {
    expect(pluginDocSlug("custom-connector")).toBe("marketplace");
  });
});
