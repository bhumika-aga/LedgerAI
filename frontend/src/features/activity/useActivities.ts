import { keepPreviousData, useQuery } from "@tanstack/react-query";

import type { Page } from "../../shared";
import type { Activity, TimelineQuery } from "./activityApi";
import { getActivities } from "./activityApi";

/**
 * Server-state hook for the activity timeline (FRONTEND_CODING_STANDARDS §6, ADR-007). Timeline data
 * lives in React Query, never a global store. The timeline is read-only, so there are no mutation hooks.
 * `keepPreviousData` keeps the current page visible while the next page loads, avoiding a flash of the
 * loading state during pagination.
 */

export const activityKeys = {
  all: ["activities"] as const,
  list: (query: TimelineQuery) => ["activities", "list", query] as const,
};

export function useActivities(query: TimelineQuery = {}) {
  return useQuery<Page<Activity>>({
    queryKey: activityKeys.list(query),
    queryFn: () => getActivities(query),
    placeholderData: keepPreviousData,
  });
}
