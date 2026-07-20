import { shellAsideClass, shellMainClass, shellRootClass } from "./shell-layout";

describe("shell layout classes", () => {
  it("root fills viewport on md+ with overflow hidden", () => {
    expect(shellRootClass).toContain("md:h-screen");
    expect(shellRootClass).toContain("md:overflow-hidden");
  });

  it("main scrolls independently", () => {
    expect(shellMainClass).toContain("overflow-y-auto");
    expect(shellMainClass).toContain("min-h-0");
  });

  it("aside is full height with min-h-0 for flex shrink", () => {
    expect(shellAsideClass).toContain("h-full");
    expect(shellAsideClass).toContain("min-h-0");
  });
});
