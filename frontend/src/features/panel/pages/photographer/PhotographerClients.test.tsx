import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PhotographerClients from './PhotographerClients';
import type {
	getAssignedAlbums,
	getAssignedClients,
	createClient,
} from '../../api/photographerApi';
import type { AlbumDto, FileDto } from '@/shared/types/api';
import type { UserInfo } from '../../types/panel';

const { albumsMock, clientsMock, createClientMock } = vi.hoisted(() => ({
	albumsMock: vi.fn<typeof getAssignedAlbums>(),
	clientsMock: vi.fn<typeof getAssignedClients>(),
	createClientMock: vi.fn<typeof createClient>(),
}));

vi.mock('../../api/photographerApi', async (importOriginal) => {
	const actual =
		await importOriginal<typeof import('../../api/photographerApi')>();
	return {
		...actual,
		getAssignedAlbums: albumsMock,
		getAssignedClients: clientsMock,
		createClient: createClientMock,
	};
});

function files(count: number): FileDto[] {
	return Array.from({ length: count }, (_, i) => ({
		fileId: `f${i}`,
		fileName: `foto${i}.jpg`,
		sizeBytes: 100,
		contentType: 'image/jpeg',
		uploadedAt: '2026-07-01T10:00:00Z',
		visible: true,
		hasWatermark: false,
	}));
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

function renderPage() {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
	});
	return render(
		<QueryClientProvider client={queryClient}>
			<MemoryRouter>
				<PhotographerClients />
			</MemoryRouter>
		</QueryClientProvider>,
	);
}

/** Karta klienta = kontener, w którym widnieje jego e-mail (nazwa może się powtarzać). */
function cardOf(email: string): HTMLElement {
	return screen.getByText(email).closest('div.bg-surface') as HTMLElement;
}

