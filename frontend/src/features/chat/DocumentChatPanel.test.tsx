import { fireEvent, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { Page } from "../../shared";
import { renderWithProviders } from "../../test/renderWithProviders";
import type { ChatExchange } from "./chatApi";
import * as chatApi from "./chatApi";
import { DocumentChatPanel } from "./DocumentChatPanel";

vi.mock("./chatApi");

const documentId = "33333333-3333-3333-3333-333333333333";

function page(exchanges: ChatExchange[]): Page<ChatExchange> {
  return {
    content: exchanges,
    page: 0,
    size: 20,
    totalElements: exchanges.length,
    totalPages: exchanges.length === 0 ? 0 : 1,
    hasNext: false,
  };
}

function exchange(overrides: Partial<ChatExchange> = {}): ChatExchange {
  return {
    id: "44444444-4444-4444-4444-444444444444",
    type: "CHAT",
    status: "COMPLETED",
    documentId,
    prompt: "What is the total?",
    content: "The total is 987654.",
    edited: false,
    failureReason: null,
    createdAt: "2026-07-23T00:00:00Z",
    updatedAt: "2026-07-23T00:00:00Z",
    ...overrides,
  };
}

function renderPanel(status: "READY" | "PROCESSING" = "READY") {
  return renderWithProviders(
    <DocumentChatPanel documentId={documentId} documentStatus={status} />,
  );
}

describe("DocumentChatPanel", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("gates until the document is ready and does not fetch", () => {
    renderPanel("PROCESSING");

    expect(
      screen.getByText(/ask questions once the document is ready/i),
    ).toBeInTheDocument();
    expect(chatApi.getChatHistory).not.toHaveBeenCalled();
  });

  it("shows the empty state when there are no questions yet", async () => {
    vi.mocked(chatApi.getChatHistory).mockResolvedValue(page([]));

    renderPanel();

    expect(await screen.findByText(/no questions yet/i)).toBeInTheDocument();
  });

  it("renders the question/answer thread", async () => {
    vi.mocked(chatApi.getChatHistory).mockResolvedValue(page([exchange()]));

    renderPanel();

    expect(await screen.findByText("What is the total?")).toBeInTheDocument();
    expect(screen.getByText("The total is 987654.")).toBeInTheDocument();
  });

  it("shows a failed exchange's reason honestly", async () => {
    vi.mocked(chatApi.getChatHistory).mockResolvedValue(
      page([
        exchange({
          status: "FAILED",
          content: null,
          failureReason: "The AI service was unavailable.",
        }),
      ]),
    );

    renderPanel();

    expect(
      await screen.findByText("The AI service was unavailable."),
    ).toBeInTheDocument();
  });

  it("asks a question and clears the input", async () => {
    vi.mocked(chatApi.getChatHistory).mockResolvedValue(page([]));
    vi.mocked(chatApi.askQuestion).mockResolvedValue(exchange());

    renderPanel();

    const input = await screen.findByLabelText("Your question");
    fireEvent.change(input, { target: { value: "What is the total?" } });
    fireEvent.click(screen.getByRole("button", { name: "Ask" }));

    await waitFor(() =>
      expect(chatApi.askQuestion).toHaveBeenCalledWith(
        documentId,
        "What is the total?",
      ),
    );
  });

  it("does not submit a blank question", async () => {
    vi.mocked(chatApi.getChatHistory).mockResolvedValue(page([]));

    renderPanel();

    await screen.findByLabelText("Your question");
    // The button is disabled while the input is empty, so a click cannot fire the request.
    expect(screen.getByRole("button", { name: "Ask" })).toBeDisabled();
    expect(chatApi.askQuestion).not.toHaveBeenCalled();
  });

  it("shows an error when loading the thread fails", async () => {
    vi.mocked(chatApi.getChatHistory).mockRejectedValue(new Error("Network"));

    renderPanel();

    expect(
      await screen.findByText(/conversation could not be loaded/i),
    ).toBeInTheDocument();
  });

  it("shows an error when asking fails", async () => {
    vi.mocked(chatApi.getChatHistory).mockResolvedValue(page([]));
    vi.mocked(chatApi.askQuestion).mockRejectedValue(new Error("503"));

    renderPanel();

    const input = await screen.findByLabelText("Your question");
    fireEvent.change(input, { target: { value: "Anything?" } });
    fireEvent.click(screen.getByRole("button", { name: "Ask" }));

    expect(
      await screen.findByText(/question could not be answered/i),
    ).toBeInTheDocument();
  });
});
