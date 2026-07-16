import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PortfolioPage from './index';
import type {
	getPublicAlbums,
	getPublicPhotosByAlbumName,
	getPublicSiteSlots,
} from '@/lib/publicApi';

const { getPublicAlbumsMock, getPhotosMock, getSlotsMock } = vi.hoisted(() => ({
	getPublicAlbumsMock: vi.fn<typeof getPublicAlbums>(),
	getPhotosMock: vi.fn<typeof getPublicPhotosByAlbumName>(),
	getSlotsMock: vi.fn<typeof getPublicSiteSlots>(),
}));

// Podmieniamy TYLKO wywołania HTTP; hooki, sortowanie i logika zakładek zostają prawdziwe.
vi.mock('@/lib/publicApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('@/lib/publicApi')>();
	return {
		...actual,
		getPublicAlbums: getPublicAlbumsMock,
		getPublicPhotosByAlbumName: getPhotosMock,
		getPublicSiteSlots: getSlotsMock,
	};
});

function renderPortfolio() {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false } },
	});
	return render(
		<QueryClientProvider client={queryClient}>
			<MemoryRouter>
				<PortfolioPage />
			</MemoryRouter>
		</QueryClientProvider>,
	);
}

describe('PortfolioPage', () => {
	beforeEach(() => {
		getPublicAlbumsMock.mockReset();
		getPhotosMock.mockReset();
		getSlotsMock.mockReset();
		// CTASection na dole strony pyta o sloty — tutaj nieistotne
		getSlotsMock.mockResolvedValue([]);
		getPhotosMock.mockResolvedValue({ albumId: 'a', name: 'x', photos: [] });
	});

	it('Tabs come from public albums with admin labels, and an empty album gets no tab a guest could hit', async () => {
		// Given - two albums with photos (one labeled, one not) and one still empty
		getPublicAlbumsMock.mockResolvedValue([
			{ albumId: 'a1', name: 'sluby', displayName: 'Śluby', photoCount: 12 },
			{ albumId: 'a2', name: 'plener-2026', displayName: null, photoCount: 5 },
			{ albumId: 'a3', name: 'nowy-pusty', displayName: 'Nowa sesja', photoCount: 0 },
		]);

		// When
		renderPortfolio();

		// Then - the Unicode label shows; the unlabeled album falls back to its technical name
		expect(await screen.findByRole('button', { name: 'Śluby' })).toBeInTheDocument();
		expect(screen.getByRole('button', { name: 'plener-2026' })).toBeInTheDocument();
		// ...and the empty album is not offered at all
		expect(screen.queryByRole('button', { name: 'Nowa sesja' })).not.toBeInTheDocument();
	});

	it('Clicking a tab loads exactly that album, so the grid never shows another category', async () => {
		// Given
		getPublicAlbumsMock.mockResolvedValue([
			{ albumId: 'a1', name: 'sluby', displayName: 'Śluby', photoCount: 12 },
			{ albumId: 'a2', name: 'plener', displayName: 'Plener', photoCount: 5 },
		]);
		const user = userEvent.setup();
		renderPortfolio();
		await screen.findByRole('button', { name: 'Plener' });

		// When - the guest switches to the second tab
		await user.click(screen.getByRole('button', { name: 'Plener' }));

		// Then - photos are requested for the album behind THAT tab
		await waitFor(() => {
			expect(getPhotosMock).toHaveBeenCalledWith('plener');
		});
	});

	it('The first tab is active by default, so the page opens with real photos instead of an unpicked state', async () => {
		// Given - backend returns albums already sorted by displayOrder
		getPublicAlbumsMock.mockResolvedValue([
			{ albumId: 'a1', name: 'sluby', displayName: 'Śluby', photoCount: 12 },
			{ albumId: 'a2', name: 'plener', displayName: 'Plener', photoCount: 5 },
		]);

		// When
		renderPortfolio();
		await screen.findByRole('button', { name: 'Śluby' });

		// Then - photos of the FIRST album load without any click
		await waitFor(() => {
			expect(getPhotosMock).toHaveBeenCalledWith('sluby');
		});
	});
});
