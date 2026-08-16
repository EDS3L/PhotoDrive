import { useMutation } from '@tanstack/react-query';
import { sendContactMessage, type ContactRequest } from '@/lib/publicApi';
import type { AxiosError } from 'axios';

export function useSendContactMessage() {
	return useMutation<void, AxiosError, ContactRequest>({
		mutationFn: sendContactMessage,
	});
}
