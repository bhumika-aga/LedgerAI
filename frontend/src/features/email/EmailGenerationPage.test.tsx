import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "../../test/renderWithProviders";
import type { EmailDraft } from "./emailApi";
import * as emailApi from "./emailApi";
import { EmailGenerationPage } from "./EmailGenerationPage";

vi.mock("./emailApi");

function draft(overrides: Partial<EmailDraft> = {}): EmailDraft {
  return {
    id: "44444444-4444-4444-4444-444444444444",
    type: "EMAIL",
    status: "COMPLETED",
    documentId: null,
    prompt: "Write a follow-up.",
    content: "Dear client, this is a follow-up.",
    edited: false,
    failureReason: null,
    createdAt: "2026-07-24T00:00:00Z",
    updatedAt: "2026-07-24T00:00:00Z",
    ...overrides,
  };
}

function renderPage(initialEntry = "/emails/new") {
  return renderWithProviders(<EmailGenerationPage />, {
    path: "/emails/new",
    initialEntries: [initialEntry],
  });
}

describe("EmailGenerationPage", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("shows the empty state before generating", () => {
    renderPage();

    expect(
      screen.getByText(/enter an instruction and generate a draft/i),
    ).toBeInTheDocument();
    // The never-sent guarantee is shown up front (BR-034).
    expect(screen.getByText(/never sends email/i)).toBeInTheDocument();
  });

  it("generates a draft from the instruction and shows it editable", async () => {
    vi.mocked(emailApi.generateEmail).mockResolvedValue(draft());

    renderPage();

    fireEvent.change(screen.getByLabelText("Instruction"), {
      target: { value: "Write a follow-up." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Generate draft" }));

    await waitFor(() =>
      expect(emailApi.generateEmail).toHaveBeenCalledWith({
        instruction: "Write a follow-up.",
        documentId: undefined,
        clientId: undefined,
      }),
    );
    expect(
      await screen.findByDisplayValue("Dear client, this is a follow-up."),
    ).toBeInTheDocument();
  });

  it("passes document context from the query string", async () => {
    vi.mocked(emailApi.generateEmail).mockResolvedValue(
      draft({ documentId: "d-1" }),
    );

    renderPage("/emails/new?documentId=d-1");

    expect(
      screen.getByText(/document will be used as context/i),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Instruction"), {
      target: { value: "Chase it." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Generate draft" }));

    await waitFor(() =>
      expect(emailApi.generateEmail).toHaveBeenCalledWith({
        instruction: "Chase it.",
        documentId: "d-1",
        clientId: undefined,
      }),
    );
  });

  it("does not submit a blank instruction", () => {
    renderPage();

    expect(
      screen.getByRole("button", { name: "Generate draft" }),
    ).toBeDisabled();
    expect(emailApi.generateEmail).not.toHaveBeenCalled();
  });

  it("shows an error state when generation fails", async () => {
    vi.mocked(emailApi.generateEmail).mockRejectedValue(new Error("503"));

    renderPage();

    fireEvent.change(screen.getByLabelText("Instruction"), {
      target: { value: "Draft it." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Generate draft" }));

    expect(
      await screen.findByText(/draft could not be generated/i),
    ).toBeInTheDocument();
  });

  it("surfaces a failed draft's reason", async () => {
    vi.mocked(emailApi.generateEmail).mockResolvedValue(
      draft({
        status: "FAILED",
        content: null,
        failureReason: "The AI service did not return a usable draft.",
      }),
    );

    renderPage();

    fireEvent.change(screen.getByLabelText("Instruction"), {
      target: { value: "Draft it." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Generate draft" }));

    expect(
      await screen.findByText("The AI service did not return a usable draft."),
    ).toBeInTheDocument();
  });
});
