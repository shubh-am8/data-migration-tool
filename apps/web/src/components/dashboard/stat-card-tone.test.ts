import { statToneClasses } from "./stat-card-tone";

describe("stat card tone classes", () => {
  it("maps success to emerald left border", () => {
    expect(statToneClasses("success")).toBe("border-l-4 border-l-emerald-600");
  });

  it("maps warning to amber left border", () => {
    expect(statToneClasses("warning")).toBe("border-l-4 border-l-amber-500");
  });

  it("maps danger to red left border", () => {
    expect(statToneClasses("danger")).toBe("border-l-4 border-l-red-600");
  });

  it("maps info to sky left border", () => {
    expect(statToneClasses("info")).toBe("border-l-4 border-l-sky-600");
  });

  it("maps default to no border accent", () => {
    expect(statToneClasses("default")).toBe("");
  });
});
