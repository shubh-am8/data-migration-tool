# In-App Docs, Confirm Dialog, and List UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a secure allowlisted in-app docs viewer (marketplace “Build your own” opens it), a reusable email-typed confirmation dialog for critical actions (Revoke/Delete), fix Connections empty-CTA logic, fix PaginationBar alignment, and document the patterns in `.cursor/rules/ui-patterns.mdc`.

**Architecture:** Bundle repo docs behind an allowlist registry (no arbitrary path reads). Render with `react-markdown` (safe by default; no `rehype-raw`). Add `EmailConfirmDialog` on shadcn `Dialog` showing target avatar/name/email and requiring exact email match before `onConfirm`. Branch Connections empty CTA on installed-connector fetch. Align `PaginationBar` controls with `items-end` and hide when empty.

**Tech Stack:** Next.js 16 App Router, React 19, shadcn Dialog/Avatar/Field/Input, `react-markdown` + `remark-gfm`, existing `apiFetch` / `notify` / `DataTable`.

## Global Constraints

- Follow `.cursor/rules/ui-patterns.mdc` and `.cursor/rules/ponytail.mdc` (minimal diff, reuse shadcn, no ad-hoc spinners).
- Docs: allowlist slugs only; never open filesystem paths from user input; no `rehype-raw`; keep default `urlTransform` (blocks `javascript:`).
- Confirm: critical actions (Revoke, Delete) must not run until typed email matches target email (case-insensitive trim).
- Empty CTAs: Marketplace when no connectors; Add Connection when connectors exist but no connections.
- Spacing: `flex` + `gap-*` only; no new `space-y-*`.
- New dependency allowed: `react-markdown` and `remark-gfm` only (no MDX toolchain).
- Update `.cursor/rules/ui-patterns.mdc` with reusable-component section in the same plan.

## Code Review Findings (bugs / gaps)

| Severity | Issue | Location |
|----------|-------|----------|
| High | Connections empty CTA always says “Install a connector first” even when connectors are installed | `apps/web/src/app/connections/page.tsx:126-134` |
| High | Users Revoke has no confirmation; Delete uses native `confirm()` only | `apps/web/src/app/users/page.tsx:55-74` |
| Medium | Marketplace “Build your own” docs CTA is a disabled button (path overflows pill) | `apps/web/src/app/connectors/marketplace/client.tsx:158-173` |
| Medium | No in-app docs route; repo markdown is not rendered | `apps/web` |
| Low | `PaginationBar` Rows `Field` stacks above Prev/Next → misaligned controls | `apps/web/src/components/shared/PaginationBar.tsx:60-77` |
| Low | Pagination still shown when table empty | `DataTable.tsx` + `PaginationBar` |

## File Structure

| File | Responsibility |
|------|----------------|
| `apps/web/src/lib/docs/registry.ts` | Allowlist slug → relative path under repo `docs/` |
| `apps/web/src/lib/docs/load-doc.ts` | Server-only safe file load + path traversal guard |
| `apps/web/src/components/shared/MarkdownDoc.tsx` | Reusable markdown renderer |
| `apps/web/src/components/shared/DocLink.tsx` | Truncating pill/link to `/docs/[slug]` |
| `apps/web/src/app/docs/[slug]/page.tsx` | Docs page inside AppShell |
| `apps/web/src/components/shared/EmailConfirmDialog.tsx` | Reusable confirm with profile + email type-to-confirm |
| `apps/web/src/app/connectors/marketplace/client.tsx` | Wire DocLink; make card open docs |
| `apps/web/src/app/connections/page.tsx` | Branch empty CTA on installed connectors |
| `apps/web/src/app/users/page.tsx` | Wire EmailConfirmDialog for Revoke/Delete |
| `apps/web/src/components/shared/PaginationBar.tsx` | Alignment + optional hide when empty |
| `apps/web/src/components/shared/DataTable.tsx` | Don’t show pagination when `totalElements === 0` |
| `.cursor/rules/ui-patterns.mdc` | Document reusable patterns |
| Tests under `apps/web/src/components/shared/*.test.tsx` | Dialog + registry guards |

---

### Task 1: Secure allowlisted docs loader + MarkdownDoc

**Files:**
- Create: `apps/web/src/lib/docs/registry.ts`
- Create: `apps/web/src/lib/docs/load-doc.ts`
- Create: `apps/web/src/components/shared/MarkdownDoc.tsx`
- Create: `apps/web/src/lib/docs/registry.test.ts`
- Modify: `apps/web/package.json` (add `react-markdown`, `remark-gfm`)

