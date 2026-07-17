import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AdminPublicAlbums from './AdminPublicAlbums';
import type {
	getAllAlbums,
	setAlbumDisplay,
	setAlbumPublic,
	createAdminAlbum,
} from '../../api/adminApi';
import type { AlbumDto } from '@/shared/types/api';

const { getAllAlbumsMock, setDisplayMock, setPublicMock, createMock } =
	vi.hoisted(() => ({
		getAllAlbumsMock: vi.fn<typeof getAllAlbums>(),
		setDisplayMock: vi.fn<typeof setAlbumDisplay>(),
		setPublicMock: vi.fn<typeof setAlbumPublic>(),
		createMock: vi.fn<typeof createAdminAlbum>(),
	}));

// Podmieniamy TYLKO wywołania HTTP — hooki RQ i logika strony zostają prawdziwe.
vi.mock('../../api/adminApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('../../api/adminApi')>();
	return {
		...actual,
		getAllAlbums: getAllAlbumsMock,
		setAlbumDisplay: setDisplayMock,
		setAlbumPublic: setPublicMock,
		createAdminAlbum: createMock,
	};
});

function album(overrides: Partial<AlbumDto>): AlbumDto {
	return {
		albumId: 'id',
		name: 'name',
		photographId: 'admin-1',
		clientId: 'admin-1',
		ttd: null,
		files: [],
		isPublic: true,
		displayName: null,
		displayOrder: 0,
		...overrides,
	};
}

function renderPage() {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false } },
	});
	return render(
		<QueryClientProvider client={queryClient}>
			<AdminPublicAlbums />
		</QueryClientProvider>,
	);
}

describe('AdminPublicAlbums', () => {
	beforeEach(() => {
		getAllAlbumsMock.mockReset();
		setDisplayMock.mockReset();
		setPublicMock.mockReset();
		createMock.mockReset();
		setDisplayMock.mockResolvedValue(undefined);
	});

	it('Only admin (portfolio) albums are listed, because a client album can never become a public tab', async () => {
		// Given - one portfolio album and one client album
		getAllAlbumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'sluby' }),
			album({
				albumId: 'c1',
				name: 'Sesja_klient@x.pl_2026-07-16',
				clientId: 'client-9',
			}),
		]);

		// When
		renderPage();

		// Then
		expect(await screen.findByText('sluby')).toBeInTheDocument();
		expect(screen.queryByText(/Sesja_klient/)).not.toBeInTheDocument();
	});

	it('Saving tab settings sends the Unicode label and order to exactly the edited album', async () => {
		// Given
		getAllAlbumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'sluby' }),
			album({ albumId: 'a2', name: 'plener' }),
		]);
		const user = userEvent.setup();
		renderPage();
		await screen.findByText('plener');

		// When - the admin edits the SECOND album's tab
		await user.click(
			screen.getByRole('button', { name: 'Ustawienia zakładki — plener' }),
		);
		await user.type(screen.getByLabelText('Etykieta zakładki'), 'Sesje plenerowe');
		const orderInput = screen.getByLabelText('Kolejność (mniejsza = wcześniej)');
		await user.clear(orderInput);
		await user.type(orderInput, '3');
		await user.click(screen.getByRole('button', { name: 'Zapisz' }));

		// Then
		expect(setDisplayMock).toHaveBeenCalledWith('a2', 'Sesje plenerowe', 3);
	});

	it('A technical name with Polish characters blocks creation with a Polish hint, so the admin never sees the raw backend 400', async () => {
		// Given
		getAllAlbumsMock.mockResolvedValue([]);
		const user = userEvent.setup();
		renderPage();
		await screen.findByRole('button', { name: /Nowy album/ });

		// When - the admin tries the natural thing: a Polish album name
		await user.click(screen.getByRole('button', { name: /Nowy album/ }));
		await user.type(
			screen.getByLabelText('Nazwa techniczna (bez polskich znaków)'),
			'Śluby',
		);

		// Then - a Polish explanation appears and the submit stays locked
		expect(
			screen.getByText(/nie może zawierać polskich znaków/),
		).toBeInTheDocument();
		expect(screen.getByRole('button', { name: /Utwórz album/ })).toBeDisabled();
		expect(createMock).not.toHaveBeenCalled();
	});

	it('An ASCII technical name passes the mirror of the backend rule and creation goes through', async () => {
		// Given
		getAllAlbumsMock.mockResolvedValue([]);
		createMock.mockResolvedValue(album({ albumId: 'new-1', name: 'sluby-2026' }));
		setPublicMock.mockResolvedValue(undefined);
		const user = userEvent.setup();
		renderPage();
		await screen.findByRole('button', { name: /Nowy album/ });

		// When
		await user.click(screen.getByRole('button', { name: /Nowy album/ }));
		await user.type(
			screen.getByLabelText('Nazwa techniczna (bez polskich znaków)'),
			'sluby-2026',
		);
		await user.click(screen.getByRole('button', { name: /Utwórz album/ }));

		// Then
		expect(createMock).toHaveBeenCalledWith('sluby-2026');
	});

	it('A cleared label is saved as null, so the tab falls back to the technical name instead of an empty string', async () => {
		// Given - an album that already has a label
		getAllAlbumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'sluby', displayName: 'Śluby' }),
		]);
		const user = userEvent.setup();
		renderPage();
		await screen.findByText('Śluby');

		// When - the admin clears the label and saves
		await user.click(
			screen.getByRole('button', { name: 'Ustawienia zakładki — Śluby' }),
		);
		await user.clear(screen.getByLabelText('Etykieta zakładki'));
		await user.click(screen.getByRole('button', { name: 'Zapisz' }));

		// Then
		expect(setDisplayMock).toHaveBeenCalledWith('a1', null, 0);
	});
});
