import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PhotographerAlbums from './PhotographerAlbums';
import type {
	getAssignedAlbums,
	getAssignedClients,
	createClientAlbum,
	setAlbumTtd,
} from '../../api/photographerApi';
import type { AlbumDto, FileDto } from '@/shared/types/api';
import type { UserInfo } from '../../types/panel';

const { albumsMock, clientsMock, createAlbumMock, setTtdMock } = vi.hoisted(
	() => ({
		albumsMock: vi.fn<typeof getAssignedAlbums>(),
		clientsMock: vi.fn<typeof getAssignedClients>(),
		createAlbumMock: vi.fn<typeof createClientAlbum>(),
		setTtdMock: vi.fn<typeof setAlbumTtd>(),
	}),
);

// Podmieniamy TYLKO wywołania HTTP — hooki RQ, filtry i logika strony zostają prawdziwe.
vi.mock('../../api/photographerApi', async (importOriginal) => {
	const actual =
		await importOriginal<typeof import('../../api/photographerApi')>();
	return {
		...actual,
		getAssignedAlbums: albumsMock,
		getAssignedClients: clientsMock,
		createClientAlbum: createAlbumMock,
		setAlbumTtd: setTtdMock,
	};
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

function album(overrides: Partial<AlbumDto>): AlbumDto {
	return {
		albumId: 'id',
		name: 'sesja',
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

function client(overrides: Partial<UserInfo>): UserInfo {
	return {
		id: 'klient-1',
		name: 'Anna Kowalska',
		email: 'anna@example.com',
		roles: ['CLIENT'],
		isActive: true,
		changePasswordOnNextLogin: false,
		assignedUsers: [],
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
				<PhotographerAlbums />
			</MemoryRouter>
		</QueryClientProvider>,
	);
}

describe('PhotographerAlbums', () => {
	beforeEach(() => {
		albumsMock.mockReset().mockResolvedValue([]);
		clientsMock.mockReset().mockResolvedValue([]);
		createAlbumMock.mockReset();
		setTtdMock.mockReset().mockResolvedValue(undefined);
	});

	it('An album without a TTD is flagged, because a client album that never expires is the state the photographer must notice', async () => {
		// Given - one album with a future TTD and one with none
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'z-data', ttd: NEXT_YEAR }),
			album({ albumId: 'a2', name: 'bez-daty', ttd: null }),
		]);

		// When
		renderPage();

		// Then - the warning belongs to the album that has no TTD, not to both
		const withoutTtd = await screen.findByText('bez-daty');
		const card = withoutTtd.closest('a') as HTMLElement;
		expect(within(card).getByText('Brak TTD')).toBeInTheDocument();

		const withTtd = screen.getByText('z-data').closest('a') as HTMLElement;
		expect(within(withTtd).queryByText('Brak TTD')).not.toBeInTheDocument();
	});

	it('A past TTD marks the album as expired, so the photographer sees the client lost access', async () => {
		// Given
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'wygasly', ttd: YESTERDAY }),
		]);

		// When
		renderPage();

		// Then
		expect(await screen.findByText('Wygasł')).toBeInTheDocument();
	});

	it('The expired filter shows only albums whose date has passed, not every album that has a date', async () => {
		// Given
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'wygasly', ttd: YESTERDAY }),
			album({ albumId: 'a2', name: 'aktualny', ttd: NEXT_YEAR }),
			album({ albumId: 'a3', name: 'bez-daty', ttd: null }),
		]);
		renderPage();
		await screen.findByText('wygasly');

		// When - the second select is the TTD filter (the first one filters by client)
		await userEvent.selectOptions(screen.getAllByRole('combobox')[1], 'EXPIRED');

		// Then
		expect(screen.getByText('wygasly')).toBeInTheDocument();
		expect(screen.queryByText('aktualny')).not.toBeInTheDocument();
		expect(screen.queryByText('bez-daty')).not.toBeInTheDocument();
	});

	it('The "without a date" filter is the complement of "with a date", so no album falls outside both', async () => {
		// Given
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'z-data', ttd: NEXT_YEAR }),
			album({ albumId: 'a2', name: 'bez-daty', ttd: null }),
		]);
		renderPage();
		await screen.findByText('z-data');
		const ttdSelect = screen.getAllByRole('combobox')[1];

		// When / Then - "with a date" keeps only the dated one...
		await userEvent.selectOptions(ttdSelect, 'WITH');
		expect(screen.getByText('z-data')).toBeInTheDocument();
		expect(screen.queryByText('bez-daty')).not.toBeInTheDocument();

		// ...and "without a date" keeps exactly the other one
		await userEvent.selectOptions(ttdSelect, 'WITHOUT');
		expect(screen.getByText('bez-daty')).toBeInTheDocument();
		expect(screen.queryByText('z-data')).not.toBeInTheDocument();
	});

	it('Filtering by client uses the client id, so two clients with similar album names never mix', async () => {
		// Given - both albums share a name fragment but belong to different clients
		clientsMock.mockResolvedValue([
			client({ id: 'klient-1', name: 'Anna Kowalska' }),
			client({ id: 'klient-2', name: 'Piotr Nowak' }),
		]);
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'sesja-anna', clientId: 'klient-1' }),
			album({ albumId: 'a2', name: 'sesja-piotr', clientId: 'klient-2' }),
		]);
		renderPage();
		await screen.findByText('sesja-anna');

		// When
		await userEvent.selectOptions(screen.getAllByRole('combobox')[0], 'klient-2');

		// Then
		expect(screen.getByText('sesja-piotr')).toBeInTheDocument();
		expect(screen.queryByText('sesja-anna')).not.toBeInTheDocument();
	});

	it("Each card shows its own client's name, so the photographer can tell whose session it is", async () => {
		// Given
		clientsMock.mockResolvedValue([
			client({ id: 'klient-1', name: 'Anna Kowalska' }),
			client({ id: 'klient-2', name: 'Piotr Nowak' }),
		]);
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'sesja-anna', clientId: 'klient-1' }),
			album({ albumId: 'a2', name: 'sesja-piotr', clientId: 'klient-2' }),
		]);

		// When
		renderPage();

		// Then
		const annaCard = (await screen.findByText('sesja-anna')).closest(
			'a',
		) as HTMLElement;
		expect(within(annaCard).getByText('Anna Kowalska')).toBeInTheDocument();
		expect(within(annaCard).queryByText('Piotr Nowak')).not.toBeInTheDocument();
	});

	it('Search matches the album name, so a long client list can still be narrowed by typing', async () => {
		// Given
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'slub-czerwiec' }),
			album({ albumId: 'a2', name: 'chrzciny-maj' }),
		]);
		renderPage();
		await screen.findByText('slub-czerwiec');

		// When
		await userEvent.type(screen.getByPlaceholderText('Szukaj albumu...'), 'chrzc');

		// Then
		expect(screen.getByText('chrzciny-maj')).toBeInTheDocument();
		expect(screen.queryByText('slub-czerwiec')).not.toBeInTheDocument();
	});

	it('The album cover is the first photo of the album, so an empty album shows a placeholder instead of a broken image', async () => {
		// Given
		albumsMock.mockResolvedValue([
			album({
				albumId: 'a1',
				name: 'z-okladka',
				files: [file({ fileName: 'pierwsze.jpg' })],
			}),
			album({ albumId: 'a2', name: 'pusty', files: [] }),
		]);

		// When
		renderPage();

		// Then - exactly one <img>, and it points at the first file of the non-empty album
		const cover = await screen.findByRole('img', { name: 'z-okladka' });
		expect(cover).toHaveAttribute(
			'src',
			expect.stringContaining('pierwsze.jpg'),
		);
		expect(screen.queryByRole('img', { name: 'pusty' })).not.toBeInTheDocument();
	});

	it('Creating an album is blocked until both a client and a name are given, because an album cannot exist without an owner', async () => {
		// Given
		clientsMock.mockResolvedValue([client({ id: 'klient-1' })]);
		renderPage();
		// Pusta lista renderuje DRUGI przycisk "Nowy album" (EmptyState) - bierzemy ten z naglowka
		await userEvent.click(
			(await screen.findAllByRole('button', { name: /Nowy album/ }))[0],
		);
		const dialog = screen.getByRole('dialog');
		const submit = within(dialog).getByRole('button', { name: /Utwórz album/ });

		// Then - disabled with nothing filled in
		expect(submit).toBeDisabled();

		// When - only a name is typed, still no client
		await userEvent.type(within(dialog).getByLabelText('Nazwa albumu'), 'sesja');
		expect(submit).toBeDisabled();

		// When - the client is picked as well
		await userEvent.selectOptions(
			within(dialog).getByRole('combobox'),
			'klient-1',
		);

		// Then
		expect(submit).toBeEnabled();
	});

	it('A whitespace-only name does not count as a name, so an album named " " cannot be created', async () => {
		// Given
		clientsMock.mockResolvedValue([client({ id: 'klient-1' })]);
		renderPage();
		// Pusta lista renderuje DRUGI przycisk "Nowy album" (EmptyState) - bierzemy ten z naglowka
		await userEvent.click(
			(await screen.findAllByRole('button', { name: /Nowy album/ }))[0],
		);
		const dialog = screen.getByRole('dialog');

		// When
		await userEvent.selectOptions(
			within(dialog).getByRole('combobox'),
			'klient-1',
		);
		await userEvent.type(within(dialog).getByLabelText('Nazwa albumu'), '   ');

		// Then
		expect(
			within(dialog).getByRole('button', { name: /Utwórz album/ }),
		).toBeDisabled();
	});

	it('A TTD given while creating is applied as a separate call after the album exists, because creation does not accept a TTD', async () => {
		// Given
		clientsMock.mockResolvedValue([client({ id: 'klient-1' })]);
		createAlbumMock.mockResolvedValue(album({ albumId: 'nowy-1', name: 'sesja' }));
		renderPage();
		// Pusta lista renderuje DRUGI przycisk "Nowy album" (EmptyState) - bierzemy ten z naglowka
		await userEvent.click(
			(await screen.findAllByRole('button', { name: /Nowy album/ }))[0],
		);
		const dialog = screen.getByRole('dialog');

		// When
		await userEvent.selectOptions(
			within(dialog).getByRole('combobox'),
			'klient-1',
		);
		await userEvent.type(within(dialog).getByLabelText('Nazwa albumu'), 'sesja');
		await userEvent.type(
			within(dialog).getByLabelText(/Data wygaśnięcia/),
			'2026-12-01',
		);
		await userEvent.click(
			within(dialog).getByRole('button', { name: /Utwórz album/ }),
		);

		// Then - the TTD must target the id RETURNED by creation; sending it with the
		// create call would be silently dropped and the album would never expire
		await vi.waitFor(() =>
			expect(setTtdMock).toHaveBeenCalledWith('nowy-1', '2026-12-01'),
		);
		expect(createAlbumMock).toHaveBeenCalledWith('klient-1', 'sesja');
	});

	it('Creating without a date makes no TTD call, so an album stays deliberately without an expiry', async () => {
		// Given
		clientsMock.mockResolvedValue([client({ id: 'klient-1' })]);
		createAlbumMock.mockResolvedValue(album({ albumId: 'nowy-1' }));
		renderPage();
		// Pusta lista renderuje DRUGI przycisk "Nowy album" (EmptyState) - bierzemy ten z naglowka
		await userEvent.click(
			(await screen.findAllByRole('button', { name: /Nowy album/ }))[0],
		);
		const dialog = screen.getByRole('dialog');

		// When
		await userEvent.selectOptions(
			within(dialog).getByRole('combobox'),
			'klient-1',
		);
		await userEvent.type(within(dialog).getByLabelText('Nazwa albumu'), 'sesja');
		await userEvent.click(
			within(dialog).getByRole('button', { name: /Utwórz album/ }),
		);

		// Then
		await vi.waitFor(() => expect(createAlbumMock).toHaveBeenCalled());
		expect(setTtdMock).not.toHaveBeenCalled();
	});

	it('An empty album list offers creating one, so a new photographer is not left on a dead end', async () => {
		// Given - no albums at all
		albumsMock.mockResolvedValue([]);

		// When
		renderPage();

		// Then
		expect(await screen.findByText('Brak albumów')).toBeInTheDocument();
		expect(
			screen.getAllByRole('button', { name: /Nowy album/ }).length,
		).toBeGreaterThan(1);
	});
});
