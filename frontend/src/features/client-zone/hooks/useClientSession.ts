import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getCurrentUser } from '../api/clientZoneApi';
import { redirectNonClientToPanel } from '../lib/rolePanelRedirect';
import { useAuthStore } from '@/app/store/authStore';

/**
 * Re-hydratuje stan logowania klienta na podstawie cookie `pd_at`.
 * Bez tego odświeżenie strony (F5) w strefie klienta pokazywało formularz
 * logowania mimo ważnej sesji. Probe `/user/me` idzie w trybie cichym
 * (bez redirectu przy 401), więc niezalogowany po prostu widzi login.
 */
export function useClientSession() {
	const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
	const setSession = useAuthStore((s) => s.setSession);

	const query = useQuery({
		queryKey: ['client', 'me'],
		queryFn: getCurrentUser,
		enabled: !isAuthenticated,
		retry: false,
		staleTime: Infinity,
	});

	useEffect(() => {
		if (query.data) {
			// F5 w strefie klienta na koncie admina/fotografa (np. wklejony URL) → do panelu (B.8).
			if (redirectNonClientToPanel(query.data.roles)) return;
			setSession({
				email: query.data.email,
				userId: query.data.id,
				mustChangePassword: query.data.changePasswordOnNextLogin,
			});
		}
	}, [query.data, setSession]);

	return { isChecking: query.isLoading };
}
