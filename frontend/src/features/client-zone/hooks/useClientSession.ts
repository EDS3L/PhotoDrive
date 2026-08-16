import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getCurrentUser } from '../api/clientZoneApi';
import { redirectNonClientToPanel } from '../lib/rolePanelRedirect';
import { useAuthStore } from '@/app/store/authStore';

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
