import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Footer } from './Footer';

describe('Footer', () => {
	it.each([
		['Nauka', '/nauka'],
		['Projekt', '/projekt'],
	])(
		'Has an inconspicuous "%s" entry leading to %s, so it is reachable without appearing in the navigation',
		(name, href) => {
			// When
			render(
				<MemoryRouter>
					<Footer />
				</MemoryRouter>,
			);

			// Then
			const link = screen.getByRole('link', { name });
			expect(link).toHaveAttribute('href', href);
			expect(link.closest('ul')).toBeNull();
		},
	);
});
