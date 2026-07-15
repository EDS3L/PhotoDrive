import { describe, it, expect, afterEach } from 'vitest';
import { panelPathForRole, redirectNonClientToPanel } from './rolePanelRedirect';

const originalLocation = window.location;

/** jsdom does not navigate, so we swap `location` for a plain object and read `href`. */
function stubLocation() {
	const location = { href: '' };
	Object.defineProperty(window, 'location', {
		configurable: true,
		writable: true,
		value: location,
	});
	return location;
}

describe('panelPathForRole', () => {
	it('An admin belongs to the admin panel, so the client zone is never their destination', () => {
		// Given / When / Then
		expect(panelPathForRole(['ADMIN'])).toBe('/admin');
	});

	it('A photographer belongs to the photographer panel', () => {
		expect(panelPathForRole(['PHOTOGRAPHER'])).toBe('/photographer');
	});

	it('A client has no panel, so they stay in the client zone', () => {
		expect(panelPathForRole(['CLIENT'])).toBeNull();
	});

	it('Admin wins over any other role, so a mixed account still lands in the admin panel', () => {
		// Given - a hypothetical account carrying more than one role
		expect(panelPathForRole(['PHOTOGRAPHER', 'ADMIN'])).toBe('/admin');
	});
});

describe('redirectNonClientToPanel', () => {
	afterEach(() => {
		Object.defineProperty(window, 'location', {
			configurable: true,
			writable: true,
			value: originalLocation,
		});
	});

	it('Sends a photographer who logged into the client zone to their panel', () => {
		// Given
		const location = stubLocation();

		// When
		const redirected = redirectNonClientToPanel(['PHOTOGRAPHER']);

		// Then
		expect(redirected).toBe(true);
		expect(location.href).toBe('/photographer');
	});

	it('Leaves a client where they are, so the client zone loads for them normally', () => {
		// Given
		const location = stubLocation();

		// When
		const redirected = redirectNonClientToPanel(['CLIENT']);

		// Then - no navigation, the caller proceeds to set the client session
		expect(redirected).toBe(false);
		expect(location.href).toBe('');
	});
});
