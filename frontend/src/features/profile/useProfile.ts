import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { User } from "../auth";
import type { UpdateProfileInput } from "./profileApi";
import { getProfile, updateProfile } from "./profileApi";

/**
 * Server-state hooks for the profile (FRONTEND_CODING_STANDARDS §6, ADR-007). The profile lives in
 * React Query — the single home for server state — rather than in any global store.
 */

export const profileQueryKey = ["profile"] as const;

/** FR-PROF-001: read the caller's own profile. */
export function useProfile() {
  return useQuery<User>({
    queryKey: profileQueryKey,
    queryFn: getProfile,
  });
}

/**
 * FR-PROF-002: update editable fields. On success the response is written straight into the cache —
 * the PATCH already returns the updated `UserResponse` (API_SPEC §6.2), so re-fetching would be a
 * wasted round trip.
 */
export function useUpdateProfile() {
  const queryClient = useQueryClient();
  return useMutation<User, unknown, UpdateProfileInput>({
    mutationFn: updateProfile,
    onSuccess: (updated) => {
      queryClient.setQueryData(profileQueryKey, updated);
    },
  });
}
