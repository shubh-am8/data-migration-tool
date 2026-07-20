import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ConfigEditor } from "./ConfigEditor";

describe("ConfigEditor", () => {
  it("renders masked sensitive value and reveals on click", async () => {
    const mockRevealHandler = jest.fn();
    const config = {
      gspace_webhook_url: { value: "********", source: "ENV", sensitive: true, masked: true, restartRequired: false },
    };

    render(<ConfigEditor config={config} onChange={jest.fn()} onSave={jest.fn()} onReveal={mockRevealHandler} />);

    expect(screen.getByDisplayValue("********")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /show/i }));
    expect(mockRevealHandler).toHaveBeenCalledWith("gspace_webhook_url");
  });

  it("shows human labels for google and domain keys", () => {
    render(<ConfigEditor config={{
      google_client_id: { value: "x", source: "ENV", sensitive: false, masked: false, restartRequired: false },
      allowed_email_domain: { value: "chatbot.team", source: "ENV", sensitive: false, masked: false, restartRequired: false },
    }} onChange={jest.fn()} onSave={jest.fn()} onReveal={jest.fn()} />);
    expect(screen.getByText("Google Client ID")).toBeInTheDocument();
    expect(screen.getByText("Allowed email domain")).toBeInTheDocument();
    expect(screen.queryByText("google_client_id")).not.toBeInTheDocument();
  });
});