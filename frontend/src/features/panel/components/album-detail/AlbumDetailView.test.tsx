import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
	QueryClient,
	QueryClientProvider,
	type UseMutationResult,
	type UseQueryResult,
} from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AlbumDetailView, type AlbumDetailConfig } from './AlbumDetailView';
import type { getWatermarkStatus } from '../../api/watermarkApi';
import type { AlbumDto, FileDto } from '@/shared/types/api';

const { getWatermarkStatusMock } = vi.hoisted(() => ({
	getWatermarkStatusMock: vi.fn<typeof getWatermarkStatus>(),
}));

// Podmieniamy TYLKO wywołanie HTTP statusu watermarku — reszta ekranu (hooki uploadu,
// zaznaczanie, filtr) działa naprawdę; API albumu wstrzykujemy przez `config`.
vi.mock('../../api/watermarkApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('../../api/watermarkApi')>();
	return { ...actual, getWatermarkStatus: getWatermarkStatusMock };
});

const uploadFilesMock = vi.fn<AlbumDetailConfig['api']['uploadFiles']>();
const getAlbumFileNamesMock =
	vi.fn<AlbumDetailConfig['api']['getAlbumFileNames']>();

type VisibilityVars = { albumId: string; fileIds: string[]; visible: boolean };
type WatermarkVars = { albumId: string; fileIds: string[]; hasWatermark: boolean };
type RemoveVars = { albumId: string; fileIds: string[] };

const setVisibleMock = vi.fn<(vars: VisibilityVars) => void>();
const setWatermarkMock = vi.fn<(vars: WatermarkVars) => void>();
const removeFilesMock = vi.fn<(vars: RemoveVars) => void>();

function photo(name: string, overrides: Partial<FileDto> = {}): FileDto {
	return {
		fileId: `id-${name}`,
		fileName: name,
		sizeBytes: 1024,
		contentType: 'image/jpeg',
		uploadedAt: '2026-07-25T10:00:00Z',
		visible: true,
		hasWatermark: false,
		...overrides,
	};
}

const PHOTOGRAPHER = 'photograf-1';
const CLIENT = 'klient-1';

/** Album klienta: fotograf i klient to różne osoby. */
function clientAlbum(files: FileDto[], overrides: Partial<AlbumDto> = {}): AlbumDto {
	return {
		albumId: 'album-1',
		name: 'Sesja Kowalscy',
		photographId: PHOTOGRAPHER,
		clientId: CLIENT,
		ttd: null,
		files,
		isPublic: false,
		displayName: null,
		displayOrder: 0,
		...overrides,
	};
}

/** Album portfolio: `photographId === clientId` (admin jest oboma). */
function portfolioAlbum(id: string, name: string, files: FileDto[] = []): AlbumDto {
	return {
		albumId: id,
		name,
		photographId: 'admin-1',
		clientId: 'admin-1',
		ttd: null,
		files,
		isPublic: false,
		displayName: null,
		displayOrder: 0,
	};
}

function mutationStub<TVars>(
	spy?: (vars: TVars) => void,
): UseMutationResult<void, Error, TVars, unknown> {
	return {
		mutate: (vars: TVars, opts?: { onSuccess?: () => void }) => {
			spy?.(vars);
			opts?.onSuccess?.();
		},
		mutateAsync: vi.fn().mockResolvedValue(undefined),
		isPending: false,
	} as unknown as UseMutationResult<void, Error, TVars, unknown>;
}

