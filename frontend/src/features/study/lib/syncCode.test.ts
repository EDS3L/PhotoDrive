import { describe, it, expect } from 'vitest';
import {
	generateSyncCode,
	isSyncCode,
	parseSyncInput,
	readSyncCodeFromHash,
	syncUrl,
} from './syncCode';

const CODE = 'k7Qp2xWmZ-a_9LrT0bNc4e';

describe('syncCode', () => {
	it('Generates 22 base64url characters (128 random bits), the exact format the backend accepts', () => {
		// When
		const code = generateSyncCode();

		// Then
		expect(code).toMatch(/^[A-Za-z0-9_-]{22}$/);
		expect(isSyncCode(code)).toBe(true);
	});

	it('Generates a different code every time, so two people never share progress by accident', () => {
		// When
		const codes = new Set(Array.from({ length: 50 }, generateSyncCode));

		// Then
		expect(codes.size).toBe(50);
	});

	it('Reads the code only from the "sync" hash parameter and tells a missing link from a damaged one', () => {
		// When / Then
		expect(readSyncCodeFromHash('')).toBeUndefined();
		expect(readSyncCodeFromHash('#pytanie-9')).toBeUndefined();
		expect(readSyncCodeFromHash(`#sync=${CODE}`)).toBe(CODE);
		expect(readSyncCodeFromHash('#sync=short')).toBeNull();
	});

	it('Accepts a pasted link or a bare code, and rejects anything else', () => {
		// When / Then
		expect(parseSyncInput(`  ${CODE} `)).toBe(CODE);
		expect(parseSyncInput(`https://photodrive.dev/nauka#sync=${CODE}`)).toBe(CODE);
		expect(parseSyncInput('https://photodrive.dev/nauka')).toBeNull();
		expect(parseSyncInput('https://photodrive.dev/nauka#sync=short')).toBeNull();
		expect(parseSyncInput('abc')).toBeNull();
	});

	it('Builds the link on the current page address and replaces any previous hash', () => {
		// When / Then
		expect(syncUrl(CODE, 'https://photodrive.dev/nauka#pytanie-3')).toBe(
			`https://photodrive.dev/nauka#sync=${CODE}`,
		);
	});
});
