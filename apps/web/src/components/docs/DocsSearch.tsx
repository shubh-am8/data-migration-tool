"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { Input } from "@/components/ui/input";
import type { DocSearchEntry } from "@/lib/docs/search-index";

function matchesQuery(entry: DocSearchEntry, q: string): boolean {
  const haystack = [entry.title, ...entry.headings, entry.text].join("\n").toLowerCase();
  return haystack.includes(q);
}

export function DocsSearch({ index }: { index: DocSearchEntry[] }) {
  const [query, setQuery] = useState("");

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return [];
    return index.filter((entry) => matchesQuery(entry, q));
  }, [index, query]);

  return (
    <div className="flex flex-col gap-2">
      <Input
        type="search"
        placeholder="Search docs…"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Escape") setQuery("");
        }}
        aria-label="Search documentation"
      />
      {results.length > 0 && (
        <ul className="flex flex-col gap-0.5">
          {results.map((entry) => (
            <li key={entry.slug}>
              <Link
                href={`/docs/${entry.slug}`}
                className="block rounded-md px-3 py-1.5 text-sm transition-colors hover:bg-muted"
              >
                {entry.title}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
