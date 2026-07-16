import type { ReactNode } from "react";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";

import type { LoginCredentials, RegisterInput, User } from "./authApi";
import * as authApi from "./authApi";
import { clearAccessToken, setAccessToken } from "./tokenStore";

// Install the Bearer-attaching request interceptor once, at module load, before any request runs.
authApi.installAuthTokenInterceptor();

/**
 * Session state (SECURITY §7, ADR-018). {@code loading} covers the initial bootstrap while the app
 * tries to restore a session from the refresh cookie; it resolves to {@code authenticated} or
 * {@code unauthenticated}.
 */
export type AuthStatus = "loading" | "authenticated" | "unauthenticated";

export interface AuthContextValue {
  status: AuthStatus;
  user: User | null;
  login: (credentials: LoginCredentials) => Promise<void>;
  register: (input: RegisterInput) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

/**
 * Owns the client session: bootstrap on load, and login/register/logout transitions. On mount it
 * attempts a silent refresh (using the httpOnly cookie) followed by {@code /auth/me}; success
 * restores the session, failure leaves the user unauthenticated. Access tokens live only in memory
 * (tokenStore); the refresh token is never touched by JS.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    let active = true;
    async function bootstrap() {
      try {
        const tokens = await authApi.refresh();
        setAccessToken(tokens.accessToken);
        const currentUser = await authApi.getCurrentUser();
        if (!active) {
          return;
        }
        setUser(currentUser);
        setStatus("authenticated");
      } catch {
        if (!active) {
          return;
        }
        clearAccessToken();
        setUser(null);
        setStatus("unauthenticated");
      }
    }
    void bootstrap();
    return () => {
      active = false;
    };
  }, []);

  const login = useCallback(async (credentials: LoginCredentials) => {
    const result = await authApi.login(credentials);
    setAccessToken(result.tokens.accessToken);
    setUser(result.user);
    setStatus("authenticated");
  }, []);

  const register = useCallback(async (input: RegisterInput) => {
    const result = await authApi.register(input);
    setAccessToken(result.tokens.accessToken);
    setUser(result.user);
    setStatus("authenticated");
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      clearAccessToken();
      setUser(null);
      setStatus("unauthenticated");
    }
  }, []);

  return (
    <AuthContext.Provider value={{ status, user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
