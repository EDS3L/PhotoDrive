import { describe, it, expect } from 'vitest';
import { filterQuestions } from './filterQuestions';
import type { StudyQuestion } from '../types';

const q = (n: number, title: string, long: string): StudyQuestion => ({
	n,
	title,
	short: [{ k: 'p', c: ['krótko'] }],
	long: [{ k: 'p', c: [long] }],
});

const questions = [
	q(1, 'Systemy liczbowe', 'Liczba 16 zapisana szesnastkowo to 10.'),
	q(6, 'Rekurencja ogonowa', 'Optymalizacja wywołań ogonowych.'),
	q(16, 'Zakleszczenie procesów', 'Cztery warunki Coffmana i algorytm bankiera.'),
];

describe('filterQuestions', () => {
	it('Empty or blank query shows every question, so the page starts as the full study list', () => {
		// When / Then
		expect(filterQuestions(questions, '   ')).toEqual(questions);
	});

	it('Search looks inside the extended answer, not only the title, so a keyword finds the question that explains it', () => {
		// When
		const result = filterQuestions(questions, 'bankiera');

		// Then
		expect(result.map((x) => x.n)).toEqual([16]);
	});

	it('Search ignores case and Polish diacritics, so typing without Polish letters still finds the answer', () => {
		// When
		const result = filterQuestions(questions, 'WYWOLAN');

		// Then
		expect(result.map((x) => x.n)).toEqual([6]);
	});

	it('A question number jumps to that question instead of matching every occurrence of the digits', () => {
		// When
		const result = filterQuestions(questions, '16');

		// Then
		expect(result.map((x) => x.n)).toEqual([16]);
	});

	it('A number that is not a question number is searched as text, so years and values remain searchable', () => {
		// Given
		const withYear = [...questions, q(43, 'Model relacyjny', 'Codd opublikował go w 1970 roku.')];

		// When
		const result = filterQuestions(withYear, '1970');

		// Then
		expect(result.map((x) => x.n)).toEqual([43]);
	});
});
