import { NAV_SECTIONS, NAV_ITEMS, visibleNavSections } from "./nav-sections";

describe("nav-sections", () => {
  it("puts Docs alone under Resources", () => {
    const resources = NAV_SECTIONS.find((s) => s.id === "resources");
    expect(resources?.label).toBe("Resources");
    expect(resources?.items.map((i) => i.href)).toEqual(["/docs"]);
  });

  it("flattens NAV_ITEMS including /docs", () => {
    expect(NAV_ITEMS.some((i) => i.href === "/docs")).toBe(true);
  });

  it("hides Users when not admin", () => {
    const sections = visibleNavSections(false);
    const hrefs = sections.flatMap((s) => s.items.map((i) => i.href));
    expect(hrefs).not.toContain("/users");
  });

  it("shows Users when admin", () => {
    const hrefs = visibleNavSections(true).flatMap((s) => s.items.map((i) => i.href));
    expect(hrefs).toContain("/users");
  });
});
