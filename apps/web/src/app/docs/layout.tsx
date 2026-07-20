import { AppShell } from "@/components/layout/AppShell";
import { DocsSearch } from "@/components/docs/DocsSearch";
import { DocsShell } from "@/components/docs/DocsShell";
import { buildDocsSearchIndex } from "@/lib/docs/search-index";

// future: public docs when DOCS_PUBLIC=true
export default async function DocsLayout({ children }: { children: React.ReactNode }) {
  const index = await buildDocsSearchIndex();
  return (
    <AppShell>
      <DocsShell searchSlot={<DocsSearch index={index} />}>{children}</DocsShell>
    </AppShell>
  );
}
