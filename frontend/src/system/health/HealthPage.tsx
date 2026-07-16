import { useQuery } from "@tanstack/react-query";

import { checkBackendHealth } from "./healthApi";

/**
 * System Health page — the frontend surface of the architecture-validation slice.
 *
 * It performs the single health call through the server-state layer (React Query) and displays only
 * whether communication with the backend succeeded. No dashboard, navigation, branding, loading
 * skeletons, or product messaging — this exists to prove the stack is wired end to end.
 */
export function HealthPage() {
  const { isPending, isError, data } = useQuery({
    queryKey: ["system-health"],
    queryFn: checkBackendHealth,
    retry: false,
  });

  let message: string;
  if (isPending) {
    message = "Checking backend…";
  } else if (isError || !data) {
    message = "Backend unreachable";
  } else {
    message = "Backend reachable";
  }

  return <p role="status">{message}</p>;
}
