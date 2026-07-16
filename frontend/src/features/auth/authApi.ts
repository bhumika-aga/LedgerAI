import { apiClient } from "../../shared";
import { getAccessToken } from "./tokenStore";

/**
 * Typed client for the Authentication module (API_SPEC §5). These functions are the only gateway the
 * auth feature uses to reach the backend; the rest of the slice calls them rather than issuing raw
 * HTTP (FRONTEND_CODING_STANDARDS §8). The refresh token is never seen here — it travels only as the
 * httpOnly cookie (ADR-018); the access token is attached as a Bearer header by the interceptor below.
 */

/** Outbound user representation (API_SPEC §17.1). */
export interface User {
  id: string;
  email: string;
  fullName: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Access-token envelope (API_SPEC §17.2). The refresh token is intentionally absent (ADR-018). */
export interface AuthTokens {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

/** Register/login response (API_SPEC §5.1–5.2). */
export interface AuthResult {
  user: User;
  tokens: AuthTokens;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterInput {
  email: string;
  password: string;
  fullName?: string;
}

let interceptorInstalled = false;

/**
 * Attaches the in-memory access token as {@code Authorization: Bearer …} to every outgoing request.
 * Public auth calls (register/login/refresh) simply carry no token when none is set; the backend
 * treats them as unauthenticated by design. Installed once and idempotent.
 */
export function installAuthTokenInterceptor(): void {
  if (interceptorInstalled) {
    return;
  }
  interceptorInstalled = true;
  apiClient.interceptors.request.use((config) => {
    const token = getAccessToken();
    if (token) {
      config.headers.set("Authorization", `Bearer ${token}`);
    }
    return config;
  });
}

export async function register(input: RegisterInput): Promise<AuthResult> {
  const { data } = await apiClient.post<AuthResult>("/auth/register", input);
  return data;
}

export async function login(
  credentials: LoginCredentials,
): Promise<AuthResult> {
  const { data } = await apiClient.post<AuthResult>("/auth/login", credentials);
  return data;
}

export async function refresh(): Promise<AuthTokens> {
  const { data } = await apiClient.post<{ tokens: AuthTokens }>(
    "/auth/refresh",
  );
  return data.tokens;
}

export async function logout(): Promise<void> {
  await apiClient.post("/auth/logout");
}

export async function getCurrentUser(): Promise<User> {
  const { data } = await apiClient.get<User>("/auth/me");
  return data;
}
