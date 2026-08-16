import { MutationCache, QueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { toast } from '@/shared/store/toastStore';

export function getApiErrorMessage(error: unknown): string {
	if (axios.isAxiosError(error)) {
		const data = error.response?.data as { message?: string } | undefined;
		if (data?.message) return data.message;
		if (error.message) return error.message;
	}
	if (error instanceof Error && error.message) return error.message;
	return 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.';
}

export const queryClient = new QueryClient({
	mutationCache: new MutationCache({
		onError: (error, _variables, _context, mutation) => {
			if (mutation.meta?.skipGlobalError) return;
			if (axios.isAxiosError(error) && error.response?.status === 401) return;
			toast.error(getApiErrorMessage(error));
		},
	}),
	defaultOptions: {
		queries: {
			retry: 1,
			staleTime: 5 * 60 * 1000,
			refetchOnWindowFocus: false,
		},
	},
});
