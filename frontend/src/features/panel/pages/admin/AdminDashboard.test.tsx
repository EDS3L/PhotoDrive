import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AdminDashboard from './AdminDashboard';
import type {
	getAllUsers,
	getAllAlbums,
	getAllAlbumsWithoutTtd,
} from '../../api/adminApi';
import type { AlbumDto, FileDto } from '@/shared/types/api';
import type { UserInfo } from '../../types/panel';

const { usersMock, albumsMock, noTtdMock } = vi.hoisted(() => ({
	usersMock: vi.fn<typeof getAllUsers>(),
	albumsMock: vi.fn<typeof getAllAlbums>(),
	noTtdMock: vi.fn<typeof getAllAlbumsWithoutTtd>(),
}));

vi.mock('../../api/adminApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('../../api/adminApi')>();
	return {
		...actual,
		getAllUsers: usersMock,
		getAllAlbums: albumsMock,
		getAllAlbumsWithoutTtd: noTtdMock,
	};
});

function file(uploadedAt: string): FileDto {
	return {
		fileId: `f-${uploadedAt}`,
		fileName: 'foto.jpg',
		sizeBytes: 100,
		contentType: 'image/jpeg',
		uploadedAt,
		visible: true,
		hasWatermark: false,
	};
}

function album(overrides: Partial<AlbumDto>): AlbumDto {
	return {
		albumId: 'a1',
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

function user(overrides: Partial<UserInfo>): UserInfo {
	return {
		id: 'u1',
		name: 'Jan Kowalski',
		email: 'jan@example.com',
		roles: ['CLIENT'],
		isActive: true,
		changePasswordOnNextLogin: false,
		assignedUsers: [],
		...overrides,
	};
}

function renderPage() {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false } },
	});
	return render(
		<QueryClientProvider client={queryClient}>
			<MemoryRouter>
				<AdminDashboard />
			</MemoryRouter>
		</QueryClientProvider>,
	);
}

/** Kafelek statystyki = kontener, w którym stoi jego etykieta. */
function statCard(label: string): HTMLElement {
	return screen.getByText(label).closest('div') as HTMLElement;
}

describe('AdminDashboard', () => {
	beforeEach(() => {
		usersMock.mockReset().mockResolvedValue([]);
		albumsMock.mockReset().mockResolvedValue([]);
		noTtdMock.mockReset().mockResolvedValue([]);
	});

	it('Active users are counted separately from all users, so deactivating somebody is visible at a glance', async () => {
		// Given - three accounts, one deactivated
		usersMock.mockResolvedValue([
			user({ id: 'u1', isActive: true }),
			user({ id: 'u2', isActive: true }),
			user({ id: 'u3', isActive: false }),
		]);

		// When
		renderPage();
		await screen.findByText('Dashboard');

		// Then
		expect(within(statCard('Użytkownicy')).getByText('3')).toBeInTheDocument();
		expect(
			within(statCard('Aktywni użytkownicy')).getByText('2'),
		).toBeInTheDocument();
	});

	it('The photo counter sums photos over all albums, so it is not the number of albums', async () => {
		// Given
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', files: [file('2026-07-01T10:00:00Z')] }),
			album({
				albumId: 'a2',
				files: [file('2026-07-02T10:00:00Z'), file('2026-07-03T10:00:00Z')],
			}),
		]);

		// When
		renderPage();
		await screen.findByText('Dashboard');

		// Then
		expect(within(statCard('Albumy')).getByText('2')).toBeInTheDocument();
		expect(within(statCard('Zdjęcia')).getByText('3')).toBeInTheDocument();
	});

	it('The TTD warning is worded for one album and for several, so the sentence is correct Polish either way', async () => {
		// Given
		noTtdMock.mockResolvedValue([album({ albumId: 'a1' })]);

		// When
		const { unmount } = renderPage();

		// Then
		expect(await screen.findByText(/1 album nie ma/)).toBeInTheDocument();
		unmount();

		// Given
		noTtdMock.mockResolvedValue([
			album({ albumId: 'a1' }),
			album({ albumId: 'a2' }),
		]);

		// When
		renderPage();

		// Then
		expect(await screen.findByText(/2 albumów nie ma/)).toBeInTheDocument();
	});

	it('No TTD warning is shown when every album has one, so the dashboard stays quiet', async () => {
		// Given
		noTtdMock.mockResolvedValue([]);

		// When
		renderPage();
		await screen.findByText('Dashboard');

		// Then
		expect(screen.queryByText(/ustawionego czasu wygaśnięcia/)).not.toBeInTheDocument();
	});

	it('Activity lists albums by their newest upload, so the most recently worked-on album comes first', async () => {
		// Given - the first album in the response holds the older photo
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'stary', files: [file('2026-01-01T10:00:00Z')] }),
			album({ albumId: 'a2', name: 'nowy', files: [file('2026-07-20T10:00:00Z')] }),
		]);

		// When
		renderPage();
		await screen.findByText('Ostatnia aktywność');

		// Then
		const items = screen.getAllByRole('listitem').map((li) => li.textContent);
		expect(items[0]).toContain('nowy');
		expect(items[1]).toContain('stary');
	});

	it('An album with no photos produces no activity entry, because nothing has happened in it', async () => {
		// Given
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'pusty', files: [] }),
			album({ albumId: 'a2', name: 'z-fotkami', files: [file('2026-07-20T10:00:00Z')] }),
		]);

		// When
		renderPage();
		await screen.findByText('Ostatnia aktywność');

		// Then - an empty album would otherwise be sorted with Math.max of an empty list
		const items = screen.getAllByRole('listitem').map((li) => li.textContent);
		expect(items).toHaveLength(1);
		expect(items[0]).toContain('z-fotkami');
	});

	it('Activity is capped at five entries, so the dashboard stays a summary', async () => {
		// Given - seven albums, each with a photo
		albumsMock.mockResolvedValue(
			Array.from({ length: 7 }, (_, i) =>
				album({
					albumId: `a${i}`,
					name: `sesja-${i}`,
					files: [file(`2026-07-0${i + 1}T10:00:00Z`)],
				}),
			),
		);

		// When
		renderPage();
		await screen.findByText('Ostatnia aktywność');

		// Then
		expect(screen.getAllByRole('listitem')).toHaveLength(5);
	});

	it('An empty system says there is no activity instead of showing an empty list', async () => {
		// Given
		albumsMock.mockResolvedValue([]);

		// When
		renderPage();

		// Then
		expect(await screen.findByText('Brak ostatniej aktywności')).toBeInTheDocument();
	});
});
