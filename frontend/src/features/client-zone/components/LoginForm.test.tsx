import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { LoginForm } from './LoginForm';
import type { getPublicSiteSlots } from '@/lib/publicApi';

const { getSlotsMock } = vi.hoisted(() => ({
	getSlotsMock: vi.fn<typeof getPublicSiteSlots>(),
}));

vi.mock('@/lib/publicApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('@/lib/publicApi')>();
	return { ...actual, getPublicSiteSlots: getSlotsMock };
});

function renderLogin() {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false } },
	});
	return render(
		<QueryClientProvider client={queryClient}>
			<MemoryRouter>
				<LoginForm />
			</MemoryRouter>
		</QueryClientProvider>,
	);
}

describe('LoginForm', () => {
	beforeEach(() => {
		getSlotsMock.mockReset();
	});

	it('The login photo comes from the CLIENT_LOGIN slot, not from any other section of the site', async () => {
		// Given - several slots configured; only CLIENT_LOGIN belongs on this screen
		getSlotsMock.mockResolvedValue([
			{ slot: 'HOME_HERO', version: 1 },
			{ slot: 'CLIENT_LOGIN', version: 42 },
		]);

		// When
		const { container } = renderLogin();

		// Then
		await waitFor(() => {
			expect(container.querySelector('img')?.getAttribute('src')).toBe(
				'/api/public/site/photo/CLIENT_LOGIN?v=42',
			);
		});
	});

	it('Without a configured slot the screen falls back to a placeholder instead of a broken image', async () => {
		// Given
		getSlotsMock.mockResolvedValue([]);

		// When
		const { container } = renderLogin();

		// Then - whatever the placeholder is, it is NOT the slot endpoint
		await waitFor(() => {
			const src = container.querySelector('img')?.getAttribute('src');
			expect(src).toBeTruthy();
			expect(src).not.toContain('/api/public/site/photo/');
		});
	});
});
