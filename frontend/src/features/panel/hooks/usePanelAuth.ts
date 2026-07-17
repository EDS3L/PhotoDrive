import { useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { panelLogin, panelLogout, getMe } from '../api/panelAuthApi';
import { usePanelAuthStore } from '../store/panelAuthStore';
import { queryClient } from '@/lib/queryClient';
import type { LoginRequest } from '@/shared/types/api';
import type { AxiosError } from 'axios';

export function usePanelLogin() {
	const setUser = usePanelAuthStore((s) => s.setUser);
	const setLoginPassword = usePanelAuthStore((s) => s.setLoginPassword);

	return useMutation<void, AxiosError, LoginRequest>({
		// formularz logowania pokazuje błąd inline — pomijamy globalny toast
		meta: { skipGlobalError: true },
		mutationFn: async (data) => {
			await panelLogin(data);
			const user = await getMe();
			const hasAccess =
				user.roles.includes('ADMIN') || user.roles.includes('PHOTOGRAPHER');
			if (!hasAccess) {
				await panelLogout();
				throw new Error('ACCESS_DENIED');
			}
			setUser(user);
			// Zasilamy cache ['panel','me'] danymi z logowania — bez tego PanelLayout
			// (usePanelMe, staleTime: Infinity) po nawigacji odpalał DRUGI GET /user/me (F.1).
			queryClient.setQueryData(['panel', 'me'], user);
			// zachowujemy hasło z logowania na wypadek wymuszonej zmiany (pierwsze logowanie)
			setLoginPassword(data.password);
		},
	});
}

export function usePanelLogout() {
	const clear = usePanelAuthStore((s) => s.clear);

	return useMutation({
		mutationFn: panelLogout,
		onSettled: () => {
			clear();
			queryClient.clear();
		},
	});
}

export function usePanelMe() {
	const setUser = usePanelAuthStore((s) => s.setUser);

	const query = useQuery({
		queryKey: ['panel', 'me'],
		queryFn: getMe,
		retry: false,
		staleTime: Infinity,
	});

	useEffect(() => {
		if (query.data) {
			setUser(query.data);
		}
	}, [query.data, setUser]);

	return query;
}
