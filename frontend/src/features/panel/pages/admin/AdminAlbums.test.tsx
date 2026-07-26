import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AdminAlbums from './AdminAlbums';
import type { getAllAlbums, createAdminAlbum } from '../../api/adminApi';
import type { AlbumDto, FileDto } from '@/shared/types/api';

const { getAllAlbumsMock, createMock } = vi.hoisted(() => ({
	getAllAlbumsMock: vi.fn<typeof getAllAlbums>(),
	createMock: vi.fn<typeof createAdminAlbum>(),
}));

vi.mock('../../api/adminApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('../../api/adminApi')>();
	return { ...actual, getAllAlbums: getAllAlbumsMock, createAdminAlbum: createMock };
});

function file(overrides: Partial<FileDto> = {}): FileDto {
	return {
		fileId: 'f1',
		fileName: 'foto.jpg',
		sizeBytes: 100,
		contentType: 'image/jpeg',
		uploadedAt: '2026-07-01T10:00:00Z',
		visible: true,
		hasWatermark: false,
		...overrides,
	};
}

/** Album klienta: fotograf ≠ klient. */
function clientAlbum(overrides: Partial<AlbumDto>): AlbumDto {
	return {
		albumId: 'c1',
		name: 'sesja-klienta',
		photographId: 'foto-1',
		clientId: 'klient-1',
		ttd: null,
		files: [],
		isPublic: false,
		displayName: null,
		displayOrder: 0,
		...overrides,
	};
}

/** Album portfolio: ta sama tożsamość w obu polach (isAdminAlbum). */
function portfolioAlbum(overrides: Partial<AlbumDto>): AlbumDto {
	return {
		...clientAlbum({}),
		albumId: 'a1',
		name: 'portfolio-sluby',
		photographId: 'admin-1',
		clientId: 'admin-1',
		...overrides,
	};
}

const YESTERDAY = new Date(Date.now() - 86_400_000).toISOString();
const NEXT_YEAR = new Date(Date.now() + 365 * 86_400_000).toISOString();

function renderPage() {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
	});
	return render(
		<QueryClientProvider client={queryClient}>
			<MemoryRouter>
				<AdminAlbums />
			</MemoryRouter>
		</QueryClientProvider>,
	);
}

function cardOf(name: string): HTMLElement {
	return screen.getByText(name).closest('a') as HTMLElement;
}

