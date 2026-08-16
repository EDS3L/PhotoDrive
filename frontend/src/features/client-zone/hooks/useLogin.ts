import { useMutation } from '@tanstack/react-query';
import { login, getCurrentUser, type CurrentUser } from '../api/clientZoneApi';
import { useAuthStore } from '@/app/store/authStore';
import { redirectNonClientToPanel } from '../lib/rolePanelRedirect';
import type { LoginRequest } from '@/shared/types/api';
import type { AxiosError } from 'axios';

export function useLogin() {
	const setSession = useAuthStore((s) => s.setSession);

	return useMutation<CurrentUser, AxiosError, LoginRequest>({
		meta: { skipGlobalError: true },
		mutationFn: async (data) => {
			await login(data);
			return getCurrentUser();
		},
		onSuccess: (me, variables) => {
			if (redirectNonClientToPanel(me.roles)) return;
			setSession({
				email: me.email,
				userId: me.id,
				mustChangePassword: me.changePasswordOnNextLogin,
				loginPassword: variables.password,
			});
		},
	});
}
