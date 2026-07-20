"use client";

import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { DOC_REGISTRY } from "@/lib/docs/registry";
import { cn } from "@/lib/utils";

export function DocLink({
  slug,
  children,
}: {
  slug: keyof typeof DOC_REGISTRY | string;
  children?: React.ReactNode;
}) {
  const title = DOC_REGISTRY[slug]?.title ?? String(slug);
  return (
    <Link
      href={`/docs/${slug}`}
      className={cn(
        buttonVariants({ variant: "outline", size: "sm" }),
        "max-w-full min-w-0 truncate"
      )}
      title={title}
    >
      {children ?? title}
    </Link>
  );
}
