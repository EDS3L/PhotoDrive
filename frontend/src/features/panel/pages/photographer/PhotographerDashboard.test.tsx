import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PhotographerDashboard from './PhotographerDashboard';
import type {
	getAssignedAlbums,
	getAssignedAlbumsWithoutTtd,
	getAssignedClients,
} from '../../api/photographerApi';
import type { AlbumDto, FileDto } from '@/shared/types/api';
import type { UserInfo } from '../../types/panel';

const { albumsMock, noTtdMock, clientsMock } = vi.hoisted(() => ({
	albumsMock: vi.fn<typeof getAssignedAlbums>(),
	noTtdMock: vi.fn<typeof getAssignedAlbumsWithoutTtd>(),
	clientsMock: vi.fn<typeof getAssignedClients>(),
}));

vi.mock('../../api/photographerApi', async (importOriginal) => {
	const actual =
		await importOriginal<typeof import('../../api/photographerApi')>();
	return {
		...actual,
		getAssignedAlbums: albumsMock,
		getAssignedAlbumsWithoutTtd: noTtdMock,
		getAssignedClients: clientsMock,
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

function client(id: string): UserInfo {
	return {
		id,
		name: 'Klient',
		email: `${id}@example.com`,
		roles: ['CLIENT'],
		isActive: true,
		changePasswordOnNextLogin: false,
		assignedUsers: [],
	};
}

/** Data przesunięta o `days` od teraz — TTD liczone jest względem chwili uruchomienia. */
function inDays(days: number): string {
	return new Date(Date.now() + days * 86_400_000).toISOString();
}

function renderPage() {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false } },
	});
	return render(
		<QueryClientProvider client={queryClient}>
			<MemoryRouter>
				<PhotographerDashboard />
			</MemoryRouter>
		</QueryClientProvider>,
	);
}

describe('PhotographerDashboard', () => {
	beforeEach(() => {
		albumsMock.mockReset().mockResolvedValue([]);
		noTtdMock.mockReset().mockResolvedValue([]);
		clientsMock.mockReset().mockResolvedValue([]);
	});

	it('The photo counter sums photos across albums, so it is not the number of albums', async () => {
		// Given - two albums with 2 and 3 photos
		clientsMock.mockResolvedValue([client('klient-1')]);
		albumsMock.mockResolvedValue([
			album({
				albumId: 'a1',
				files: [file('2026-07-01T10:00:00Z'), file('2026-07-02T10:00:00Z')],
			}),
			album({
				albumId: 'a2',
				files: [
					file('2026-07-03T10:00:00Z'),
					file('2026-07-04T10:00:00Z'),
					file('2026-07-05T10:00:00Z'),
				],
			}),
		]);

		// When
		renderPage();

		// Then
		const photos = (await screen.findByText('Zdjęcia')).closest(
			'div',
		) as HTMLElement;
		expect(within(photos).getByText('5')).toBeInTheDocument();
	});

	it('An album whose expiry is far away raises no alert, so the list holds only what needs attention', async () => {
		// Given - a TTD a month out is "ok"
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'spokojny', ttd: inDays(30) }),
		]);

		// When
		renderPage();
		await screen.findByText('Dashboard');

		// Then
		expect(screen.queryByText('Alerty wygaśnięcia')).not.toBeInTheDocument();
	});

	it('Alerts are ordered expired first, then urgent, then merely approaching — because that is the order they need reacting to', async () => {
		// Given - deliberately supplied in the WRONG order
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'za-tydzien', ttd: inDays(6) }),
			album({ albumId: 'a2', name: 'wygasly', ttd: inDays(-2) }),
			album({ albumId: 'a3', name: 'pilny', ttd: inDays(2) }),
			album({ albumId: 'a4', name: 'daleki', ttd: inDays(60) }),
		]);

		// When
		renderPage();
		await screen.findByText('Alerty wygaśnięcia');

		// Then - the far-away one is absent and the remaining three are sorted by urgency
		const alerts = screen.getByText('Alerty wygaśnięcia').closest('div')!;
		const names = within(alerts)
			.getAllByRole('link')
			.map((link) => link.textContent);
		expect(names[0]).toContain('wygasly');
		expect(names[1]).toContain('pilny');
		expect(names[2]).toContain('za-tydzien');
		expect(names.join(' ')).not.toContain('daleki');
	});

	it('An expired album is labelled as expired rather than showing a date that already passed', async () => {
		// Given
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'wygasly', ttd: inDays(-1) }),
		]);

		// When
		renderPage();

		// Then
		expect(await screen.findByText('Wygasł')).toBeInTheDocument();
	});

	it('The no-TTD warning uses Polish singular and plural, so the sentence reads correctly for one album and for several', async () => {
		// Given - exactly one album without a TTD
		noTtdMock.mockResolvedValue([album({ albumId: 'a1' })]);

		// When
		const { unmount } = renderPage();

		// Then
		expect(await screen.findByText(/1 album nie ma/)).toBeInTheDocument();
		unmount();

		// Given - three of them
		noTtdMock.mockResolvedValue([
			album({ albumId: 'a1' }),
			album({ albumId: 'a2' }),
			album({ albumId: 'a3' }),
		]);

		// When
		renderPage();

		// Then
		expect(await screen.findByText(/3 albumów nie ma/)).toBeInTheDocument();
	});

	it('No warning appears when every album has a TTD, so the dashboard is quiet when nothing is pending', async () => {
		// Given
		noTtdMock.mockResolvedValue([]);
		albumsMock.mockResolvedValue([album({ albumId: 'a1', ttd: inDays(30) })]);

		// When
		renderPage();
		await screen.findByText('Dashboard');

		// Then
		expect(screen.queryByText(/nie ma/)).not.toBeInTheDocument();
	});

	it('Recent albums are ordered by the newest upload inside them, not by the order the API returned', async () => {
		// Given - the album listed first holds the OLDEST photo
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'stary', files: [file('2026-01-01T10:00:00Z')] }),
			album({ albumId: 'a2', name: 'nowy', files: [file('2026-07-20T10:00:00Z')] }),
		]);

		// When
		renderPage();
		const recent = (await screen.findByText('Ostatnie albumy')).closest(
			'div',
		) as HTMLElement;

		// Then
		const names = within(recent)
			.getAllByRole('link')
			.map((link) => link.textContent);
		expect(names[0]).toContain('nowy');
		expect(names[1]).toContain('stary');
	});

	it('An album with no photos sorts last among recent albums, because it has no upload date to compare', async () => {
		// Given
		albumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'pusty', files: [] }),
			album({ albumId: 'a2', name: 'z-fotkami', files: [file('2026-07-20T10:00:00Z')] }),
		]);

		// When
		renderPage();
		const recent = (await screen.findByText('Ostatnie albumy')).closest(
			'div',
		) as HTMLElement;

		// Then
		const names = within(recent)
			.getAllByRole('link')
			.map((link) => link.textContent);
		expect(names[0]).toContain('z-fotkami');
		expect(names[1]).toContain('pusty');
	});

	it('At most five recent albums are listed, so the panel stays a summary and not a full listing', async () => {
		// Given - seven albums
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
		const recent = (await screen.findByText('Ostatnie albumy')).closest(
			'div',
		) as HTMLElement;

		// Then
		expect(within(recent).getAllByRole('link')).toHaveLength(5);
	});

	it('A photographer with no albums sees an explicit empty note rather than an empty box', async () => {
		// Given
		albumsMock.mockResolvedValue([]);

		// When
		renderPage();

		// Then
		expect(await screen.findByText('Brak albumów')).toBeInTheDocument();
	});
});
