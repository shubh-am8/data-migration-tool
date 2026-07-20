import { extractHeadings, flattenMarkdown } from "./search-index";

test("extractHeadings collects only ## headings", () => {
  const md = `# Title\n## Section A\n### Sub\n## Section B\n`;
  expect(extractHeadings(md)).toEqual(["Section A", "Section B"]);
});

test("flattenMarkdown skips mermaid and caps length", () => {
  const md = "## Hello\n\n```mermaid\nflowchart TB\n```\n\nSome **bold** [link](x.md) text.";
  const flat = flattenMarkdown(md);
  expect(flat).not.toMatch(/mermaid|flowchart/);
  expect(flat).toContain("Hello");
  expect(flat).toContain("bold");
  expect(flat).toContain("link");
  expect(flat.length).toBeLessThanOrEqual(8000);
});
