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
