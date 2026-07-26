import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { AxiosRequestConfig } from 'axios';

const { apiClientMock } = vi.hoisted(() => ({
	apiClientMock: {
		get: vi.fn(),
		post: vi.fn(),
		patch: vi.fn(),
		put: vi.fn(),
		delete: vi.fn(),
	},
}));

vi.mock('@/lib/apiClient', () => ({ apiClient: apiClientMock }));

import {
	swapFiles,
	setFilesVisible,
	addWatermark,
	removeFiles,
	renameFile,
	downloadAlbum,
	getPhotoUrl,
	uploadFiles,
	getAlbumFileNames,
	setAlbumTtd,
	deleteAlbum,
} from './albumFilesApi';

describe('albumFilesApi (single source for admin/photographer/client, F.2)', () => {
	beforeEach(() => {
		apiClientMock.get.mockReset().mockResolvedValue({ data: [] });
		apiClientMock.post.mockReset().mockResolvedValue({ data: new ArrayBuffer(8) });
		apiClientMock.patch.mockReset().mockResolvedValue({ data: undefined });
		apiClientMock.put.mockReset().mockResolvedValue({ data: undefined });
		apiClientMock.delete.mockReset().mockResolvedValue({ data: undefined });
	});

	it('swaps files between the two named albums with a fileIdList body', async () => {
		// When
		await swapFiles('src-1', 'dst-2', ['a', 'b']);

		// Then - the exact endpoint the backend expects (a drift here would silently break swap)
		expect(apiClientMock.patch).toHaveBeenCalledWith(
			'/album/src-1/album/dst-2/swap',
			{ fileIdList: ['a', 'b'] },
		);
	});

	it('sets visibility with idList body and the flag as a query param, not in the body', async () => {
		// When
		await setFilesVisible('alb-1', ['f1'], true);

		// Then
		expect(apiClientMock.patch).toHaveBeenCalledWith(
			'/album/alb-1/files/setVisible',
			{ idList: ['f1'] },
			{ params: { visible: true } },
		);
	});

	it('toggles watermark with filesUUIDList body and the flag as a query param', async () => {
		// When
		await addWatermark('alb-1', ['f1', 'f2'], false);

		// Then
		expect(apiClientMock.post).toHaveBeenCalledWith(
			'/album/alb-1/files/addWatermark',
			{ filesUUIDList: ['f1', 'f2'] },
			{ params: { hasWatermark: false } },
		);
	});

	it('removes files with a fileIdList body', async () => {
		// When
		await removeFiles('alb-1', ['f1']);

		// Then
		expect(apiClientMock.post).toHaveBeenCalledWith('/album/alb-1/remove', {
			fileIdList: ['f1'],
		});
	});

	it('renames a file by id with a newFileName body', async () => {
		// When
		await renameFile('alb-1', 'file-9', 'nowa.jpg');

		// Then
		expect(apiClientMock.put).toHaveBeenCalledWith(
			'/album/alb-1/rename/file-9',
			{ newFileName: 'nowa.jpg' },
		);
	});

	it('downloads the album as an application/zip blob fetched as an arraybuffer', async () => {
		// When
		const blob = await downloadAlbum('alb-1', ['a.jpg', 'b.jpg']);

		// Then - the response must be requested as binary, otherwise the zip gets corrupted
		expect(apiClientMock.post).toHaveBeenCalledWith(
			'/album/alb-1/download',
			{ fileList: ['a.jpg', 'b.jpg'] },
			{ responseType: 'arraybuffer' },
		);
		expect(blob).toBeInstanceOf(Blob);
		expect(blob.type).toBe('application/zip');
	});

	it('builds a photo URL that encodes the filename and appends width+height only when given', () => {
		// Then - filename with a space must be encoded, both dimensions passed through
		expect(getPhotoUrl('alb-1', 'ślub 1.jpg', 1200, 800)).toBe(
			'/api/album/alb-1/photo/%C5%9Blub%201.jpg?width=1200&height=800',
		);
		// ...and no query at all when no size is requested
		expect(getPhotoUrl('alb-1', 'a.jpg')).toBe('/api/album/alb-1/photo/a.jpg');
	});

	it('Collision detection asks for names only, so opening the upload dialog does not pull whole album metadata', async () => {
		// Given
		apiClientMock.get.mockResolvedValue({ data: ['foto.jpg', 'foto_1.jpg'] });

		// When
		const names = await getAlbumFileNames('alb-1');

		// Then - this lightweight query is what feeds the rename/skip dialog (B.28)
		expect(apiClientMock.get).toHaveBeenCalledWith('/album/alb-1/file-names');
		expect(names).toEqual(['foto.jpg', 'foto_1.jpg']);
	});

	it('A TTD is sent in the body under the name the backend reads', async () => {
		// When
		await setAlbumTtd('alb-1', '2026-12-01');

		// Then
		expect(apiClientMock.patch).toHaveBeenCalledWith('/album/alb-1/setTtd', {
			ttd: '2026-12-01',
		});
	});

	it('Deleting an album uses DELETE on its own path, so it cannot be confused with removing files from it', async () => {
		// When
		await deleteAlbum('alb-1');

		// Then - `/remove` (POST) drops selected photos, `/delete` drops the album itself
		expect(apiClientMock.delete).toHaveBeenCalledWith('/album/alb-1/delete');
		expect(apiClientMock.post).not.toHaveBeenCalled();
	});

	it('uploads as multipart and reports progress as a whole percentage', async () => {
		// Given
		const onProgress = vi.fn();
		const file = new File(['x'], 'a.jpg', { type: 'image/jpeg' });

		// When
		await uploadFiles('alb-1', [file], onProgress);

		// Then - correct endpoint + multipart body
		const [url, body, config] = apiClientMock.post.mock.calls[0] as [
			string,
			FormData,
			AxiosRequestConfig,
		];
		expect(url).toBe('/album/upload/alb-1/files');
		expect(body).toBeInstanceOf(FormData);
		expect(config.headers).toEqual({ 'Content-Type': 'multipart/form-data' });

		// ...and the axios progress event is turned into a rounded percent
		config.onUploadProgress?.({ loaded: 50, total: 200 } as never);
		expect(onProgress).toHaveBeenCalledWith(25);
	});
});
