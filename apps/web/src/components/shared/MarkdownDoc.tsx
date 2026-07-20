"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

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
  code: ({ children }: React.ComponentProps<"code">) => (
    <code className="rounded bg-muted px-1 py-0.5 font-mono text-xs">{children}</code>
  ),
  pre: ({ children }: React.ComponentProps<"pre">) => (
    <pre className="overflow-x-auto rounded-md bg-muted p-4 font-mono text-xs">{children}</pre>
  ),
  a: ({ children, href }: React.ComponentProps<"a">) => (
    <a
      href={href}
      className="text-primary underline underline-offset-4 hover:opacity-80"
      target={href?.startsWith("http") ? "_blank" : undefined}
      rel={href?.startsWith("http") ? "noopener noreferrer" : undefined}
    >
      {children}
    </a>
  ),
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
