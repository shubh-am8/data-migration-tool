"use client";

import { useEffect, useId, useState } from "react";

let mermaidInit: Promise<typeof import("mermaid").default> | undefined;

function getMermaid() {
  if (!mermaidInit) {
    mermaidInit = import("mermaid").then(({ default: mermaid }) => {
      mermaid.initialize({ startOnLoad: false, securityLevel: "strict", theme: "neutral" });
      return mermaid;
    });
  }
  return mermaidInit;
}

export function MermaidBlock({ chart }: { chart: string }) {
  const id = useId().replace(/:/g, "");
  const [svg, setSvg] = useState<string>("");
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const mermaid = await getMermaid();
      const { svg } = await mermaid.render(`mmd-${id}`, chart);
      if (!cancelled) setSvg(svg);
    })().catch(() => {
      if (!cancelled) setSvg("");
    });
    return () => {
      cancelled = true;
    };
  }, [chart, id]);
  if (!svg) {
    return (
      <pre className="overflow-x-auto rounded-md bg-muted p-4 font-mono text-xs">{chart}</pre>
    );
  }
  // ponytail: dangerouslySetInnerHTML is only for mermaid-generated SVG under securityLevel "strict"; never user markdown HTML.
  return (
    <div
      className="overflow-x-auto rounded-md border bg-card p-4"
      dangerouslySetInnerHTML={{ __html: svg }}
    />
  );
}