**Interfaces:**
- Consumes: repo files `docs/connectors/adding-a-connector.md`, `docs/connectors/README.md`, `docs/marketplace.md`
- Produces:
  - `DOC_REGISTRY: Record<string, { title: string; relativePath: string }>`
  - `listDocSlugs(): string[]`
  - `loadDoc(slug: string): Promise<{ title: string; markdown: string }>` (throws / returns null for unknown)
  - `MarkdownDoc({ markdown }: { markdown: string })`

- [ ] **Step 1: Install deps**

```bash
cd apps/web && pnpm add react-markdown remark-gfm
# or: npm install react-markdown remark-gfm
```

- [ ] **Step 2: Write failing registry test**

```ts
// apps/web/src/lib/docs/registry.test.ts
import { DOC_REGISTRY, resolveDocPath } from "./registry";

test("allowlisted slug resolves under docs/", () => {
  const p = resolveDocPath("adding-a-connector");
  expect(p.replace(/\\/g, "/")).toMatch(/docs\/connectors\/adding-a-connector\.md$/);
});

test("unknown slug returns null") => {
  expect(resolveDocPath("../../../etc/passwd")).toBeNull();
});

test("registry has adding-a-connector") => {
  expect(DOC_REGISTRY["adding-a-connector"]).toBeDefined();
};
```

- [ ] **Step 3: Run test — expect FAIL**

Run: `cd apps/web && npm test -- --testPathPattern=registry.test`

- [ ] **Step 4: Implement registry + loader**

```ts
// apps/web/src/lib/docs/registry.ts
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
```

```ts
// apps/web/src/lib/docs/load-doc.ts
import "server-only";
import fs from "fs/promises";
import { DOC_REGISTRY, resolveDocPath } from "./registry";

export async function loadDoc(slug: string): Promise<{ title: string; markdown: string } | null> {
  const entry = DOC_REGISTRY[slug];
  const filePath = resolveDocPath(slug);
  if (!entry || !filePath) return null;
  const markdown = await fs.readFile(filePath, "utf8");
  return { title: entry.title, markdown };
}
```

```tsx
// apps/web/src/components/shared/MarkdownDoc.tsx
"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

export function MarkdownDoc({ markdown }: { markdown: string }) {
  return (
    <article className="prose prose-neutral dark:prose-invert max-w-none flex flex-col gap-4 text-sm [&_pre]:overflow-x-auto [&_code]:break-words">
      <ReactMarkdown remarkPlugins={[remarkGfm]} skipHtml>
        {markdown}
      </ReactMarkdown>
    </article>
  );
}
```

If `prose` classes are unavailable (no typography plugin), use semantic Tailwind on `components` prop instead — prefer composing existing text utilities without adding `@tailwindcss/typography` unless already present.

- [ ] **Step 5: Run tests — expect PASS**

Run: `cd apps/web && npm test -- --testPathPattern=registry.test`

- [ ] **Step 6: Commit**

```bash
git add apps/web/package.json apps/web/pnpm-lock.yaml apps/web/package-lock.json \
  apps/web/src/lib/docs apps/web/src/components/shared/MarkdownDoc.tsx
git commit -m "feat: add allowlisted markdown docs loader"
```

---

### Task 2: Docs route + DocLink + marketplace card

**Files:**
- Create: `apps/web/src/app/docs/[slug]/page.tsx`
- Create: `apps/web/src/components/shared/DocLink.tsx`
- Modify: `apps/web/src/app/connectors/marketplace/client.tsx` (Build your own card)
- Modify: `apps/web/src/components/layout/AppSidebar.tsx` only if a Docs nav entry is desired — **skip nav** (YAGNI); deep-link from marketplace is enough

**Interfaces:**
- Consumes: `loadDoc`, `MarkdownDoc`, `DOC_REGISTRY`
- Produces: route `/docs/[slug]`; `DocLink({ slug, children? })`

- [ ] **Step 1: Implement DocLink (responsive pill)**

```tsx
// apps/web/src/components/shared/DocLink.tsx
"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { DOC_REGISTRY } from "@/lib/docs/registry";

export function DocLink({
  slug,
  children,
}: {
  slug: keyof typeof DOC_REGISTRY | string;
  children?: React.ReactNode;
}) {
  const title = DOC_REGISTRY[slug]?.title ?? String(slug);
  return (
    <Button asChild variant="outline" size="sm" className="max-w-full">
      <Link
        href={`/docs/${slug}`}
        className="min-w-0 max-w-full truncate"
        title={title}
      >
        {children ?? title}
      </Link>
    </Button>
  );
}
```

If `Button asChild` is unavailable in this shadcn build, use:

