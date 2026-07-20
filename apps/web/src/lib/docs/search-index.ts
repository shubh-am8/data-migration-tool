import "server-only";

import { loadDoc } from "./load-doc";
import { listDocSlugs } from "./registry";

export type DocSearchEntry = {
  slug: string;
  title: string;
  headings: string[];
  text: string;
};

/** Extract `##` section headings (not `#` or `###`). */
export function extractHeadings(markdown: string): string[] {
  const headings: string[] = [];
  for (const line of markdown.split("\n")) {
    const match = /^##\s+(.+)$/.exec(line);
    if (match) headings.push(match[1].trim());
  }
  return headings;
}

/** Flatten markdown to searchable plain text, capped at ~8k chars. */
export function flattenMarkdown(markdown: string, maxChars = 8000): string {
  let text = markdown;
  text = text.replace(/```mermaid[\s\S]*?```/gi, " ");
  text = text.replace(/```[\s\S]*?```/g, " ");
  text = text.replace(/`[^`]+`/g, " ");
  text = text.replace(/^#{1,6}\s+/gm, "");
  text = text.replace(/!\[([^\]]*)\]\([^)]+\)/g, "$1");
  text = text.replace(/\[([^\]]+)\]\([^)]+\)/g, "$1");
  text = text.replace(/[*_]{1,3}([^*_]+)[*_]{1,3}/g, "$1");
  text = text.replace(/\|/g, " ");
  text = text.replace(/\s+/g, " ").trim();
  if (text.length > maxChars) text = text.slice(0, maxChars);
  return text;
}

export async function buildDocsSearchIndex(): Promise<DocSearchEntry[]> {
  const entries: DocSearchEntry[] = [];
  for (const slug of listDocSlugs()) {
    const doc = await loadDoc(slug);
    if (!doc) continue;
    entries.push({
      slug,
      title: doc.title,
      headings: extractHeadings(doc.markdown),
      text: flattenMarkdown(doc.markdown),
    });
  }
  return entries;
}
