import path from "path";

export const DOC_REGISTRY: Record<string, { title: string; relativePath: string }> = {
  "adding-a-connector": {
    title: "Adding a Connector",
    relativePath: "connectors/adding-a-connector.md",
  },
  "connectors-overview": {
    title: "Connectors Overview",
    relativePath: "connectors/README.md",
  },
  marketplace: {
    title: "Marketplace",
    relativePath: "marketplace.md",
  },
};

/** Repo docs root: monorepo root /docs (Next cwd is apps/web). */
export function docsRoot(): string {
  return path.resolve(process.cwd(), "../../docs");
}

export function resolveDocPath(slug: string): string | null {
  const entry = DOC_REGISTRY[slug];
  if (!entry) return null;
  const root = docsRoot();
  const resolved = path.resolve(root, entry.relativePath);
  const rel = path.relative(root, resolved);
  if (rel.startsWith("..") || path.isAbsolute(rel)) return null;
  return resolved;
}

export function listDocSlugs(): string[] {
  return Object.keys(DOC_REGISTRY);
}
