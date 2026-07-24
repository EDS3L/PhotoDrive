import { useMutation } from '@tanstack/react-query';
import { sendContactMessage, type ContactRequest } from '@/lib/publicApi';
import type { AxiosError } from 'axios';

/**
 * Wysyła wiadomość z formularza kontaktowego. Błąd trafia do globalnego toasta
 * (MutationCache.onError) — formularz zostaje, żeby gość mógł spróbować ponownie.
 */
export function useSendContactMessage() {
	return useMutation<void, AxiosError, ContactRequest>({
		mutationFn: sendContactMessage,
	});
}
