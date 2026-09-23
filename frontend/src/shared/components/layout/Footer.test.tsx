import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Footer } from './Footer';

describe('Footer', () => {
	it('Has an inconspicuous "Nauka" entry leading to the study page, so it is reachable without appearing in the navigation', () => {
		// When
		render(
			<MemoryRouter>
				<Footer />
			</MemoryRouter>,
		);

		// Then
		const link = screen.getByRole('link', { name: 'Nauka' });
		expect(link).toHaveAttribute('href', '/nauka');
		expect(link.closest('ul')).toBeNull();
	});
});