function makeConfig(albums: AlbumDto[]): AlbumDetailConfig {
	return {
		albumsQueryKey: ['panel', 'test-albums'],
		albumsListPath: '/panel/albums',
		api: {
			getPhotoUrl: (albumId, fileName) =>
				`/api/album/${albumId}/photo/${fileName}`,
			getAlbumFileNames: getAlbumFileNamesMock,
			uploadFiles: uploadFilesMock,
		},
		hooks: {
			useAlbums: () =>
				({ data: albums, isLoading: false }) as unknown as UseQueryResult<
					AlbumDto[],
					Error
				>,
			useDeleteAlbum: () => mutationStub<string>(),
			useRemoveFiles: () => mutationStub<RemoveVars>(removeFilesMock),
			useSetFilesVisible: () => mutationStub<VisibilityVars>(setVisibleMock),
			useAddWatermark: () => mutationStub<WatermarkVars>(setWatermarkMock),
			useSwapFiles: () =>
				mutationStub<{
					sourceAlbumId: string;
					targetAlbumId: string;
					fileIds: string[];
				}>(),
			useRenameFile: () =>
				mutationStub<{ albumId: string; fileId: string; newName: string }>(),
			useSetAlbumTtd: () => mutationStub<{ albumId: string; ttd: string }>(),
			useDownloadAlbum: () =>
				({
					mutate: vi.fn(),
					isPending: false,
				}) as unknown as UseMutationResult<
					Blob,
					Error,
					{ albumId: string; fileList: string[] },
					unknown
				>,
		},
	};
}

function renderView(albums: AlbumDto[]) {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false } },
	});
	return render(
		<QueryClientProvider client={queryClient}>
			<MemoryRouter initialEntries={['/panel/albums/album-1']}>
				<Routes>
					<Route
						path='/panel/albums/:albumId'
						element={<AlbumDetailView config={makeConfig(albums)} />}
					/>
				</Routes>
			</MemoryRouter>
		</QueryClientProvider>,
	);
}

/** Wchodzi w tryb zaznaczania i klika podane zdjęcia. */
async function selectPhotos(user: ReturnType<typeof userEvent.setup>, names: string[]) {
	await user.click(screen.getByRole('button', { name: /Zaznacz$/ }));
	for (const name of names) {
		await user.click(screen.getByAltText(name));
	}
}

