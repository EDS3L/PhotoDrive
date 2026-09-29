import { describe, it, expect } from 'vitest';
import {
	CHALLENGES,
	CHALLENGES_INTRO,
	CHALLENGES_OUTRO,
	PITCH,
	SLIDES,
	TEST_STATS,
	toSeconds,
	wordCount,
} from './content';

const CALM_WORDS_PER_MINUTE = 130;

describe('defense pitch', () => {
	it('Walks through every slide of the presentation exactly once and in order, so speaking and clicking stay in sync', () => {
		// When
		const slides = PITCH.map((segment) => segment.slide);

		// Then
		expect(slides).toEqual(SLIDES.map((_, i) => i + 1));
	});

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
		const words = wordCount(PITCH.flatMap((s) => s.text));

		// When / Then
		expect(words).toBeGreaterThanOrEqual(550);
		expect(words).toBeLessThanOrEqual(750);
	});
});

describe('challenges answer', () => {
	it('Takes three to four minutes at a calm pace, which is the time the recurring question deserves', () => {
		// Given
		const words = wordCount([CHALLENGES_INTRO, ...CHALLENGES.map((c) => c.text), CHALLENGES_OUTRO]);

		// When
		const minutes = words / CALM_WORDS_PER_MINUTE;

		// Then
		expect(minutes).toBeGreaterThanOrEqual(3);
		expect(minutes).toBeLessThanOrEqual(4);
	});
});

describe('test statistics', () => {
	it('Quoted totals add up, so no number on the page contradicts another in front of the committee', () => {
		// Given
		const { total, backend, frontend, pyramid } = TEST_STATS;

		// When / Then
		expect(backend + frontend).toBe(total);
		expect(pyramid.domain + pyramid.unit + pyramid.integration).toBe(backend);
	});
});
