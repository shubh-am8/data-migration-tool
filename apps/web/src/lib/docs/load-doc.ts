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
