import { notFound } from "next/navigation";
import { AppShell } from "@/components/layout/AppShell";
import { MarkdownDoc } from "@/components/shared/MarkdownDoc";
import { PageHeader } from "@/components/shared/PageHeader";
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
  let doc: Awaited<ReturnType<typeof loadDoc>>;
  try {
    doc = await loadDoc(slug);
  } catch {
    notFound();
  }
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
