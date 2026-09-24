import { describe, it, expect } from 'vitest';
import { PITCH, toSeconds } from './content';

describe('defense pitch', () => {
	it('Segments follow each other without gaps or overlaps, so the talk can be rehearsed against a clock', () => {
		// When / Then
		PITCH.slice(1).forEach((segment, i) => {
			expect(segment.from).toBe(PITCH[i].to);
		});
	});

	it('Starts at 0:00 and ends at exactly 5:00, which is the time the committee gives for the project talk', () => {
		// When / Then
		expect(PITCH[0].from).toBe('0:00');
		expect(toSeconds(PITCH[PITCH.length - 1].to)).toBe(300);
	});

	it('Is long enough to fill five minutes and short enough to finish in them at a calm speaking pace', () => {
		// Given
		const words = PITCH.flatMap((s) => s.text).join(' ').split(/\s+/).length;

		// When / Then
		expect(words).toBeGreaterThanOrEqual(550);
		expect(words).toBeLessThanOrEqual(750);
	});
});
