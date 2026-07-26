import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AlbumList } from './AlbumList';
import { useAuthStore } from '@/app/store/authStore';
import type { getAssignedAlbums } from '../api/clientZoneApi';
import type { AlbumDto, FileDto } from '@/shared/types/api';

const { getAlbumsMock } = vi.hoisted(() => ({
	getAlbumsMock: vi.fn<typeof getAssignedAlbums>(),
}));

vi.mock('../api/clientZoneApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('../api/clientZoneApi')>();
	return { ...actual, getAssignedAlbums: getAlbumsMock };
});

function file(overrides: Partial<FileDto>): FileDto {
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
		albumId: 'album-1',
		name: 'sesja-lipiec',
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

const YESTERDAY = new Date(Date.now() - 86_400_000).toISOString();
const NEXT_YEAR = new Date(Date.now() + 365 * 86_400_000).toISOString();

function renderList(onOpen = vi.fn()) {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false } },
	});
	render(
		<QueryClientProvider client={queryClient}>
			<AlbumList onOpenAlbum={onOpen} />
		</QueryClientProvider>,
	);
	return { onOpen };
}

/** Karta albumu = kontener, w którym jest jego nagłówek. */
function cardOf(name: string): HTMLElement {
	return screen.getByText(name).closest('div.border') as HTMLElement;
}

describe('AlbumList / AlbumCard (client zone)', () => {
	beforeEach(() => {
		getAlbumsMock.mockReset().mockResolvedValue([]);
		// `useAlbums` pyta o dane tylko dla zalogowanego klienta.
		useAuthStore.getState().setSession({
			email: 'klient@example.com',
			userId: 'klient-1',
			mustChangePassword: false,
		});
	});

	it('A client with no albums is told they will appear later, so an empty page does not look broken', async () => {
		// Given
		getAlbumsMock.mockResolvedValue([]);

		// When
		renderList();

		// Then
		expect(await screen.findByText('Brak albumów')).toBeInTheDocument();
	});

	it('A failed load says so instead of pretending the client has no albums, because the two mean very different things', async () => {
		// Given
		getAlbumsMock.mockRejectedValue(new Error('network'));

		// When
		renderList();

		// Then
		expect(
			await screen.findByText(/Nie udało się załadować albumów/),
		).toBeInTheDocument();
		expect(screen.queryByText('Brak albumów')).not.toBeInTheDocument();
	});

	it('The cover is the first VISIBLE photo, so a hidden photo is never used as the album thumbnail', async () => {
		// Given - the hidden photo comes first in the album
		getAlbumsMock.mockResolvedValue([
			album({
				name: 'sesja',
				files: [
					file({ fileId: 'f1', fileName: 'ukryte.jpg', visible: false }),
					file({ fileId: 'f2', fileName: 'widoczne.jpg', visible: true }),
				],
			}),
		]);

		// When
		renderList();

		// Then
		const cover = await screen.findByRole('img', { name: 'sesja' });
		expect(cover).toHaveAttribute('src', expect.stringContaining('widoczne.jpg'));
		expect(cover).not.toHaveAttribute('src', expect.stringContaining('ukryte.jpg'));
	});

	it('An album with nothing visible falls back to a placeholder, so the card never shows a broken image', async () => {
		// Given
		getAlbumsMock.mockResolvedValue([
			album({ name: 'sesja', files: [file({ visible: false })] }),
		]);

		// When
		renderList();

		// Then
		const cover = await screen.findByRole('img', { name: 'sesja' });
		expect(cover.getAttribute('src')).not.toContain('/api/album/');
	});

	it('The photo count on the card counts visible photos only, matching what opening the album will show', async () => {
		// Given
		getAlbumsMock.mockResolvedValue([
			album({
				name: 'sesja',
				files: [
					file({ fileId: 'f1', visible: true }),
					file({ fileId: 'f2', visible: true }),
					file({ fileId: 'f3', visible: false }),
				],
			}),
		]);

		// When
		renderList();

		// Then
		expect(await screen.findByText(/2 zdjęć/)).toBeInTheDocument();
	});

	it('An expired album cannot be opened, because the backend would refuse the photos anyway', async () => {
		// Given
		getAlbumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'wygasly', ttd: YESTERDAY, files: [file({})] }),
		]);

		// When
		const { onOpen } = renderList();
		await screen.findByText('wygasly');

		// Then - no "open" action at all, just an archival note
		const card = cardOf('wygasly');
		expect(within(card).queryByRole('button', { name: 'Otwórz album' })).toBeNull();
		expect(within(card).getByRole('button', { name: 'Archiwalne' })).toBeDisabled();
		expect(within(card).getByText('Link wygasł')).toBeInTheDocument();
		expect(onOpen).not.toHaveBeenCalled();
	});

	it('A live album opens with the album it belongs to, so clicking one card cannot open another', async () => {
		// Given
		getAlbumsMock.mockResolvedValue([
			album({ albumId: 'a1', name: 'pierwszy', files: [file({})] }),
			album({ albumId: 'a2', name: 'drugi', files: [file({})] }),
		]);
		const { onOpen } = renderList();
		await screen.findByText('drugi');

		// When
		await userEvent.click(
			within(cardOf('drugi')).getByRole('button', { name: 'Otwórz album' }),
		);

		// Then
		expect(onOpen).toHaveBeenCalledTimes(1);
		expect(onOpen.mock.calls[0][0]).toMatchObject({ albumId: 'a2' });
	});

	it('A future expiry is shown as a deadline rather than as expired', async () => {
		// Given
		getAlbumsMock.mockResolvedValue([
			album({ name: 'aktualny', ttd: NEXT_YEAR, files: [file({})] }),
		]);

		// When
		renderList();

		// Then
		expect(await screen.findByText(/Dostępny do/)).toBeInTheDocument();
		expect(screen.queryByText('Link wygasł')).not.toBeInTheDocument();
	});
});
