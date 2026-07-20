"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { ChevronDownIcon, ChevronRightIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { DOC_NAV, DOC_REGISTRY } from "@/lib/docs/registry";

export function DocsNav({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname();
  const activeSlug = pathname.startsWith("/docs/") ? pathname.slice("/docs/".length) : null;

  const [expanded, setExpanded] = useState<Record<string, boolean>>(() =>
    Object.fromEntries(DOC_NAV.map((section) => [section.id, true]))
  );

  const toggle = (id: string) => {
    setExpanded((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <nav className="flex flex-col gap-1">
      {DOC_NAV.map((section) => {
        const isExpanded = expanded[section.id] ?? true;
        return (
          <div key={section.id} className="flex flex-col gap-0.5">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="h-8 w-full justify-start gap-1 px-2 font-medium"
              onClick={() => toggle(section.id)}
            >
              {isExpanded ? (
                <ChevronDownIcon className="size-4 shrink-0" />
              ) : (
                <ChevronRightIcon className="size-4 shrink-0" />
              )}
              {section.title}
            </Button>
            {isExpanded && (
              <div className="flex flex-col gap-0.5 pl-3">
                {section.children.map(({ slug }) => {
                  const href = `/docs/${slug}`;
                  const active = activeSlug === slug;
                  return (
                    <Link
                      key={slug}
                      href={href}
                      onClick={onNavigate}
                      className={cn(
                        "rounded-md px-3 py-1.5 text-sm transition-colors hover:bg-muted",
                        active && "bg-muted font-medium"
                      )}
                    >
                      {DOC_REGISTRY[slug]?.title ?? slug}
                    </Link>
                  );
                })}
              </div>
            )}
          </div>
        );
      })}
    </nav>
  );
}
