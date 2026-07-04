import { useMutation } from '@tanstack/react-query';
import { login, getCurrentUser, type CurrentUser } from '../api/clientZoneApi';
import { useAuthStore } from '@/app/store/authStore';
import type { LoginRequest } from '@/shared/types/api';
import type { AxiosError } from 'axios';

export function useLogin() {
	const setSession = useAuthStore((s) => s.setSession);

	return useMutation<CurrentUser, AxiosError, LoginRequest>({
		// formularz logowania pokazuje błąd inline — pomijamy globalny toast
		meta: { skipGlobalError: true },
		mutationFn: async (data) => {
			await login(data);
			// pobieramy /me, by mieć id + flagę wymuszonej zmiany hasła
			return getCurrentUser();
		},
		onSuccess: (me, variables) => {
			setSession({
				email: me.email,
				userId: me.id,
				mustChangePassword: me.changePasswordOnNextLogin,
				loginPassword: variables.password,
			});
		},
	});
}
