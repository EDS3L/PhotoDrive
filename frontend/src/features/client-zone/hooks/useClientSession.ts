import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getCurrentUser } from '../api/clientZoneApi';
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
			setSession({
				email: query.data.email,
				userId: query.data.id,
				mustChangePassword: query.data.changePasswordOnNextLogin,
			});
		}
	}, [query.data, setSession]);

	return { isChecking: query.isLoading };
}
