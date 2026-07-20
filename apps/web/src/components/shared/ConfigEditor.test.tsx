import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ConfigEditor } from "./ConfigEditor";

const baseProps = {
  onChange: jest.fn(),
  onSave: jest.fn(),
  onReveal: jest.fn(),
  onHide: jest.fn(),
};

describe("ConfigEditor", () => {
  it("renders masked sensitive value and reveals on Show", async () => {
    const onReveal = jest.fn();
    const config = {
      gspace_webhook_url: {
        value: "********",
        source: "ENV",
        sensitive: true,
        masked: true,
        restartRequired: false,
      },
    };

    render(<ConfigEditor config={config} {...baseProps} onReveal={onReveal} />);

    expect(screen.getByDisplayValue("********")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /show/i }));
    expect(onReveal).toHaveBeenCalledWith("gspace_webhook_url");
  });

  it("calls onHide when Hide is clicked", async () => {
    const onHide = jest.fn();
    const config = {
      google_client_secret: {
        value: "secret-value",
        source: "ENV",
        sensitive: true,
        masked: false,
        restartRequired: false,
      },
    };

    render(<ConfigEditor config={config} {...baseProps} onHide={onHide} />);
    await userEvent.click(screen.getByRole("button", { name: /hide/i }));
    expect(onHide).toHaveBeenCalledWith("google_client_secret");
  });

  it("hides IP whitelist editor when mode is OPEN", () => {
    render(
      <ConfigEditor
        config={{
          ip_whitelist_mode: {
            value: "OPEN",
            source: "DEFAULT",
            sensitive: false,
            masked: false,
            restartRequired: false,
          },
          ip_whitelist: {
            value: "[]",
            source: "DEFAULT",
            sensitive: false,
            masked: false,
            restartRequired: false,
          },
        }}
        {...baseProps}
      />
    );
    expect(screen.queryByText("Allowed IPs")).not.toBeInTheDocument();
    expect(screen.queryByText("Add IP")).not.toBeInTheDocument();
  });

  it("shows labeled IP editor when mode is RESTRICTED", () => {
    render(
      <ConfigEditor
        config={{
          ip_whitelist_mode: {
            value: "RESTRICTED",
            source: "DASHBOARD",
            sensitive: false,
            masked: false,
            restartRequired: false,
          },
          ip_whitelist: {
            value: JSON.stringify([{ label: "VPN", ip: "203.0.113.9" }]),
            source: "DASHBOARD",
            sensitive: false,
            masked: false,
            restartRequired: false,
          },
        }}
        {...baseProps}
      />
    );
    expect(screen.getByText("Allowed IPs")).toBeInTheDocument();
    expect(screen.getByDisplayValue("VPN")).toBeInTheDocument();
    expect(screen.getByDisplayValue("203.0.113.9")).toBeInTheDocument();
  });

  it("shows human labels for google and domain keys", () => {
    render(
      <ConfigEditor
        config={{
          google_client_id: {
            value: "x",
            source: "ENV",
            sensitive: true,
            masked: true,
            restartRequired: false,
          },
          allowed_email_domain: {
            value: "chatbot.team",
            source: "ENV",
            sensitive: false,
            masked: false,
            restartRequired: false,
          },
        }}
        {...baseProps}
      />
    );
    expect(screen.getByText("Google Client ID")).toBeInTheDocument();
    expect(screen.getByText("Allowed email domain")).toBeInTheDocument();
    expect(screen.queryByText("google_client_id")).not.toBeInTheDocument();
  });
});
