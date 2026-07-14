import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { AxiosError } from 'axios';
import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios';
import { apiClient } from './apiClient';

const originalAdapter = apiClient.defaults.adapter;
const originalLocation = window.location;

/** Every request fails with the given status — we test the interceptor, not the network. */
function failWith(status: number): AxiosAdapter {
	return async (config: InternalAxiosRequestConfig) => {
		throw new AxiosError(
			`Request failed with status code ${status}`,
			'ERR_BAD_REQUEST',
			config,
			undefined,
			{ status, data: {}, statusText: '', headers: {}, config },
		);
	};
}

/** jsdom does not navigate, so we swap `location` for a plain object and read `href`. */
function stubLocation(pathname: string) {
	const location = { pathname, href: '' };
	Object.defineProperty(window, 'location', {
		configurable: true,
		writable: true,
		value: location,
	});
	return location;
}

/**
 * Cookie z JWT wygasa po 60 min — wtedy backend odpowiada 401. Interceptor decyduje,
 * DOKĄD odesłać użytkownika (panel vs strefa klienta) i kiedy NIE przekierowywać:
 * pomyłka tutaj = wyrzucanie z formularza logowania albo pętla przekierowań po F5.
 */
describe('apiClient — reaction to 401', () => {
	beforeEach(() => {
		apiClient.defaults.adapter = failWith(401);
	});

	afterEach(() => {
		apiClient.defaults.adapter = originalAdapter;
		Object.defineProperty(window, 'location', {
			configurable: true,
			writable: true,
			value: originalLocation,
		});
	});

	it('sends an expired panel session back to the panel login screen', async () => {
		// Given - the photographer was working in the panel
		const location = stubLocation('/admin/albums');

		// When
		await expect(apiClient.get('/user/me')).rejects.toThrow();

		// Then - not to the client login, which would not let them back in
		expect(location.href).toBe('/panel-login');
	});

	it('treats the photographer routes as the panel as well', async () => {
		// Given
		const location = stubLocation('/photographer/albums/42');

		// When
		await expect(apiClient.get('/album/all')).rejects.toThrow();

		// Then
		expect(location.href).toBe('/panel-login');
	});

	it('sends an expired client session to the client zone', async () => {
		// Given - the client was browsing their album
		const location = stubLocation('/strefa-klienta');

		// When
		await expect(apiClient.get('/album/getAllAssignedAlbums')).rejects.toThrow();

		// Then
		expect(location.href).toBe('/strefa-klienta');
	});

	it('does not redirect on a failed login, so the form can show "wrong password"', async () => {
		// Given - a 401 from /auth/login means bad credentials, not an expired session
		const location = stubLocation('/panel-login');

		// When
		await expect(
			apiClient.post('/auth/login', { email: 'a@b.pl', password: 'zle' }),
		).rejects.toThrow();

		// Then - the user stays on the form with their typed email
		expect(location.href).toBe('');
	});

	it('does not redirect when the session is probed silently', async () => {
		// Given - after F5 the client zone asks /user/me just to check whether a cookie is still valid
		const location = stubLocation('/strefa-klienta');

		// When
		await expect(
			apiClient.get('/user/me', { skipAuthRedirect: true }),
		).rejects.toThrow();

		// Then - a "no session" answer is a valid answer, not a reason to bounce the page
		expect(location.href).toBe('');
	});

	it('leaves other errors alone and lets the caller handle them', async () => {
		// Given - a server error, not an authentication problem
		apiClient.defaults.adapter = failWith(500);
		const location = stubLocation('/admin/albums');

		// When
		await expect(apiClient.get('/album/all')).rejects.toThrow();

		// Then - the user is not thrown out of the panel over a single broken request
		expect(location.href).toBe('');
	});
});
