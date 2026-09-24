import { describe, it, expect } from 'vitest';
import { COMMITTEE, groupByCommittee } from './committee';
import { questions } from '../data/questions';
import type { StudyQuestion } from '../types';

const q = (n: number): StudyQuestion => ({
	n,
	title: `Pytanie ${n}`,
	short: [{ k: 'p', c: ['k'] }],
	long: [{ k: 'p', c: ['r'] }],
});

describe('groupByCommittee', () => {
	it('Every exam question lands in exactly one group, so the committee view neither drops nor duplicates a question', () => {
		// When
		const groups = groupByCommittee(questions);

		// Then
		const numbers = groups.flatMap((g) => g.questions.map((x) => x.n));
		expect(numbers).toHaveLength(67);
		expect(new Set(numbers).size).toBe(67);
	});

	it('No question is assigned to two committee members, so the groups never compete for the same topic', () => {
		// When
		const assigned = COMMITTEE.flatMap((m) => m.questions);

		// Then
		expect(new Set(assigned).size).toBe(assigned.length);
	});

	it('Groups follow the committee order with leftovers last, and questions inside a group stay in numeric order', () => {
		// Given
		const input = [q(64), q(45), q(2), q(35), q(40), q(1), q(11)];

		// When
		const groups = groupByCommittee(input);

		// Then
		expect(groups.map((g) => [g.name, g.questions.map((x) => x.n)])).toEqual([
			['Duraj', [35, 64]],
			['Kasprowicz', [40, 45]],
			['Rychlik', [1, 2]],
			['Pozostałe', [11]],
		]);
	});

	it('A member with no matching question is hidden, so a search result shows only the relevant examiners', () => {
		// When
		const groups = groupByCommittee([q(42)]);

		// Then
		expect(groups.map((g) => g.name)).toEqual(['Kasprowicz']);
	});
});
