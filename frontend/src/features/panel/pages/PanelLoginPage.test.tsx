import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PanelLoginPage from './PanelLoginPage';
import { usePanelAuthStore } from '../store/panelAuthStore';
import type { getPublicSiteSlots } from '@/lib/publicApi';

const { getSlotsMock } = vi.hoisted(() => ({
	getSlotsMock: vi.fn<typeof getPublicSiteSlots>(),
}));

vi.mock('@/lib/publicApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('@/lib/publicApi')>();
	return { ...actual, getPublicSiteSlots: getSlotsMock };
});

function renderLoginPage() {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false } },
	});
	return render(
		<QueryClientProvider client={queryClient}>
			<MemoryRouter>
				<PanelLoginPage />
			</MemoryRouter>
		</QueryClientProvider>,
	);
}

describe('PanelLoginPage', () => {
	beforeEach(() => {
		getSlotsMock.mockReset();
		usePanelAuthStore.getState().clear();
	});

	it('The login photo comes from the PANEL_LOGIN slot, so the client-zone photo never leaks onto the admin screen', async () => {
		// Given - both login slots configured; this screen must pick the PANEL one
		getSlotsMock.mockResolvedValue([
			{ slot: 'CLIENT_LOGIN', version: 7 },
			{ slot: 'PANEL_LOGIN', version: 99 },
		]);

		// When
		const { container } = renderLoginPage();

		// Then
		await waitFor(() => {
			expect(container.querySelector('img')?.getAttribute('src')).toBe(
				'/api/public/site/photo/PANEL_LOGIN?v=99',
			);
		});
	});
});
