import { useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import axios from 'axios';
import { useAuthStore } from '@/lib/stores/auth-store';
import type {
	LoginRequest,
	LoginResponse,
	RemindPasswordRequest,
	UserDto,
} from '@/types/user';
import { normalizeUser } from '@/types/user';

export function useLogin() {
	const setUser = useAuthStore((s) => s.setUser);
	const qc = useQueryClient();

	return useMutation({
		mutationFn: async (data: LoginRequest) => {
			const res = await api.post<LoginResponse>('/auth/login', data);
			return res.data;
		},
		onSuccess: async () => {
			const res = await api.get<UserDto[]>('/user/all');
			const users = res.data.map(normalizeUser);
			qc.setQueryData(['users'], users);
			if (users.length > 0) {
				setUser(users[0]);
			}
		},
	});
}

export function useLogout() {
	const logout = useAuthStore((s) => s.logout);
	const qc = useQueryClient();

	return useMutation({
		mutationFn: () => api.post('/auth/logout'),
		onSuccess: () => {
			logout();
			qc.clear();
		},
	});
}

export function useCreatePasswordToken() {
	return useMutation({
		mutationFn: (email: string) =>
			api.post(`/auth/create/passwordToken/${encodeURIComponent(email)}`),
	});
}

export function useRemindPassword() {
	return useMutation({
		mutationFn: (data: RemindPasswordRequest) =>
			api.post('/auth/remindPassword', data),
	});
}

export function useInitAuth() {
	const { setUser, setLoading } = useAuthStore();

	useEffect(() => {
		let cancelled = false;
		const silent = axios.create({
			baseURL: '/api',
			withCredentials: true,
			headers: {
				'Content-Type': 'application/json',
				Accept: 'application/json',
			},
		});

		(async () => {
			setLoading(true);
			try {
				const res = await silent.get<UserDto[]>('/user/all');
				if (!cancelled) {
					const users = res.data.map(normalizeUser);
					setUser(users.length > 0 ? users[0] : null);
				}
			} catch {
				if (!cancelled) setUser(null);
			}
		})();

		return () => {
			cancelled = true;
		};
	}, [setUser, setLoading]);
}
