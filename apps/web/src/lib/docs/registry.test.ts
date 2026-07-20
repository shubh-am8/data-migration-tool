import { DOC_REGISTRY, resolveDocPath } from "./registry";

test("allowlisted slug resolves under docs/", () => {
  const p = resolveDocPath("adding-a-connector");
  expect(p.replace(/\\/g, "/")).toMatch(/docs\/connectors\/adding-a-connector\.md$/);
});

test("unknown slug returns null", () => {
  expect(resolveDocPath("../../../etc/passwd")).toBeNull();
});

test("registry has adding-a-connector", () => {
  expect(DOC_REGISTRY["adding-a-connector"]).toBeDefined();
});
