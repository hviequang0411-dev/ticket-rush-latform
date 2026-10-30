import { useEffect, useState } from 'react';
import { get, post } from './http';

// Sign-in is the gateway's: it runs the OIDC code flow with Keycloak and keeps the tokens in its session.
// The browser only ever holds a session cookie; API calls go through the gateway, which relays the token.

export interface Account {
  login: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  authorities: string[];
}

const RETURN_TO = 'ticketrush.returnTo';

let cached: Promise<Account | null> | null = null;

/** The signed-in user, or null. Shared by every caller until sign-in state changes. */
export function fetchAccount(): Promise<Account | null> {
  cached ??= get<Account>('/api/account').catch(() => null);
  return cached;
}

export function displayName(account: Account): string {
  const name = [account.lastName, account.firstName].filter(Boolean).join(' ');
  return name || account.login;
}

export function isAdmin(account: Account | null): boolean {
  return !!account?.authorities.includes('ROLE_ADMIN');
}

/** Leaves for Keycloak; {@link consumeReturnTo} brings the user back to `returnTo` afterwards. */
export function signIn(returnTo: string = window.location.pathname + window.location.search): void {
  sessionStorage.setItem(RETURN_TO, returnTo);
  window.location.href = '/oauth2/authorization/oidc';
}

export async function signOut(): Promise<void> {
  const { logoutUrl } = await post<{ logoutUrl: string }>('/api/logout');
  cached = null;
  window.location.href = logoutUrl;
}

/** Where the user was before signing in, once. */
export function consumeReturnTo(): string | null {
  const path = sessionStorage.getItem(RETURN_TO);
  sessionStorage.removeItem(RETURN_TO);
  return path;
}

export function useAccount(): { account: Account | null; loading: boolean } {
  const [state, setState] = useState<{ account: Account | null; loading: boolean }>({ account: null, loading: true });
  useEffect(() => {
    let active = true;
    fetchAccount().then(account => active && setState({ account, loading: false }));
    return () => {
      active = false;
    };
  }, []);
  return state;
}
