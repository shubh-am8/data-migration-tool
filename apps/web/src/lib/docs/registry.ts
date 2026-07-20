import path from "path";

export const DOC_REGISTRY: Record<string, { title: string; relativePath: string }> = {
  architecture: {
    title: "Architecture",
    relativePath: "architecture.md",
  },
  development: {
    title: "Development",
    relativePath: "development.md",
  },
  deployment: {
    title: "Deployment",
    relativePath: "deployment.md",
  },
  configuration: {
    title: "Configuration",
    relativePath: "configuration.md",
  },
  marketplace: {
    title: "Marketplace",
    relativePath: "marketplace.md",
  },
  "connectors-overview": {
    title: "Connectors Overview",
    relativePath: "connectors/README.md",
  },
  "adding-a-connector": {
    title: "Adding a Connector",
    relativePath: "connectors/adding-a-connector.md",
  },
  api: {
    title: "API Service",
    relativePath: "components/api.md",
  },
  worker: {
    title: "Worker Service",
    relativePath: "components/worker.md",
  },
  frontend: {
    title: "Frontend",
    relativePath: "components/frontend.md",
  },
  "connector-sdk": {
    title: "Connector SDK",
    relativePath: "components/connector-sdk.md",
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

/** Rewrite relative .md hrefs to in-app /docs/{slug} when registered. */
export function hrefToDocSlug(href: string | undefined): string | undefined {
  if (!href || /^https?:\/\//i.test(href)) return href;

  const normalized = href.replace(/^\.\//, "");

  if (normalized === "README.md" || normalized === "docs/README.md" || normalized === "../README.md") {
    return "/docs";
  }

  for (const [slug, { relativePath }] of Object.entries(DOC_REGISTRY)) {
    if (
      normalized === relativePath ||
      normalized.endsWith(`/${relativePath}`) ||
      normalized.endsWith(relativePath)
    ) {
      return `/docs/${slug}`;
    }
  }

  const basename = normalized.split("/").pop();
  if (basename?.endsWith(".md")) {
    const matches = Object.entries(DOC_REGISTRY).filter(
      ([, { relativePath }]) => relativePath === basename || relativePath.endsWith(`/${basename}`)
    );
    if (matches.length === 1) return `/docs/${matches[0][0]}`;
  }

  return href;
}
