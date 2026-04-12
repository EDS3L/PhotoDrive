import { useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { panelLogin, panelLogout, getMe } from '../api/panelAuthApi';
import { usePanelAuthStore } from '../store/panelAuthStore';
import { queryClient } from '@/lib/queryClient';
import type { LoginRequest } from '@/shared/types/api';
import type { AxiosError } from 'axios';

export function usePanelLogin() {
	const setUser = usePanelAuthStore((s) => s.setUser);

	return useMutation<void, AxiosError, LoginRequest>({
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