```tsx
<Link href={`/docs/${slug}`} className="inline-flex max-w-full ...">
  <span className="truncate">{children ?? title}</span>
</Link>
```

- [ ] **Step 2: Docs page**

```tsx
// apps/web/src/app/docs/[slug]/page.tsx
import { notFound } from "next/navigation";
import { AppShell } from "@/components/layout/AppShell";
import { PageHeader } from "@/components/shared/PageHeader";
import { MarkdownDoc } from "@/components/shared/MarkdownDoc";
import { loadDoc } from "@/lib/docs/load-doc";
import { listDocSlugs } from "@/lib/docs/registry";

export function generateStaticParams() {
  return listDocSlugs().map((slug) => ({ slug }));
}

export default async function DocPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const doc = await loadDoc(slug);
  if (!doc) notFound();

  return (
    <AppShell>
      <div className="mx-auto flex w-full max-w-3xl flex-col gap-6">
        <PageHeader title={doc.title} description="Product documentation" />
        <MarkdownDoc markdown={doc.markdown} />
      </div>
    </AppShell>
  );
}
```

- [ ] **Step 3: Wire marketplace card**

Replace disabled button block with clickable card/link:

```tsx
<Card className="flex flex-col">
  <CardHeader>
    <CardTitle>Build your own</CardTitle>
    <CardDescription>
      Implement the ConnectorPlugin SPI, package a JAR, and upload it here.
    </CardDescription>
  </CardHeader>
  <CardContent className="flex min-w-0 flex-col gap-2">
    <DocLink slug="adding-a-connector" />
    <p className="text-sm text-muted-foreground">
      Open the guide in-app, or read the same file in the repo under{" "}
      <code className="break-all">docs/connectors/</code>.
    </p>
  </CardContent>
</Card>
```

Optionally wrap the whole card in `Link` to `/docs/adding-a-connector` for one-click open — prefer DocLink CTA to avoid nested interactive elements.

- [ ] **Step 4: Verify**

Run: `cd apps/web && npm run build` (or `npm test` + manual `/docs/adding-a-connector`)
Expected: page renders markdown; unknown slug → 404; DocLink truncates on narrow cards.

- [ ] **Step 5: Commit**

```bash
git add apps/web/src/app/docs apps/web/src/components/shared/DocLink.tsx \
  apps/web/src/app/connectors/marketplace/client.tsx
git commit -m "feat: in-app docs page and marketplace DocLink"
```

---

### Task 3: EmailConfirmDialog reusable component

**Files:**
- Create: `apps/web/src/components/shared/EmailConfirmDialog.tsx`
- Create: `apps/web/src/components/shared/EmailConfirmDialog.test.tsx`
- Modify: `apps/web/src/app/users/page.tsx`

**Interfaces:**
- Consumes: shadcn `Dialog`, `Avatar`, `Field`, `Input`, `Button`; target `{ name, email, pictureUrl? }`
- Produces: `EmailConfirmDialog` props:
  - `open: boolean`
  - `onOpenChange: (open: boolean) => void`
  - `title: string`
  - `description: string`
  - `confirmLabel: string`
  - `confirmVariant?: "danger" | "warning"`
  - `subject: { name: string; email: string; pictureUrl?: string | null }`
  - `onConfirm: () => void | Promise<void>`
  - Confirm button disabled until `input.trim().toLowerCase() === subject.email.trim().toLowerCase()`

- [ ] **Step 1: Failing test — confirm disabled until email matches**

```tsx
// EmailConfirmDialog.test.tsx — RTL
// render with subject.email = "a@b.com"
// assert Confirm button disabled
// type "a@b.com" → enabled
```

- [ ] **Step 2: Implement dialog**

Use existing `dialog.tsx`. Show Avatar + name + email. Field label: `Type email to confirm`. Danger/warning confirm button.

- [ ] **Step 3: Wire Users page**

Replace immediate `revoke` / `confirm()` delete with state:

```ts
type Pending =
  | { type: "revoke"; user: AdminUser }
  | { type: "delete"; user: AdminUser }
  | null;
```

On Revoke/Delete click → set pending. Dialog `onConfirm` calls API then clears pending + `load()`.

- [ ] **Step 4: Run tests**

Run: `cd apps/web && npm test -- --testPathPattern=EmailConfirmDialog`

- [ ] **Step 5: Commit**

```bash
git add apps/web/src/components/shared/EmailConfirmDialog.tsx \
  apps/web/src/components/shared/EmailConfirmDialog.test.tsx \
  apps/web/src/app/users/page.tsx
git commit -m "feat: email-typed confirm dialog for revoke and delete"
```

---

### Task 4: Connections empty CTA + PaginationBar UX