describe('AlbumDetailView', () => {
	beforeEach(() => {
		uploadFilesMock.mockReset();
		getAlbumFileNamesMock.mockReset();
		getWatermarkStatusMock.mockReset();
		setVisibleMock.mockReset();
		setWatermarkMock.mockReset();
		removeFilesMock.mockReset();
		getWatermarkStatusMock.mockResolvedValue({
			configured: false,
			updatedAt: null,
		});
	});

	it('Picking the same photo again still starts an upload, so a cancelled collision dialog or a re-added deleted photo does not leave the button dead', async () => {
		// Given - an album with no name collisions, so every pick goes straight to the upload
		getAlbumFileNamesMock.mockResolvedValue([]);
		uploadFilesMock.mockResolvedValue(undefined);
		const user = userEvent.setup();
		renderView([clientAlbum([])]);
		const input = await screen.findByLabelText('Pliki zdjęć do wgrania');
		const file = new File(['x'], 'foto.jpg', { type: 'image/jpeg' });

		// When - the photographer picks the very same file twice in a row
		await user.upload(input, file);
		await waitFor(() => expect(uploadFilesMock).toHaveBeenCalledTimes(1));
		await user.upload(input, file);

		// Then - the second pick reaches the API as well. A file input that keeps its previous
		// value emits no `change` for an identical pick, so the whole upload would be skipped.
		await waitFor(() => expect(uploadFilesMock).toHaveBeenCalledTimes(2));
		expect(uploadFilesMock.mock.calls[1][1]).toEqual([file]);
	});

	// -----------------------------------------------------------------------
	// filtr widoczności
	// -----------------------------------------------------------------------

	it('The hidden filter shows only photos the client cannot see, because that is the list the photographer curates from', async () => {
		// Given - a half-curated album
		const user = userEvent.setup();
		renderView([
			clientAlbum([
				photo('widoczne.jpg', { visible: true }),
				photo('ukryte.jpg', { visible: false }),
			]),
		]);

		// When
		await user.click(screen.getByRole('button', { name: 'Ukryte' }));

		// Then
		expect(screen.getByAltText('ukryte.jpg')).toBeInTheDocument();
		expect(screen.queryByAltText('widoczne.jpg')).not.toBeInTheDocument();
	});

	it('Select-all under a filter takes only the photos on screen, so a batch action never touches a photo the photographer cannot see', async () => {
		// Given - one visible and one hidden photo, filtered down to the visible one
		const user = userEvent.setup();
		renderView([
			clientAlbum([
				photo('widoczne.jpg', { visible: true }),
				photo('ukryte.jpg', { visible: false }),
			]),
		]);
		await user.click(screen.getByRole('button', { name: 'Widoczne' }));
		await user.click(screen.getByRole('button', { name: /Zaznacz$/ }));

		// When - "select all" while the hidden photo is filtered out
		await user.click(screen.getByRole('button', { name: /Zaznacz wszystkie/ }));
		await user.click(screen.getByRole('button', { name: /Ukryj/ }));

		// Then - only the on-screen photo is affected; the hidden one must not be swept in
		expect(setVisibleMock).toHaveBeenCalledWith({
			albumId: 'album-1',
			fileIds: ['id-widoczne.jpg'],
			visible: false,
		});
	});

	// -----------------------------------------------------------------------
	// akcje wsadowe adekwatne do zaznaczenia (B.19)
	// -----------------------------------------------------------------------

	it('A selection of only visible photos offers hiding but not showing, so the photographer is never offered an action that would do nothing', async () => {
		// Given
		const user = userEvent.setup();
		renderView([clientAlbum([photo('a.jpg', { visible: true })])]);

		// When
		await selectPhotos(user, ['a.jpg']);

		// Then
		expect(screen.getByRole('button', { name: /Ukryj/ })).toBeInTheDocument();
		expect(screen.queryByRole('button', { name: /^Pokaż/ })).not.toBeInTheDocument();
	});

	it('A mixed selection offers both showing and hiding, because batch actions are idempotent and skip photos already in the target state', async () => {
		// Given - one visible, one hidden, both selected
		const user = userEvent.setup();
		renderView([
			clientAlbum([
				photo('a.jpg', { visible: true }),
				photo('b.jpg', { visible: false }),
			]),
		]);

		// When
		await selectPhotos(user, ['a.jpg', 'b.jpg']);

		// Then
		expect(screen.getByRole('button', { name: /Ukryj/ })).toBeInTheDocument();
		expect(screen.getByRole('button', { name: /Pokaż/ })).toBeInTheDocument();
	});

	// -----------------------------------------------------------------------
	// watermark (A1 + reguła portfolio)
	// -----------------------------------------------------------------------

	it('Adding a watermark is hidden until a platform logo exists, while removing it stays available, so a leftover flag can always be cleared', async () => {
		// Given - no logo uploaded, and a photo that still carries the flag from before
		getWatermarkStatusMock.mockResolvedValue({ configured: false, updatedAt: null });
		const user = userEvent.setup();
		renderView([clientAlbum([photo('a.jpg', { hasWatermark: true })])]);

		// When
		await selectPhotos(user, ['a.jpg']);

		// Then - without this asymmetry the flag could never be cleared, and clearing it is
		// exactly what unblocks deleting the logo
		expect(
			screen.getByRole('button', { name: /Usuń watermark/ }),
		).toBeInTheDocument();
		expect(
			screen.queryByRole('button', { name: /^Watermark/ }),
		).not.toBeInTheDocument();
	});

	it('Watermarking is never offered on a portfolio album, because the platform watermark protects undelivered client photos rather than branding the showcase', async () => {
		// Given - a logo IS configured, so only the album type can be hiding the action
		getWatermarkStatusMock.mockResolvedValue({
			configured: true,
			updatedAt: '2026-07-25T10:00:00Z',
		});
		const user = userEvent.setup();
		renderView([portfolioAlbum('album-1', 'Portfolio', [photo('a.jpg')])]);
		await waitFor(() =>
			expect(getWatermarkStatusMock).toHaveBeenCalled(),
		);

		// When
		await selectPhotos(user, ['a.jpg']);

		// Then
		expect(
			screen.queryByRole('button', { name: /^Watermark/ }),
		).not.toBeInTheDocument();
	});

	it('Watermarking a client album is offered once a logo exists, which is the case the flag was designed for', async () => {
		// Given
		getWatermarkStatusMock.mockResolvedValue({
			configured: true,
			updatedAt: '2026-07-25T10:00:00Z',
		});
		const user = userEvent.setup();
		renderView([clientAlbum([photo('a.jpg')])]);
		await waitFor(() => expect(getWatermarkStatusMock).toHaveBeenCalled());

		// When
		await selectPhotos(user, ['a.jpg']);
		await user.click(screen.getByRole('button', { name: /^Watermark/ }));

		// Then
		expect(setWatermarkMock).toHaveBeenCalledWith({
			albumId: 'album-1',
			fileIds: ['id-a.jpg'],
			hasWatermark: true,
		});
	});

	// -----------------------------------------------------------------------
	// swap tylko portfolio↔portfolio
	// -----------------------------------------------------------------------

	it('Moving photos is not offered from a client album, because client material only enters by upload and leaves by deletion', async () => {
		// Given - a client album, with a portfolio album available as a tempting target
		const user = userEvent.setup();
		renderView([
			clientAlbum([photo('a.jpg')]),
			portfolioAlbum('album-2', 'Portfolio'),
		]);

		// When
		await selectPhotos(user, ['a.jpg']);

		// Then - offering it here would put private photos one toggle away from the internet
		expect(
			screen.queryByRole('button', { name: /Przenieś/ }),
		).not.toBeInTheDocument();
	});

	it('Moving from a portfolio album offers only other portfolio albums as targets, so a client album is never proposed', async () => {
		// Given - the source is portfolio; one portfolio and one client album exist elsewhere
		const user = userEvent.setup();
		renderView([
			portfolioAlbum('album-1', 'Portfolio Śluby', [photo('a.jpg')]),
			portfolioAlbum('album-2', 'Portfolio Plenery'),
			clientAlbum([], { albumId: 'album-3', name: 'Sesja Nowakowie' }),
		]);

		// When
		await selectPhotos(user, ['a.jpg']);
		await user.click(screen.getByRole('button', { name: /Przenieś/ }));

		// Then - the domain rejects a client target anyway (400), so proposing it would only
		// walk the photographer into an error
		expect(screen.getByText('Portfolio Plenery')).toBeInTheDocument();
		expect(screen.queryByText('Sesja Nowakowie')).not.toBeInTheDocument();
	});

	// -----------------------------------------------------------------------
	// usuwanie za potwierdzeniem
	// -----------------------------------------------------------------------

	it('Batch delete asks for confirmation first and only then removes exactly the selected photos', async () => {
		// Given
		const user = userEvent.setup();
		renderView([
			clientAlbum([photo('a.jpg'), photo('b.jpg'), photo('c.jpg')]),
		]);
		await selectPhotos(user, ['a.jpg', 'b.jpg']);

		// When - the destructive action is requested
		await user.click(screen.getByRole('button', { name: /Usuń \(2\)/ }));

		// Then - nothing is deleted before the confirmation is accepted
		expect(removeFilesMock).not.toHaveBeenCalled();
		const dialog = within(screen.getByRole('dialog'));
		await user.click(dialog.getByRole('button', { name: 'Usuń (2)' }));
		expect(removeFilesMock).toHaveBeenCalledWith({
			albumId: 'album-1',
			fileIds: ['id-a.jpg', 'id-b.jpg'],
		});
	});
});
