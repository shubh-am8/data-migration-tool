import { DOC_REGISTRY, listDocSlugs, resolveDocPath } from "./registry";

test("allowlisted slug resolves under docs/", () => {
  const p = resolveDocPath("adding-a-connector");
  expect(p.replace(/\\/g, "/")).toMatch(/docs\/connectors\/adding-a-connector\.md$/);
});

test("architecture slug resolves under docs/", () => {
  const p = resolveDocPath("architecture");
  expect(p.replace(/\\/g, "/")).toMatch(/docs\/architecture\.md$/);
});

test("unknown slug returns null", () => {
  expect(resolveDocPath("../../../etc/passwd")).toBeNull();
});

test("registry includes expanded doc slugs", () => {
  for (const slug of [
    "architecture",
    "development",
    "deployment",
    "configuration",
    "marketplace",
    "connectors-overview",
    "adding-a-connector",
    "api",
    "worker",
    "frontend",
    "connector-sdk",
  ]) {
    expect(DOC_REGISTRY[slug]).toBeDefined();
  }
  expect(listDocSlugs()).toHaveLength(11);
});