describe('AdminAlbums', () => {
	beforeEach(() => {
		getAllAlbumsMock.mockReset().mockResolvedValue([]);
		createMock.mockReset();
	});

	it('An album counts as a portfolio album when photographer and client are the same identity, which is the rule the backend uses', async () => {
		// Given
		getAllAlbumsMock.mockResolvedValue([
			portfolioAlbum({ albumId: 'a1', name: 'portfolio-sluby' }),
			clientAlbum({ albumId: 'c1', name: 'sesja-klienta' }),
		]);

		// When
		renderPage();
		await screen.findByText('portfolio-sluby');

		// Then - the badge follows that comparison, not the album's name
		expect(within(cardOf('portfolio-sluby')).getByText('Admin')).toBeInTheDocument();
		expect(within(cardOf('sesja-klienta')).getByText('Klient')).toBeInTheDocument();
	});

	it('The type filter separates portfolio albums from client albums', async () => {
		// Given
		getAllAlbumsMock.mockResolvedValue([
			portfolioAlbum({ albumId: 'a1', name: 'portfolio-sluby' }),
			clientAlbum({ albumId: 'c1', name: 'sesja-klienta' }),
		]);
		renderPage();
		await screen.findByText('portfolio-sluby');
		const typeSelect = screen.getAllByRole('combobox')[0];

		// When / Then
		await userEvent.selectOptions(typeSelect, 'ADMIN');
		expect(screen.getByText('portfolio-sluby')).toBeInTheDocument();
		expect(screen.queryByText('sesja-klienta')).not.toBeInTheDocument();

		await userEvent.selectOptions(typeSelect, 'CLIENT');
		expect(screen.getByText('sesja-klienta')).toBeInTheDocument();
		expect(screen.queryByText('portfolio-sluby')).not.toBeInTheDocument();
	});

	it('A missing TTD is flagged on client albums only, because a portfolio album is not allowed to have one at all (B.40)', async () => {
		// Given - neither album has a TTD
		getAllAlbumsMock.mockResolvedValue([
			portfolioAlbum({ albumId: 'a1', name: 'portfolio-sluby', ttd: null }),
			clientAlbum({ albumId: 'c1', name: 'sesja-klienta', ttd: null }),
		]);

		// When
		renderPage();
		await screen.findByText('portfolio-sluby');

		// Then - warning the admin about a portfolio album would demand something the
		// backend rejects with a 400
		expect(within(cardOf('sesja-klienta')).getByText('Brak TTD')).toBeInTheDocument();
		expect(
			within(cardOf('portfolio-sluby')).queryByText('Brak TTD'),
		).not.toBeInTheDocument();
	});

	it('Total and visible photo counts are shown separately, so the admin sees how much is still curated', async () => {
		// Given - three photos, one hidden
		getAllAlbumsMock.mockResolvedValue([
			clientAlbum({
				name: 'sesja-klienta',
				files: [
					file({ fileId: 'f1', visible: true }),
					file({ fileId: 'f2', visible: true }),
					file({ fileId: 'f3', visible: false }),
				],
			}),
		]);

		// When
		renderPage();
		await screen.findByText('sesja-klienta');

		// Then
		const card = cardOf('sesja-klienta');
		expect(within(card).getByText('3 zdjęć')).toBeInTheDocument();
		expect(within(card).getByText('2 widoczne')).toBeInTheDocument();
	});

	it('A published album is marked as public, because that is the one state visible to the whole internet', async () => {
		// Given
		getAllAlbumsMock.mockResolvedValue([
			portfolioAlbum({ albumId: 'a1', name: 'publiczny', isPublic: true }),
			portfolioAlbum({ albumId: 'a2', name: 'prywatny', isPublic: false }),
		]);

		// When
		renderPage();
		await screen.findByText('publiczny');

		// Then
		expect(within(cardOf('publiczny')).getByText('Publiczny')).toBeInTheDocument();
		expect(within(cardOf('prywatny')).queryByText('Publiczny')).not.toBeInTheDocument();
	});

	it('An expired album is marked as expired instead of showing its past date as a deadline', async () => {
		// Given
		getAllAlbumsMock.mockResolvedValue([
			clientAlbum({ albumId: 'c1', name: 'wygasly', ttd: YESTERDAY }),
			clientAlbum({ albumId: 'c2', name: 'aktualny', ttd: NEXT_YEAR }),
		]);

		// When
		renderPage();
		await screen.findByText('wygasly');

		// Then
		expect(within(cardOf('wygasly')).getByText('WYGASŁ')).toBeInTheDocument();
		expect(within(cardOf('aktualny')).getByText(/Do:/)).toBeInTheDocument();
	});

	it('The expired filter keeps only albums past their date, not every album that has one', async () => {
		// Given
		getAllAlbumsMock.mockResolvedValue([
			clientAlbum({ albumId: 'c1', name: 'wygasly', ttd: YESTERDAY }),
			clientAlbum({ albumId: 'c2', name: 'aktualny', ttd: NEXT_YEAR }),
		]);
		renderPage();
		await screen.findByText('wygasly');

		// When
		await userEvent.selectOptions(screen.getAllByRole('combobox')[1], 'EXPIRED');

		// Then
		expect(screen.getByText('wygasly')).toBeInTheDocument();
		expect(screen.queryByText('aktualny')).not.toBeInTheDocument();
	});

	it('The counter describes the filtered list, so it agrees with the cards on screen', async () => {
		// Given
		getAllAlbumsMock.mockResolvedValue([
			portfolioAlbum({ albumId: 'a1', name: 'portfolio-sluby' }),
			clientAlbum({ albumId: 'c1', name: 'sesja-klienta' }),
		]);
		renderPage();
		expect(await screen.findByText('2 albumów')).toBeInTheDocument();

		// When
		await userEvent.selectOptions(screen.getAllByRole('combobox')[0], 'ADMIN');

		// Then
		expect(screen.getByText('1 albumów')).toBeInTheDocument();
	});

	it('Search narrows albums by name', async () => {
		// Given
		getAllAlbumsMock.mockResolvedValue([
			clientAlbum({ albumId: 'c1', name: 'slub-czerwiec' }),
			clientAlbum({ albumId: 'c2', name: 'chrzciny-maj' }),
		]);
		renderPage();
		await screen.findByText('slub-czerwiec');

		// When
		await userEvent.type(screen.getByPlaceholderText('Szukaj albumu...'), 'chrzc');

		// Then
		expect(screen.getByText('chrzciny-maj')).toBeInTheDocument();
		expect(screen.queryByText('slub-czerwiec')).not.toBeInTheDocument();
	});

	it('A whitespace-only name cannot create an album, because the name becomes a directory path on disk', async () => {
		// Given
		renderPage();
		await userEvent.click(await screen.findByRole('button', { name: /Nowy album/ }));
		const dialog = screen.getByRole('dialog');

		// When
		await userEvent.type(within(dialog).getByLabelText('Nazwa albumu'), '   ');

		// Then
		expect(
			within(dialog).getByRole('button', { name: /Utwórz album/ }),
		).toBeDisabled();
	});

	it('Creating an album sends the typed name and closes the form afterwards', async () => {
		// Given
		createMock.mockResolvedValue(portfolioAlbum({ albumId: 'nowy' }));
		renderPage();
		await userEvent.click(await screen.findByRole('button', { name: /Nowy album/ }));
		const dialog = screen.getByRole('dialog');

		// When
		await userEvent.type(
			within(dialog).getByLabelText('Nazwa albumu'),
			'portfolio-plenery',
		);
		await userEvent.click(
			within(dialog).getByRole('button', { name: /Utwórz album/ }),
		);

		// Then
		await vi.waitFor(() =>
			expect(createMock).toHaveBeenCalledWith('portfolio-plenery'),
		);
		await vi.waitFor(() =>
			expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
		);
	});

	it('A filter matching nothing explains itself rather than showing an empty grid', async () => {
		// Given
		getAllAlbumsMock.mockResolvedValue([
			clientAlbum({ albumId: 'c1', name: 'sesja-klienta' }),
		]);
		renderPage();
		await screen.findByText('sesja-klienta');

		// When
		await userEvent.selectOptions(screen.getAllByRole('combobox')[0], 'ADMIN');

		// Then
		expect(screen.getByText('Brak albumów')).toBeInTheDocument();
	});
});
