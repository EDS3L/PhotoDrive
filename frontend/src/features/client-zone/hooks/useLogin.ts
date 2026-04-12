import { useMutation } from '@tanstack/react-query';
import { login } from '../api/clientZoneApi';
import { useAuthStore } from '@/app/store/authStore';
import type { LoginRequest } from '@/shared/types/api';
import type { AxiosError } from 'axios';

export function useLogin() {
	const setAuthenticated = useAuthStore((s) => s.setAuthenticated);

	return useMutation<void, AxiosError, LoginRequest>({
		mutationFn: login,
		onSuccess: (_data, variables) => {
			setAuthenticated(true, variables.email);
		},
	});
}
