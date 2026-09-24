import { describe, it, expect } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import ProjectPage from './index';
import { SECTIONS } from './content';

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

	it('Opens with the timed five-minute pitch, since that is what gets rehearsed before the defense', () => {
		// When
		render(<ProjectPage />);

		// Then
		const pitch = screen.getByRole('region', { name: 'Pitch na 5 minut' });
		expect(within(pitch).getByText(/0:00–0:40/)).toBeInTheDocument();
		expect(within(pitch).getByText(/4:30–5:00/)).toBeInTheDocument();
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
