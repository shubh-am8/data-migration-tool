import { DOC_REGISTRY, hrefToDocSlug, listDocSlugs, resolveDocPath } from "./registry";

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

const OSS_DOC_SLUGS = [
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
] as const;

test("registry includes expanded doc slugs", () => {
  for (const slug of OSS_DOC_SLUGS) {
    expect(DOC_REGISTRY[slug]).toBeDefined();
  }
  expect(listDocSlugs()).toHaveLength(OSS_DOC_SLUGS.length);
});

test("every allowlisted slug resolves under docs/", () => {
  for (const slug of OSS_DOC_SLUGS) {
    const p = resolveDocPath(slug);
    expect(p).not.toBeNull();
    expect(p!.replace(/\\/g, "/")).toMatch(/\/docs\/.+\.md$/);
  }
});

test("path traversal slugs return null", () => {
  for (const slug of ["../../../etc/passwd", "..%2F..%2Fetc%2Fpasswd", "architecture/../../etc/passwd"]) {
    expect(resolveDocPath(slug)).toBeNull();
  }
});

test("hrefToDocSlug rewrites registered markdown links", () => {
  expect(hrefToDocSlug("marketplace.md")).toBe("/docs/marketplace");
  expect(hrefToDocSlug("connectors/adding-a-connector.md")).toBe("/docs/adding-a-connector");
  expect(hrefToDocSlug("adding-a-connector.md")).toBe("/docs/adding-a-connector");
  expect(hrefToDocSlug("../marketplace.md")).toBe("/docs/marketplace");
  expect(hrefToDocSlug("README.md")).toBe("/docs");
  expect(hrefToDocSlug("../README.md")).toBe("/docs");
  expect(hrefToDocSlug("connectors/README.md")).toBe("/docs/connectors-overview");
  expect(hrefToDocSlug("https://example.com/foo")).toBe("https://example.com/foo");
  expect(hrefToDocSlug("CHANGELOG.md")).toBe("CHANGELOG.md");
});
