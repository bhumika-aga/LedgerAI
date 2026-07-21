import type { Page } from "../../shared";
import { apiClient } from "../../shared";

/**
 * Typed client for the Activity module (API_SPEC §15). Reuses the shared `apiClient` (Bearer + cookies)
 * and shared `Page`, describing only the one documented, read-only operation
 * (FRONTEND_CODING_STANDARDS §8). The timeline is append-only: there are deliberately no create/update/
 * delete calls (FR-TMLN-004, BR-016).
 */

/** The documented action types the implemented modules emit (DATABASE §5.8). */
export type ActivityType =
  | "CLIENT_CREATED"
  | "DOCUMENT_UPLOADED"
  | "DOCUMENT_DELETED"
  | "SUMMARY_GENERATED";

/** ActivityResponse (API_SPEC §17.8) — read-only; never carries the owning `userId`. */
export interface Activity {
  id: string;
  actionType: ActivityType;
  summary: string | null;
  clientId: string | null;
  documentId: string | null;
  metadata: Record<string, unknown> | null;
  createdAt: string;
}

export interface TimelineQuery {
  clientId?: string;
  page?: number;
  size?: number;
}

/** API_SPEC §15.1: the chronological timeline (newest first by default), optionally per-client. */
export async function getActivities(
  query: TimelineQuery = {},
): Promise<Page<Activity>> {
  const { data } = await apiClient.get<Page<Activity>>("/activities", {
    params: query,
  });
  return data;
}
