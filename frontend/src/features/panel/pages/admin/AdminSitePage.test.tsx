import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AdminSitePage from './AdminSitePage';
import type {
	getSiteSlots,
	uploadSiteSlotImage,
	deleteSiteSlotImage,
} from '../../api/siteSlotsApi';
import type { SiteSlotDto } from '@/shared/types/api';

const { getSiteSlotsMock, uploadMock, deleteMock } = vi.hoisted(() => ({
	getSiteSlotsMock: vi.fn<typeof getSiteSlots>(),
	uploadMock: vi.fn<typeof uploadSiteSlotImage>(),
	deleteMock: vi.fn<typeof deleteSiteSlotImage>(),
}));

// Podmieniamy TYLKO wywołania HTTP — hooki RQ i cała logika strony zostają prawdziwe.
vi.mock('../../api/siteSlotsApi', async (importOriginal) => {
	const actual =
		await importOriginal<typeof import('../../api/siteSlotsApi')>();
	return {
		...actual,
		getSiteSlots: getSiteSlotsMock,
		uploadSiteSlotImage: uploadMock,
		deleteSiteSlotImage: deleteMock,
	};
});

const allSlots: SiteSlotDto[] = [
	{ slot: 'HOME_HERO', configured: true, updatedAt: '2026-07-16T10:00:00Z' },
	{ slot: 'HOME_INTRO', configured: false, updatedAt: null },
	{ slot: 'HOME_CTA', configured: false, updatedAt: null },
	{ slot: 'ABOUT_BIO', configured: false, updatedAt: null },
	{ slot: 'ABOUT_EQUIPMENT', configured: false, updatedAt: null },
];

function renderPage() {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false } },
	});
	return render(
		<QueryClientProvider client={queryClient}>
			<AdminSitePage />
		</QueryClientProvider>,
	);
}

describe('AdminSitePage', () => {
	beforeEach(() => {
		getSiteSlotsMock.mockReset();
		uploadMock.mockReset();
		deleteMock.mockReset();
	});

	it('Every slot the backend defines is listed, including empty ones, so the admin sees every configurable section', async () => {
		// Given - one configured slot out of five
		getSiteSlotsMock.mockResolvedValue(allSlots);

		// When
		renderPage();

		// Then - all five sections are on screen; empty ones invite an upload, the filled one a swap
		expect(
			await screen.findByText('Strona główna — tło hero'),
		).toBeInTheDocument();
		expect(screen.getByText('O mnie — portret')).toBeInTheDocument();
		expect(screen.getAllByRole('button', { name: /Wgraj zdjęcie/ })).toHaveLength(4);
		expect(screen.getByRole('button', { name: /Podmień/ })).toBeInTheDocument();
	});

	it('The chosen file is uploaded to exactly the slot whose button was clicked, not to the first or last slot', async () => {
		// Given
		getSiteSlotsMock.mockResolvedValue(allSlots);
		uploadMock.mockResolvedValue(undefined);
		const user = userEvent.setup();
		renderPage();
		await screen.findByText('O mnie — portret');

		// When - the admin picks a file for ABOUT_BIO (a middle slot, not the first)
		await user.click(
			screen.getByRole('button', { name: 'Wgraj zdjęcie — O mnie — portret' }),
		);
		const file = new File(['x'], 'portret.jpg', { type: 'image/jpeg' });
		fireEvent.change(screen.getByLabelText('Plik zdjęcia sekcji'), {
			target: { files: [file] },
		});

		// Then
		await waitFor(() => {
			expect(uploadMock).toHaveBeenCalledWith('ABOUT_BIO', file);
		});
	});

	it('A slot freshly added in the backend still shows up (with its raw key), so a new section never requires a panel release', async () => {
		// Given - the backend knows a slot this frontend build has no label for
		getSiteSlotsMock.mockResolvedValue([
			...allSlots,
			{ slot: 'FOOTER_BANNER', configured: false, updatedAt: null },
		]);

		// When
		renderPage();

		// Then - rendered with the raw key instead of being silently dropped
		expect(await screen.findByText('FOOTER_BANNER')).toBeInTheDocument();
	});
});
