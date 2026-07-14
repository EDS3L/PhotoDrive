import { describe, it, expect, beforeEach } from 'vitest';
import { resolveRole, usePanelAuthStore } from './panelAuthStore';
import type { CurrentUserInfo } from '../types/panel';

const admin: CurrentUserInfo = {
	id: 'u-1',
	name: 'Ala',
	email: 'ala@photodrive.dev',
	roles: ['ADMIN'],
	changePasswordOnNextLogin: false,
};

describe('resolveRole', () => {
	it('gives ADMIN the wider panel when the account holds both roles', () => {
		// Given - an admin who is also a photographer
		// When / Then - the admin panel is a superset, so it wins
		expect(resolveRole(['PHOTOGRAPHER', 'ADMIN'])).toBe('ADMIN');
	});

	it('recognises a photographer', () => {
		// Given / When / Then
		expect(resolveRole(['PHOTOGRAPHER'])).toBe('PHOTOGRAPHER');
	});

	it('gives a client no panel role at all', () => {
		// Given - a client account, which belongs in the client zone
		// When / Then - null keeps them out of the panel entirely
		expect(resolveRole(['CLIENT'])).toBeNull();
		expect(resolveRole([])).toBeNull();
	});
});

describe('usePanelAuthStore', () => {
	beforeEach(() => {
		usePanelAuthStore.getState().clear();
	});

	it('derives the role from the roles returned by /user/me', () => {
		// Given / When
		usePanelAuthStore.getState().setUser({ ...admin, roles: ['PHOTOGRAPHER'] });

		// Then
		expect(usePanelAuthStore.getState().role).toBe('PHOTOGRAPHER');
		expect(usePanelAuthStore.getState().isAuthenticated).toBe(true);
	});

	it('wipes the password kept for the forced change when the session ends', () => {
		// Given - the login password is held in memory for the "set a new password" screen
		usePanelAuthStore.getState().setUser(admin);
		usePanelAuthStore.getState().setLoginPassword('haslo-startowe');

		// When - the user logs out
		usePanelAuthStore.getState().clear();

		// Then - no plaintext password survives in memory after logout
		expect(usePanelAuthStore.getState().loginPassword).toBeNull();
		expect(usePanelAuthStore.getState().user).toBeNull();
		expect(usePanelAuthStore.getState().isAuthenticated).toBe(false);
	});
});
