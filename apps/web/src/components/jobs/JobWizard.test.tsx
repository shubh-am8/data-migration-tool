import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { JobWizard } from "./JobWizard";

describe("JobWizard run mode", () => {
  beforeEach(() => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    }) as unknown as typeof fetch;
  });

  it("defaults to Test with the simulation checkbox visible and no production alert", async () => {
    render(<JobWizard onComplete={jest.fn()} />);
    await waitFor(() => expect(global.fetch).toHaveBeenCalled());

    expect(screen.getByText("Seed sample data job")).toBeInTheDocument();
    expect(screen.queryByText(/will run against production data/i)).not.toBeInTheDocument();
  });

  it("switching to Production hides the simulation checkbox and shows a confirmation alert", async () => {
    render(<JobWizard onComplete={jest.fn()} />);
    await waitFor(() => expect(global.fetch).toHaveBeenCalled());

    await userEvent.click(screen.getByRole("button", { name: "Production" }));

    expect(screen.getByText(/will run against production data/i)).toBeInTheDocument();
    expect(screen.queryByText("Seed sample data job")).not.toBeInTheDocument();
  });
});