**Files:**
- Modify: `apps/web/src/app/connections/page.tsx`
- Modify: `apps/web/src/components/shared/PaginationBar.tsx`
- Modify: `apps/web/src/components/shared/DataTable.tsx`

**Interfaces:**
- Consumes: `GET /api/marketplace` → `installed ?? enabled` (same as `connections/new/client.tsx`)
- Produces: branched empty CTA; aligned pagination; hide pagination when `totalElements === 0`

- [ ] **Step 1: Connections page — fetch installed count**

```ts
const [hasInstalledConnector, setHasInstalledConnector] = useState<boolean | null>(null);

useEffect(() => {
  apiFetch<{ id: string; installed?: boolean; enabled?: boolean }[]>("/api/marketplace")
    .then((list) => {
      setHasInstalledConnector(list.some((p) => p.installed ?? p.enabled));
    })
    .catch(() => setHasInstalledConnector(false));
}, []);
```

Empty CTA:

```tsx
empty={
  <div className="flex flex-col items-center gap-3">
    <p className="text-sm text-muted-foreground">No connections yet.</p>
    {hasInstalledConnector ? (
      <Link href="/connections/new">
        <Button variant="outline" size="sm">Add a connection</Button>
      </Link>
    ) : (
      <Link href="/connectors/marketplace">
        <Button variant="outline" size="sm">Install a connector first</Button>
      </Link>
    )}
  </div>
}
```

- [ ] **Step 2: PaginationBar alignment**

Replace stacked `Field`/`FieldLabel` with inline label so Prev/Next share one baseline:

```tsx
<div className="flex flex-wrap items-center justify-between gap-3">
  <p className="text-sm text-muted-foreground">...</p>
  <div className="flex flex-wrap items-center gap-2">
    <label className="flex items-center gap-2 text-xs text-muted-foreground">
      <span>Rows</span>
      <Select ...>
        <SelectTrigger className="h-7 w-[4.5rem]">
          <SelectValue />
        </SelectTrigger>
        ...
      </Select>
    </label>
    <Button ...>Prev</Button>
    <Button ...>Next</Button>
  </div>
</div>
```

- [ ] **Step 3: DataTable — hide pagination when empty**

```tsx
{showPagination && totalElements > 0 && (
  <PaginationBar ... />
)}
```

- [ ] **Step 4: Verify**

Manual: with installed connector and 0 connections → “Add a connection”. Pagination hidden on empty table; controls vertically aligned when rows exist.

- [ ] **Step 5: Commit**

```bash
git add apps/web/src/app/connections/page.tsx \
  apps/web/src/components/shared/PaginationBar.tsx \
  apps/web/src/components/shared/DataTable.tsx
git commit -m "fix: connections empty CTA and pagination alignment"
```

---

### Task 5: Update ui-patterns rule

**Files:**
- Modify: `.cursor/rules/ui-patterns.mdc`

- [ ] **Step 1: Append reusable-component section**

```markdown
## Reusable components (prefer these)

- **Critical destructive actions** (revoke, delete, uninstall): use `EmailConfirmDialog` — show subject avatar/name/email; require typing that email before confirm. Never native `window.confirm` for admin-critical ops.
- **In-app docs**: register slugs in `lib/docs/registry.ts`; render via `/docs/[slug]` + `MarkdownDoc`. Link with `DocLink` (truncating pill). Never `fetch` arbitrary paths or enable `rehype-raw`.
- **Empty list CTAs**: if no installed connectors → Marketplace; if connectors exist but list empty → primary create route (e.g. Add Connection). Do not hardcode “Install a connector first” when connectors are installed.
- **Pagination**: `DataTable` + `PaginationBar`; hide bar when `totalElements === 0`; keep Rows select and Prev/Next on one `items-center` row.
```

Keep file under ~50 lines if possible; trim older bullets only if needed for length.

- [ ] **Step 2: Commit**

```bash
git add .cursor/rules/ui-patterns.mdc
git commit -m "docs: extend ui-patterns for confirm dialog and in-app docs"
```

---

## Self-Review

1. Spec coverage: docs UI, DocLink overflow, connections CTA bug, pagination UX, email confirm for Revoke/Delete, ui-patterns update — all have tasks.
2. Placeholders: none intentionally left.
3. Types: `EmailConfirmDialog` subject shape reused on Users page; doc slug keys match registry.

## Security notes (from research)

- Prefer allowlisted static docs over runtime path params into `fs`.
- `react-markdown` is safe by default; do not add `rehype-raw`.
- Keep default URL transform (blocks `javascript:`).
- Confirm dialog is UX control; server must still authorize revoke/delete (existing admin APIs).
