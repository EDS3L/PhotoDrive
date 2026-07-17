import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { queryClient } from '@/lib/queryClient';
import { usePanelLogin, usePanelMe } from './usePanelAuth';
import { usePanelAuthStore } from '../store/panelAuthStore';
import type { CurrentUserInfo } from '../types/panel';

const { panelLoginMock, getMeMock, panelLogoutMock } = vi.hoisted(() => ({
	panelLoginMock: vi.fn(),
	getMeMock: vi.fn(),
	panelLogoutMock: vi.fn(),
}));

vi.mock('../api/panelAuthApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('../api/panelAuthApi')>();
	return {
		...actual,
		panelLogin: panelLoginMock,
		getMe: getMeMock,
		panelLogout: panelLogoutMock,
	};
});

const admin: CurrentUserInfo = {
	id: 'u-1',
	name: 'Ala',
	email: 'ala@photodrive.dev',
	roles: ['ADMIN'],
	changePasswordOnNextLogin: false,
};

// usePanelLogin zasila SINGLETON queryClient, a usePanelMe czyta z tego samego klienta
// przez provider — wrapper musi używać dokładnie tego singletona, nie świeżej instancji.
function wrapper({ children }: { children: ReactNode }) {
	return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

describe('usePanelLogin', () => {
	beforeEach(() => {
		panelLoginMock.mockReset();
		getMeMock.mockReset();
		panelLogoutMock.mockReset();
		queryClient.clear();
		usePanelAuthStore.getState().clear();
	});

	it('seeds the panel session cache on login, so the panel screen never fires a second GET /user/me (F.1)', async () => {
		// Given - login succeeds and /user/me returns an admin
		panelLoginMock.mockResolvedValue(undefined);
		getMeMock.mockResolvedValue(admin);

		// When - the user logs in on the login screen (no session query mounted there yet)
		const login = renderHook(() => usePanelLogin(), { wrapper });
		await act(async () => {
			await login.result.current.mutateAsync({
				email: 'ala@photodrive.dev',
				password: 'secret',
			});
		});

		// Then - the session cache is already populated and /user/me was hit exactly once
		expect(queryClient.getQueryData(['panel', 'me'])).toEqual(admin);
		expect(getMeMock).toHaveBeenCalledTimes(1);

		// When - the panel screen mounts its session query after navigation
		const me = renderHook(() => usePanelMe(), { wrapper });

		// Then - it serves the seeded session without a second network round-trip
		await waitFor(() => expect(me.result.current.data).toEqual(admin));
		expect(getMeMock).toHaveBeenCalledTimes(1);
	});
});
