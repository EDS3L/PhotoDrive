import { describe, it, expect } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import ProjectPage from './index';
import { CHALLENGES, SECTIONS } from './content';

describe('ProjectPage', () => {
	it('Every table-of-contents link points to a section that exists, so no jump lands on nothing', () => {
		// When
		render(<ProjectPage />);

		// Then
		const nav = screen.getByRole('navigation', { name: 'Spis sekcji' });
		const links = within(nav).getAllByRole('link');
		expect(links).toHaveLength(SECTIONS.length);
		links.forEach((link) => {
			const id = link.getAttribute('href')!.slice(1);
			expect(document.getElementById(id)).not.toBeNull();
		});
	});

	it('Opens with the timed five-minute pitch labelled by slide, since that is what gets rehearsed with the presentation', () => {
		// When
		render(<ProjectPage />);

		// Then
		const pitch = screen.getByRole('region', { name: 'Pitch na 5 minut' });
		expect(within(pitch).getByText('Slajd 1 · 0:00–0:20 · PhotoDrive')).toBeInTheDocument();
		expect(within(pitch).getByText('Slajd 8 · 4:30–5:00 · Wnioski i dalszy rozwój')).toBeInTheDocument();
	});

	it('Has a ready answer about the biggest challenges with every story in it, because that question comes up at every defense', () => {
		// When
		render(<ProjectPage />);

		// Then
		const challenges = screen.getByRole('region', { name: 'Największe wyzwania i problemy' });
		CHALLENGES.forEach((c, i) => {
			expect(within(challenges).getByText(`${i + 1}. ${c.title}`)).toBeInTheDocument();
		});
	});

	it('Asks search engines not to index the page, because it is a private preparation aid on the public site', () => {
		// When
		render(<ProjectPage />);

		// Then
		expect(document.head.querySelector('meta[name="robots"]')).toHaveAttribute(
			'content',
			'noindex, nofollow',
		);
	});
});
