import { describe, it, expect, beforeEach } from 'vitest';
import { AxiosError } from 'axios';
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { getApiErrorMessage, queryClient } from './queryClient';
import { useToastStore } from '@/shared/store/toastStore';

function axiosError(status: number, data?: unknown): AxiosError {
	const config = { headers: {} } as InternalAxiosRequestConfig;
	const response = {
		status,
		data,
		statusText: '',
		headers: {},
		config,
	} as AxiosResponse;
	return new AxiosError(
		`Request failed with status code ${status}`,
		'ERR_BAD_REQUEST',
		config,
		undefined,
		response,
	);
}

describe('getApiErrorMessage', () => {
	it('shows the message written by the backend, not a generic HTTP phrase', () => {
		// Given - the backend explains the domain rule it enforced
		const error = axiosError(400, { message: 'Album o tej nazwie już istnieje.' });

		// When / Then
		expect(getApiErrorMessage(error)).toBe('Album o tej nazwie już istnieje.');
	});

	it('falls back to the transport message when the response carries no body', () => {
		// Given - e.g. the server is unreachable
		const error = new AxiosError('Network Error');

		// When / Then
		expect(getApiErrorMessage(error)).toBe('Network Error');
	});

	it('reads the message of an ordinary Error', () => {
		// Given / When / Then
		expect(getApiErrorMessage(new Error('Coś poszło nie tak'))).toBe('Coś poszło nie tak');
	});

	it('never shows an empty toast, even for a thrown value that is not an Error', () => {
		// Given - something outside our control was thrown
		// When / Then - the user gets a sentence, not "undefined"
		expect(getApiErrorMessage('nope')).toBe(
			'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
		);
		expect(getApiErrorMessage(null)).toBe(
			'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
		);
	});
});

/**
 * Globalna obsługa błędów mutacji: pojedyncze `onError` w `MutationCache` zamiast
 * powtarzanego try/catch przy każdej mutacji. Testy pilnują dwóch wyjątków od reguły,
 * bo oba są łatwe do zepsucia: formularz logowania i 401.
 */
describe('queryClient — global mutation error handling', () => {
	beforeEach(() => {
		useToastStore.setState({ toasts: [] });
	});

	async function runFailingMutation(
		error: unknown,
		meta?: Record<string, unknown>,
	) {
		const mutation = queryClient.getMutationCache().build<unknown, unknown, void, unknown>(
			queryClient,
			{
				meta,
				mutationFn: async () => {
					throw error;
				},
			},
		);
		await mutation.execute(undefined).catch(() => undefined);
	}

	it('turns an unhandled mutation failure into a toast, so no error passes silently', async () => {
		// Given / When
		await runFailingMutation(axiosError(400, { message: 'Nie można usunąć znaku wodnego.' }));

		// Then
		expect(useToastStore.getState().toasts).toEqual([
			expect.objectContaining({
				variant: 'error',
				message: 'Nie można usunąć znaku wodnego.',
			}),
		]);
	});

	it('stays silent for a mutation that shows its own inline error', async () => {
		// Given - the login form opts out to render the error next to the field
		// When
		await runFailingMutation(axiosError(401, { message: 'Zły login lub hasło' }), {
			skipGlobalError: true,
		});

		// Then - no floating toast duplicating the inline message
		expect(useToastStore.getState().toasts).toHaveLength(0);
	});

	it('stays silent on 401, because the interceptor already redirects to the login screen', async () => {
		// Given - the session expired mid-action
		// When
		await runFailingMutation(axiosError(401, { message: 'Unauthorized' }));

		// Then - a toast on a page we are leaving anyway would only add noise
		expect(useToastStore.getState().toasts).toHaveLength(0);
	});
});
