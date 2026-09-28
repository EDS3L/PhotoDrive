import { describe, it, expect } from 'vitest';
import { questions } from './questions';
import type { StudyQuestion } from '../types';

describe('study questions data', () => {
	it('Contains all 67 exam questions numbered 1 to 67 in order, so none was lost in the document conversion', () => {
		// When / Then
		expect(questions.map((q) => q.n)).toEqual(
			Array.from({ length: 67 }, (_, i) => i + 1),
		);
	});

	it('Every question has both a short and an extended answer, since the page is built around the two-level study mode', () => {
		// When
		const incomplete = questions.filter((q) => !q.short.length || !q.long.length);

		// Then
		expect(incomplete).toEqual([]);
	});

	it('Every question has 3 to 5 short memory cues for learned mode, so a learned question shows prompts to recall and not the full answer again', () => {
		// When
		const invalid = questions
			.filter(
				(q) =>
					!Array.isArray(q.hints) ||
					q.hints.length < 3 ||
					q.hints.length > 5 ||
					q.hints.some((hint) => !hint.trim() || hint.length > 90),
			)
			.map((q) => q.n);

		// Then
		expect(invalid).toEqual([]);
	});

	it('The number-systems example converts correctly in every base and uses an easy-to-remember number, so it holds up when recited at the defence', () => {
		// Given
		const [first] = questions;
		const short = JSON.stringify(first.short);

		// When
		const match = short.match(/(\d+)\(10\) = ([01]+)\(2\) = ([0-7]+)\(8\) = ([0-9A-F]+)\(16\)/);

		// Then
		expect(match).not.toBeNull();
		const [, dec, bin, oct, hex] = match!;
		expect(dec).toBe('255');
		expect([parseInt(bin, 2), parseInt(oct, 8), parseInt(hex, 16)]).toEqual([255, 255, 255]);
		expect(JSON.stringify(first)).not.toContain('202');
	});

	it('Contains no leftover Markdown backticks, so code names render as text and not as raw markup', () => {
		// When / Then
		expect(JSON.stringify(questions)).not.toContain('`');
	});

	it('Contains no raw Markdown heading markers in text blocks, so section titles render as headings and not as "###"', () => {
		// When
		const withMarkers = questions.flatMap((q) =>
			[...q.short, ...q.long]
				.filter(
					(block) =>
						block.k !== 'table' &&
						block.k !== 'code' &&
						block.c.some((part) => /(^|\s)#{2,}\s/.test(typeof part === 'string' ? part : part.b)),
				)
				.map(() => q.n),
		);

		// Then
		expect(withMarkers).toEqual([]);
	});

	it('Mentions the diploma project only in a closing "Z projektu" note, so the answers themselves stay general facts', () => {
		// Given
		const text = (blocks: StudyQuestion['long']) => JSON.stringify(blocks);

		// When
		const violations = questions
			.filter((q) => {
				const noteAt = q.long.findIndex((b) => b.k === 'h3' && b.c[0] === 'Z projektu');
				const body = noteAt === -1 ? q.long : q.long.slice(0, noteAt);
				const noteIsLast = noteAt === -1 || noteAt === q.long.length - 2;
				return /PhotoDrive/.test(text(q.short)) || /PhotoDrive/.test(text(body)) || !noteIsLast;
			})
			.map((q) => q.n);

		// Then
		expect(violations).toEqual([]);
	});
});
