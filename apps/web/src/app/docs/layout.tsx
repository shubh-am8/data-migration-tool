import { AppShell } from "@/components/layout/AppShell";
import { DocsShell } from "@/components/docs/DocsShell";

// future: public docs when DOCS_PUBLIC=true
export default function DocsLayout({ children }: { children: React.ReactNode }) {
  return (
    <AppShell>
      <DocsShell>{children}</DocsShell>
    </AppShell>
  );
}
