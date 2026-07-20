import { AppShell } from "@/components/layout/AppShell";
import { DocLink } from "@/components/shared/DocLink";
import { PageHeader } from "@/components/shared/PageHeader";
import { DOC_REGISTRY, listDocSlugs } from "@/lib/docs/registry";

export default function DocsIndexPage() {
  const slugs = listDocSlugs().sort((a, b) =>
    DOC_REGISTRY[a].title.localeCompare(DOC_REGISTRY[b].title)
  );

  return (
    <AppShell>
      <div className="mx-auto flex w-full max-w-3xl flex-col gap-6">
        <PageHeader title="Documentation" description="Product guides and reference" />
        <ul className="flex flex-col gap-2">
          {slugs.map((slug) => (
            <li key={slug}>
              <DocLink slug={slug} />
            </li>
          ))}
        </ul>
      </div>
    </AppShell>
  );
}
