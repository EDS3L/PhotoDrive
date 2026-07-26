import { describe, it, expect, vi, beforeEach } from 'vitest';

const { apiClientMock } = vi.hoisted(() => ({
	apiClientMock: {
		get: vi.fn(),
		post: vi.fn(),
		patch: vi.fn(),
	},
}));

vi.mock('@/lib/apiClient', () => ({ apiClient: apiClientMock }));

import {
	login,
	logout,
	getCurrentUser,
	changePassword,
	getAssignedAlbums,
	requestPasswordToken,
	resetPassword,
} from './clientZoneApi';

/**
 * Strefa klienta rozmawia z tymi samymi endpointami auth co panel, ale jedna rzecz
 * różni ją zasadniczo: probe sesji musi być CICHY. Te testy przypinają kształt żądań,
 * bo kontrakt jest utrzymywany ręcznie (brak generacji z OpenAPI).
 */
describe('clientZoneApi', () => {
	beforeEach(() => {
		apiClientMock.get.mockReset().mockResolvedValue({ data: {} });
		apiClientMock.post.mockReset().mockResolvedValue({ data: undefined });
		apiClientMock.patch.mockReset().mockResolvedValue({ data: undefined });
	});

	it('The session probe is marked silent, so a visitor without a cookie sees the login form instead of a redirect loop', async () => {
		// When
		await getCurrentUser();

		// Then - without skipAuthRedirect the 401 interceptor would redirect, and the
		// re-hydration check would throw the client off the page it just opened
		expect(apiClientMock.get).toHaveBeenCalledWith('/user/me', {
			skipAuthRedirect: true,
		});
	});

	it('Logging in posts the credentials and relies on the cookie the server sets, so no token is handled in JS', async () => {
		// When
		await login({ email: 'klient@example.com', password: 'tajne' });

		// Then
		expect(apiClientMock.post).toHaveBeenCalledWith('/auth/login', {
			email: 'klient@example.com',
			password: 'tajne',
		});
	});

	it('Logging out is a server call, because only the server can clear an HttpOnly cookie', async () => {
		// When
		await logout();

		// Then
		expect(apiClientMock.post).toHaveBeenCalledWith('/auth/logout');
	});

	it('A client reads only the albums assigned to them', async () => {
		// Given
		apiClientMock.get.mockResolvedValue({ data: [] });

		// When
		await getAssignedAlbums();

		// Then
		expect(apiClientMock.get).toHaveBeenCalledWith('/album/getAllAssignedAlbums');
	});

	it('Requesting an authorisation code puts the e-mail in the path encoded, so a plus-addressed e-mail is not mangled', async () => {
		// When
		await requestPasswordToken('klient+test@example.com');

		// Then - a raw "+" in a path segment is decoded as a space by some servers, which
		// would send the code to a different address than the one typed
		expect(apiClientMock.post).toHaveBeenCalledWith(
			'/auth/create/passwordToken/klient%2Btest%40example.com',
		);
	});

	it('Resetting a password sends e-mail, code and new password together, because the backend answers a single uniform 400 for any failure', async () => {
		// When
		await resetPassword('klient@example.com', 'ABC123', 'noweHaslo1!');

		// Then - a swap of two of these strings compiles fine and shows the user the same
		// generic error as a wrong code (B.14 anti-enumeration), so it would be invisible
		expect(apiClientMock.post).toHaveBeenCalledWith('/auth/remindPassword', {
			email: 'klient@example.com',
			token: 'ABC123',
			newPassword: 'noweHaslo1!',
		});
	});

	it('The forced password change sends the current password too, which is what the server verifies before clearing the flag', async () => {
		// When
		await changePassword('klient-1', 'stare', 'nowe');

		// Then
		expect(apiClientMock.patch).toHaveBeenCalledWith(
			'/user/klient-1/changePassword',
			{ currentPassword: 'stare', newPassword: 'nowe' },
		);
	});
});
