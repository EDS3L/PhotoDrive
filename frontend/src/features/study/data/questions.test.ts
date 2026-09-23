import { describe, it, expect } from 'vitest';
import { questions } from './questions';

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
});
