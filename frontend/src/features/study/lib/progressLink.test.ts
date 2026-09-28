import { describe, it, expect } from 'vitest';
import { decodeProgress, encodeProgress, progressUrl, readProgressFromHash } from './progressLink';

describe('progressLink', () => {
	it('Round-trips any set of learned questions, so the other device gets exactly the same progress', () => {
		// Given
		const learned = new Set([1, 8, 9, 16, 33, 67]);

		// When
		const decoded = decodeProgress(encodeProgress(learned));

		// Then
		expect(decoded).toEqual(learned);
	});

	it('Keeps a link for all 67 questions short enough to send between devices', () => {
		// Given
		const all = new Set(Array.from({ length: 67 }, (_, i) => i + 1));

		// When
		const code = encodeProgress(all);

		// Then
		expect(code.length).toBeLessThanOrEqual(12);
		expect(code).toMatch(/^[A-Za-z0-9_-]+$/);
	});

	it('Treats an empty code as "nothing learned", so a device can be reset from a link', () => {
		// When / Then
		expect(decodeProgress(encodeProgress(new Set()))).toEqual(new Set());
	});

	it('Rejects a damaged code instead of guessing progress from it', () => {
		// When / Then
		expect(decodeProgress('ab$c')).toBeNull();
		expect(decodeProgress('A')).toBeNull();
	});

	it('Reads progress only from the "p" hash parameter and tells a missing link from a damaged one', () => {
		// When / Then
		expect(readProgressFromHash('')).toBeUndefined();
		expect(readProgressFromHash('#pytanie-9')).toBeUndefined();
		expect(readProgressFromHash(`#p=${encodeProgress(new Set([3]))}`)).toEqual(new Set([3]));
		expect(readProgressFromHash('#p=ab$c')).toBeNull();
	});

	it('Builds the link on the current page address and replaces any previous progress in it', () => {
		// When
		const url = progressUrl(new Set([2]), 'https://photodrive.dev/nauka#p=old');

		// Then
		expect(url).toBe(`https://photodrive.dev/nauka#p=${encodeProgress(new Set([2]))}`);
	});
});
