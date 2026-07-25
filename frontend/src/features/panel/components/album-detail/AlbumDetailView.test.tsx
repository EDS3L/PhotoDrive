import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
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
import type { AlbumDto } from '@/shared/types/api';

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

const album: AlbumDto = {
	albumId: 'album-1',
	name: 'Sesja Kowalscy',
	photographId: 'photograf-1',
	clientId: 'klient-1',
	ttd: null,
	files: [],
	isPublic: false,
	displayName: null,
	displayOrder: 0,
};

function voidMutation<TVars>(): UseMutationResult<void, Error, TVars, unknown> {
	return {
		mutate: vi.fn(),
		mutateAsync: vi.fn().mockResolvedValue(undefined),
		isPending: false,
	} as unknown as UseMutationResult<void, Error, TVars, unknown>;
}

const config: AlbumDetailConfig = {
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
			({ data: [album], isLoading: false }) as unknown as UseQueryResult<
				AlbumDto[],
				Error
			>,
		useDeleteAlbum: () => voidMutation<string>(),
		useRemoveFiles: () => voidMutation<{ albumId: string; fileIds: string[] }>(),
		useSetFilesVisible: () =>
			voidMutation<{ albumId: string; fileIds: string[]; visible: boolean }>(),
		useAddWatermark: () =>
			voidMutation<{
				albumId: string;
				fileIds: string[];
				hasWatermark: boolean;
			}>(),
		useSwapFiles: () =>
			voidMutation<{
				sourceAlbumId: string;
				targetAlbumId: string;
				fileIds: string[];
			}>(),
		useRenameFile: () =>
			voidMutation<{ albumId: string; fileId: string; newName: string }>(),
		useSetAlbumTtd: () => voidMutation<{ albumId: string; ttd: string }>(),
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

function renderView() {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false } },
	});
	return render(
		<QueryClientProvider client={queryClient}>
			<MemoryRouter initialEntries={['/panel/albums/album-1']}>
				<Routes>
					<Route
						path='/panel/albums/:albumId'
						element={<AlbumDetailView config={config} />}
					/>
				</Routes>
			</MemoryRouter>
		</QueryClientProvider>,
	);
}

describe('AlbumDetailView', () => {
	beforeEach(() => {
		uploadFilesMock.mockReset();
		getAlbumFileNamesMock.mockReset();
		getWatermarkStatusMock.mockReset();
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
		renderView();
		const input = await screen.findByLabelText('Pliki zdjęć do wgrania');
		const photo = new File(['x'], 'foto.jpg', { type: 'image/jpeg' });

		// When - the photographer picks the very same file twice in a row
		await user.upload(input, photo);
		await waitFor(() => expect(uploadFilesMock).toHaveBeenCalledTimes(1));
		await user.upload(input, photo);

		// Then - the second pick reaches the API as well. A file input that keeps its previous
		// value emits no `change` for an identical pick, so the whole upload would be skipped.
		await waitFor(() => expect(uploadFilesMock).toHaveBeenCalledTimes(2));
		expect(uploadFilesMock.mock.calls[1][1]).toEqual([photo]);
	});
});
