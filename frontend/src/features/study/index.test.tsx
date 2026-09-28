import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import StudyPage from './index';
import { LEARNED_STORAGE_KEY } from './lib/useLearned';
import type { StudyQuestion } from './types';

const questions: StudyQuestion[] = [
	{
		n: 1,
		title: 'Systemy liczbowe',
		hints: ['podstawy 2 / 8 / 10 / 16', 'wartość cyfry zależy od pozycji'],
		short: [{ k: 'p', c: ['Krótka o systemach.'] }],
		long: [
			{ k: 'h3', c: ['Konwersje'] },
			{ k: 'li', c: [{ b: 'Dwójkowy' }, ' — cyfry 0 i 1.'] },
			{ k: 'table', rows: [['System', 'Podstawa'], ['binarny', '2']] },
		],
	},
	{
		n: 2,
		title: 'Programowanie strukturalne',
		hints: ['sekwencja, wybór, iteracja'],
		short: [{ k: 'p', c: ['Krótka o strukturach.'] }],
		long: [{ k: 'code', c: ['if (x) {\n  y();\n}'] }],
	},
];

describe('StudyPage', () => {
	beforeEach(() => {
		localStorage.clear();
	});

	it('Every question starts as not learned, and marking it learned swaps its answers for short memory cues', async () => {
		// Given
		render(<StudyPage questions={questions} />);
		const mark = screen.getByRole('button', { name: 'Pytanie 1 nauczone' });
		expect(mark).toHaveAttribute('aria-pressed', 'false');
		expect(screen.getByText('Nauczone: 0/2')).toBeInTheDocument();

		// When
		await userEvent.click(mark);

		// Then
		expect(mark).toHaveAttribute('aria-pressed', 'true');
		expect(screen.getByText('Systemy liczbowe')).toBeInTheDocument();
		expect(screen.queryByText('Krótka o systemach.')).not.toBeInTheDocument();
		expect(
			within(screen.getByRole('list', { name: 'Skróty myślowe do pytania 1' })).getAllByRole(
				'listitem',
			),
		).toHaveLength(2);
		expect(screen.getByText('wartość cyfry zależy od pozycji')).toBeInTheDocument();
		expect(screen.queryByText('sekwencja, wybór, iteracja')).not.toBeInTheDocument();
		expect(screen.getAllByRole('button', { name: /odpowiedź rozszerzona/i })).toHaveLength(1);
		expect(screen.getByText('Krótka o strukturach.')).toBeInTheDocument();
		expect(screen.getByText('Nauczone: 1/2')).toBeInTheDocument();

		// When
		await userEvent.click(mark);

		// Then
		expect(screen.getByText('Krótka o systemach.')).toBeInTheDocument();
		expect(screen.queryByText('wartość cyfry zależy od pozycji')).not.toBeInTheDocument();
	});

	it('Learned questions are remembered on the device, so they stay learned after the page is reopened', async () => {
		// Given
		const { unmount } = render(<StudyPage questions={questions} />);
		await userEvent.click(screen.getByRole('button', { name: 'Pytanie 2 nauczone' }));
		unmount();

		// When
		render(<StudyPage questions={questions} />);

		// Then
		expect(screen.getByRole('button', { name: 'Pytanie 2 nauczone' })).toHaveAttribute(
			'aria-pressed',
			'true',
		);
		expect(screen.queryByText('Krótka o strukturach.')).not.toBeInTheDocument();
		expect(screen.getByRole('button', { name: 'Pytanie 1 nauczone' })).toHaveAttribute(
			'aria-pressed',
			'false',
		);
	});

	it('Unreadable saved progress falls back to everything not learned instead of breaking the page', () => {
		// Given
		localStorage.setItem(LEARNED_STORAGE_KEY, '{not json');

		// When
		render(<StudyPage questions={questions} />);

		// Then
		expect(screen.getByText('Krótka o systemach.')).toBeInTheDocument();
		expect(screen.getByText('Nauczone: 0/2')).toBeInTheDocument();
	});

	it('Shows short answers up front and reveals the extended answer only on demand, so the page stays a quick-study list', async () => {
		// Given
		render(<StudyPage questions={questions} />);
		expect(screen.getByText('Krótka o systemach.')).toBeInTheDocument();
		expect(screen.queryByText('Konwersje')).not.toBeInTheDocument();

		// When
		const [firstToggle] = screen.getAllByRole('button', { name: /odpowiedź rozszerzona/i });
		await userEvent.click(firstToggle);

		// Then
		expect(screen.getByText('Konwersje')).toBeInTheDocument();
		expect(screen.getByText('Dwójkowy')).toBeInTheDocument();
		expect(screen.getByRole('cell', { name: 'binarny' })).toBeInTheDocument();
		expect(screen.queryByText(/y\(\);/)).not.toBeInTheDocument();
	});

	it('Searching narrows the list to matching questions and says so when nothing matches', async () => {
		// Given
		render(<StudyPage questions={questions} />);
		const search = screen.getByRole('searchbox', { name: 'Szukaj pytania' });

		// When
		await userEvent.type(search, 'strukturalne');

		// Then
		expect(screen.queryByText('Systemy liczbowe')).not.toBeInTheDocument();
		expect(screen.getByText('Programowanie strukturalne')).toBeInTheDocument();

		// When
		await userEvent.clear(search);
		await userEvent.type(search, 'nieistniejące hasło');

		// Then
		expect(screen.getByText('Brak pytań pasujących do wyszukiwania.')).toBeInTheDocument();
	});

	it('Expand-all opens every visible extended answer and collapse-all closes them again', async () => {
		// Given
		render(<StudyPage questions={questions} />);

		// When
		await userEvent.click(screen.getByRole('button', { name: 'Rozwiń wszystkie' }));

		// Then
		expect(screen.getByText('Konwersje')).toBeInTheDocument();
		expect(screen.getByText(/y\(\);/)).toBeInTheDocument();

		// When
		await userEvent.click(screen.getByRole('button', { name: 'Zwiń wszystkie' }));

		// Then
		expect(screen.queryByText('Konwersje')).not.toBeInTheDocument();
	});

	it('Committee order groups questions under the examiner responsible for them, and numeric order brings back the plain list', async () => {
		// Given
		render(<StudyPage questions={questions} />);
		expect(screen.queryByRole('heading', { name: 'Rychlik' })).not.toBeInTheDocument();

		// When
		await userEvent.click(screen.getByRole('button', { name: 'Komisyjnie' }));

		// Then
		const section = screen.getByRole('region', { name: 'Rychlik' });
		expect(section).toHaveTextContent('Systemy liczbowe');
		expect(section).toHaveTextContent('Programowanie strukturalne');
		expect(screen.queryByRole('heading', { name: 'Kasprowicz' })).not.toBeInTheDocument();
		expect(screen.getByRole('button', { name: 'Komisyjnie' })).toHaveAttribute('aria-pressed', 'true');

		// When
		await userEvent.click(screen.getByRole('button', { name: 'Numerycznie' }));

		// Then
		expect(screen.queryByRole('region', { name: 'Rychlik' })).not.toBeInTheDocument();
		expect(screen.getByText('Systemy liczbowe')).toBeInTheDocument();
	});

	it('Asks search engines not to index the page, because it is a private study aid on the public site', () => {
		// When
		render(<StudyPage questions={questions} />);

		// Then
		expect(document.head.querySelector('meta[name="robots"]')).toHaveAttribute(
			'content',
			'noindex, nofollow',
		);
	});
});
