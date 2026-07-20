import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { EmailConfirmDialog } from "./EmailConfirmDialog";

const baseProps = {
  open: true,
  onOpenChange: jest.fn(),
  title: "Delete user",
  description: "This cannot be undone.",
  confirmLabel: "Delete",
  confirmVariant: "danger" as const,
  subject: { name: "Ada Lovelace", email: "a@b.com" },
  onConfirm: jest.fn(),
};

describe("EmailConfirmDialog", () => {
  it("keeps confirm disabled until typed email matches", async () => {
    render(<EmailConfirmDialog {...baseProps} />);

    const confirm = screen.getByRole("button", { name: "Delete" });
    expect(confirm).toBeDisabled();

    await userEvent.type(screen.getByLabelText("Type email to confirm"), "a@b.com");
    expect(confirm).toBeEnabled();
  });
});
