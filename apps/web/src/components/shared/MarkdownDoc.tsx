"use client";

import Link from "next/link";
import React from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { MermaidBlock } from "@/components/shared/MermaidBlock";
import { hrefToDocSlug } from "@/lib/docs/registry";

function isMermaidCode(child: React.ReactNode): boolean {
  return (
    React.isValidElement<{ className?: string }>(child) &&
    typeof child.props.className === "string" &&
    child.props.className.includes("language-mermaid")
  );
}

const markdownComponents = {
  h1: ({ children }: React.ComponentProps<"h1">) => (
    <h1 className="text-2xl font-semibold tracking-tight">{children}</h1>
  ),
  h2: ({ children }: React.ComponentProps<"h2">) => (
    <h2 className="mt-6 text-xl font-semibold tracking-tight">{children}</h2>
  ),
  h3: ({ children }: React.ComponentProps<"h3">) => (
    <h3 className="mt-4 text-lg font-medium">{children}</h3>
  ),
  p: ({ children }: React.ComponentProps<"p">) => (
    <p className="text-sm leading-relaxed text-foreground">{children}</p>
  ),
  code: ({ className, children }: React.ComponentProps<"code">) => {
    if (className?.includes("language-mermaid")) {
      return <MermaidBlock chart={String(children).replace(/\n$/, "")} />;
    }
    return (
      <code className="rounded bg-muted px-1 py-0.5 font-mono text-xs">{children}</code>
    );
  },
  pre: ({ children }: React.ComponentProps<"pre">) => {
    if (isMermaidCode(children)) return <>{children}</>;
    return (
      <pre className="overflow-x-auto rounded-md bg-muted p-4 font-mono text-xs">{children}</pre>
    );
  },
  a: ({ children, href }: React.ComponentProps<"a">) => {
    const resolved = hrefToDocSlug(href);
    const className = "text-primary underline underline-offset-4 hover:opacity-80";
    if (resolved?.startsWith("/docs")) {
      return (
        <Link href={resolved} className={className}>
          {children}
        </Link>
      );
    }
    const external = resolved?.startsWith("http");
    return (
      <a
        href={resolved}
        className={className}
        target={external ? "_blank" : undefined}
        rel={external ? "noopener noreferrer" : undefined}
      >
        {children}
      </a>
    );
  },
  ul: ({ children }: React.ComponentProps<"ul">) => (
    <ul className="flex list-disc flex-col gap-1 pl-5 text-sm">{children}</ul>
  ),
  ol: ({ children }: React.ComponentProps<"ol">) => (
    <ol className="flex list-decimal flex-col gap-1 pl-5 text-sm">{children}</ol>
  ),
};

export function MarkdownDoc({ markdown }: { markdown: string }) {
  return (
    <article className="flex max-w-none flex-col gap-4 text-sm">
      <ReactMarkdown remarkPlugins={[remarkGfm]} skipHtml components={markdownComponents}>
        {markdown}
      </ReactMarkdown>
    </article>
  );
}
