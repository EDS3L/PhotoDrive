import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useClientSession } from './useClientSession';
import { useAuthStore } from '@/app/store/authStore';
import type { CurrentUser, getCurrentUser } from '../api/clientZoneApi';

const { getCurrentUserMock } = vi.hoisted(() => ({
	getCurrentUserMock: vi.fn<typeof getCurrentUser>(),
}));

vi.mock('../api/clientZoneApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('../api/clientZoneApi')>();
	return { ...actual, getCurrentUser: getCurrentUserMock };
});

function wrapper({ children }: { children: ReactNode }) {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false } },
	});
	return (
		<QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
	);
}

/** Podmienia `window.location.href` na obserwowalne pole (jsdom blokuje realną nawigację). */
function stubLocation(): { get: () => string } {
	const location = { href: '' } as Location;
	Object.defineProperty(window, 'location', {
		value: location,
		writable: true,
		configurable: true,
	});
	return { get: () => location.href };
}

describe('useClientSession', () => {
	beforeEach(() => {
		getCurrentUserMock.mockReset();
		// Store jest modułowym singletonem — bez resetu sesja przecieka między testami.
		useAuthStore.getState().clear();
	});

	it('A valid cookie restores the client session, so refreshing the page does not throw the client back to the login form', async () => {
		// Given - the browser still holds pd_at and /user/me answers for a CLIENT
		getCurrentUserMock.mockResolvedValue({
			id: 'klient-1',
			name: 'Klient',
			email: 'klient@photodrive.pl',
			roles: ['CLIENT'],
			changePasswordOnNextLogin: false,
		} satisfies CurrentUser);

		// When
		renderHook(() => useClientSession(), { wrapper });

		// Then
		await waitFor(() =>
			expect(useAuthStore.getState().isAuthenticated).toBe(true),
		);
		expect(useAuthStore.getState().email).toBe('klient@photodrive.pl');
		expect(useAuthStore.getState().userId).toBe('klient-1');
	});

	it('An admin landing in the client zone is sent to the panel and gets no client session, because the client zone is for the CLIENT role only', async () => {
		// Given - an ADMIN cookie, e.g. from a pasted client-zone URL (B.8/A7)
		const location = stubLocation();
		getCurrentUserMock.mockResolvedValue({
			id: 'admin-1',
			name: 'Admin',
			email: 'admin@photodrive.pl',
			roles: ['ADMIN'],
			changePasswordOnNextLogin: false,
		} satisfies CurrentUser);

		// When
		renderHook(() => useClientSession(), { wrapper });

		// Then - redirected AND deliberately left without a client session; setting one would
		// show the admin a client zone that answers 403 on every album call
		await waitFor(() => expect(location.get()).toBe('/admin'));
		expect(useAuthStore.getState().isAuthenticated).toBe(false);
	});

	it('A photographer is routed to their own panel rather than the admin one', async () => {
		// Given
		const location = stubLocation();
		getCurrentUserMock.mockResolvedValue({
			id: 'foto-1',
			name: 'Fotograf',
			email: 'foto@photodrive.pl',
			roles: ['PHOTOGRAPHER'],
			changePasswordOnNextLogin: false,
		} satisfies CurrentUser);

		// When
		renderHook(() => useClientSession(), { wrapper });

		// Then
		await waitFor(() => expect(location.get()).toBe('/photographer'));
	});

	it('A rejected probe leaves the visitor logged out instead of surfacing an error, which is what keeps the login form on screen', async () => {
		// Given - no valid cookie; the probe runs in silent mode so 401 must not redirect
		getCurrentUserMock.mockRejectedValue(new Error('401'));

		// When
		const { result } = renderHook(() => useClientSession(), { wrapper });

		// Then
		await waitFor(() => expect(result.current.isChecking).toBe(false));
		expect(useAuthStore.getState().isAuthenticated).toBe(false);
	});

	it('An already authenticated visitor is not probed again, so navigating inside the zone costs no extra request', async () => {
		// Given - the session is already in the store (just logged in)
		useAuthStore.getState().setSession({
			email: 'klient@photodrive.pl',
			userId: 'klient-1',
			mustChangePassword: false,
		});

		// When
		renderHook(() => useClientSession(), { wrapper });

		// Then - the query is disabled while authenticated
		expect(getCurrentUserMock).not.toHaveBeenCalled();
	});
});
