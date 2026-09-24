import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import StudyPage from './index';
import type { StudyQuestion } from './types';

const questions: StudyQuestion[] = [
	{
		n: 1,
		title: 'Systemy liczbowe',
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
		short: [{ k: 'p', c: ['Krótka o strukturach.'] }],
		long: [{ k: 'code', c: ['if (x) {\n  y();\n}'] }],
	},
];

describe('StudyPage', () => {
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