describe('PhotographerClients', () => {
	beforeEach(() => {
		albumsMock.mockReset().mockResolvedValue([]);
		clientsMock.mockReset().mockResolvedValue([]);
		createClientMock.mockReset();
	});

	it('Each client card counts only that client\'s albums and photos, so one busy client does not inflate another\'s numbers', async () => {
		// Given - Anna has two albums (3 + 2 photos), Piotr has one (1 photo)
		clientsMock.mockResolvedValue([
			client({ id: 'klient-1', name: 'Anna Kowalska', email: 'anna@example.com' }),
			client({ id: 'klient-2', name: 'Piotr Nowak', email: 'piotr@example.com' }),
		]);
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', clientId: 'klient-1', files: files(3) }),
			album({ albumId: 'a2', clientId: 'klient-1', files: files(2) }),
			album({ albumId: 'a3', clientId: 'klient-2', files: files(1) }),
		]);

		// When
		renderPage();
		await screen.findByText('anna@example.com');

		// Then - grouping by clientId is the whole point; a missing group key would show
		// every album under every client
		const anna = cardOf('anna@example.com');
		expect(within(anna).getByText('2')).toBeInTheDocument(); // albums
		expect(within(anna).getByText('5')).toBeInTheDocument(); // photos

		const piotr = cardOf('piotr@example.com');
		expect(within(piotr).getAllByText('1').length).toBeGreaterThan(0);
	});

	it('An album without a client is ignored, so a portfolio album never shows up under someone', async () => {
		// Given - an album with no owner (admin/portfolio album)
		clientsMock.mockResolvedValue([client({ id: 'klient-1' })]);
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', clientId: null, files: files(4) }),
		]);

		// When
		renderPage();
		await screen.findByText('anna@example.com');

		// Then
		expect(screen.getByText('Brak albumów')).toBeInTheDocument();
	});

	it('At most three albums are listed per client and the rest is summarised, so a long history does not push the card off screen', async () => {
		// Given - five albums for one client
		clientsMock.mockResolvedValue([client({ id: 'klient-1' })]);
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', clientId: 'klient-1', name: 'sesja-1' }),
			album({ albumId: 'a2', clientId: 'klient-1', name: 'sesja-2' }),
			album({ albumId: 'a3', clientId: 'klient-1', name: 'sesja-3' }),
			album({ albumId: 'a4', clientId: 'klient-1', name: 'sesja-4' }),
			album({ albumId: 'a5', clientId: 'klient-1', name: 'sesja-5' }),
		]);

		// When
		renderPage();
		await screen.findByText('sesja-1');

		// Then
		expect(screen.getByText('sesja-3')).toBeInTheDocument();
		expect(screen.queryByText('sesja-4')).not.toBeInTheDocument();
		expect(screen.getByText('+2 więcej...')).toBeInTheDocument();
	});

	it('Search matches the e-mail as well as the name, because a photographer often remembers only one of them', async () => {
		// Given
		clientsMock.mockResolvedValue([
			client({ id: 'klient-1', name: 'Anna Kowalska', email: 'anna@example.com' }),
			client({ id: 'klient-2', name: 'Piotr Nowak', email: 'piotr@firma.pl' }),
		]);
		renderPage();
		await screen.findByText('anna@example.com');
		const search = screen.getByPlaceholderText('Szukaj klienta...');

		// When - searching by a fragment of the e-mail only
		await userEvent.type(search, 'firma');

		// Then
		expect(screen.getByText('piotr@firma.pl')).toBeInTheDocument();
		expect(screen.queryByText('anna@example.com')).not.toBeInTheDocument();

		// When - and by a fragment of the name
		await userEvent.clear(search);
		await userEvent.type(search, 'kowal');

		// Then
		expect(screen.getByText('anna@example.com')).toBeInTheDocument();
		expect(screen.queryByText('piotr@firma.pl')).not.toBeInTheDocument();
	});

	it('Initials are taken from the name, so a card is recognisable before the photo loads', async () => {
		// Given
		clientsMock.mockResolvedValue([
			client({ id: 'klient-1', name: 'anna maria kowalska' }),
		]);

		// When
		renderPage();

		// Then - first letters of the first two words, upper-cased
		expect(await screen.findByText('AM')).toBeInTheDocument();
	});

	it('Creating a client needs both a name and an e-mail, because the start password is mailed to that address', async () => {
		// Given
		renderPage();
		await userEvent.click(
			(await screen.findAllByRole('button', { name: /Dodaj klienta/ }))[0],
		);
		const dialog = screen.getByRole('dialog');
		const submit = within(dialog).getByRole('button', { name: /Utwórz konto/ });

		// Then
		expect(submit).toBeDisabled();

		// When - only the name
		await userEvent.type(within(dialog).getByLabelText('Imię i Nazwisko'), 'Jan');
		expect(submit).toBeDisabled();

		// When - the e-mail as well
		await userEvent.type(
			within(dialog).getByLabelText('Email'),
			'jan@example.com',
		);

		// Then
		expect(submit).toBeEnabled();
	});

	it('A created client is sent with name and e-mail only, because the role and the password are decided server-side', async () => {
		// Given
		createClientMock.mockResolvedValue(client({ id: 'nowy-1' }));
		renderPage();
		await userEvent.click(
			(await screen.findAllByRole('button', { name: /Dodaj klienta/ }))[0],
		);
		const dialog = screen.getByRole('dialog');

		// When
		await userEvent.type(within(dialog).getByLabelText('Imię i Nazwisko'), 'Jan');
		await userEvent.type(
			within(dialog).getByLabelText('Email'),
			'jan@example.com',
		);
		await userEvent.click(
			within(dialog).getByRole('button', { name: /Utwórz konto/ }),
		);

		// Then - `mutationFn` jest przekazane bez opakowania, więc React Query dokłada
		// drugi argument (kontekst mutacji); sprawdzamy dane, nie liczbę argumentów
		await vi.waitFor(() => expect(createClientMock).toHaveBeenCalled());
		expect(createClientMock.mock.calls[0][0]).toEqual({
			name: 'Jan',
			email: 'jan@example.com',
		});
	});

	it('The form closes and empties itself after a success, so the next client does not inherit the previous name', async () => {
		// Given
		createClientMock.mockResolvedValue(client({ id: 'nowy-1' }));
		renderPage();
		await userEvent.click(
			(await screen.findAllByRole('button', { name: /Dodaj klienta/ }))[0],
		);
		const dialog = screen.getByRole('dialog');
		await userEvent.type(within(dialog).getByLabelText('Imię i Nazwisko'), 'Jan');
		await userEvent.type(
			within(dialog).getByLabelText('Email'),
			'jan@example.com',
		);

		// When
		await userEvent.click(
			within(dialog).getByRole('button', { name: /Utwórz konto/ }),
		);

		// Then - reopening shows empty fields
		await vi.waitFor(() =>
			expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
		);
		await userEvent.click(
			screen.getAllByRole('button', { name: /Dodaj klienta/ })[0],
		);
		expect(screen.getByLabelText('Imię i Nazwisko')).toHaveValue('');
		expect(screen.getByLabelText('Email')).toHaveValue('');
	});

	it('A photographer with no clients is offered to add one, instead of facing an empty screen', async () => {
		// Given
		clientsMock.mockResolvedValue([]);

		// When
		renderPage();

		// Then
		expect(await screen.findByText('Brak klientów')).toBeInTheDocument();
	});
});
