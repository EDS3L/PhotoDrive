import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AlbumDetailModal } from './AlbumDetailModal';
import type { downloadAlbumZip } from '../api/clientZoneApi';
import type { triggerBlobDownload } from '@/lib/downloadBlob';
import type { AlbumDto, FileDto } from '@/shared/types/api';

const { downloadZipMock, triggerDownloadMock } = vi.hoisted(() => ({
	downloadZipMock: vi.fn<typeof downloadAlbumZip>(),
	triggerDownloadMock: vi.fn<typeof triggerBlobDownload>(),
}));

vi.mock('../api/clientZoneApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('../api/clientZoneApi')>();
	return { ...actual, downloadAlbumZip: downloadZipMock };
});

vi.mock('@/lib/downloadBlob', () => ({
	triggerBlobDownload: triggerDownloadMock,
}));

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

function album(overrides: Partial<AlbumDto> = {}): AlbumDto {
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

function renderModal(dto: AlbumDto, onClose = vi.fn()) {
	render(<AlbumDetailModal album={dto} onClose={onClose} />);
	return { onClose };
}

/** Lightbox to drugi dialog — rozpoznajemy go po nazwie „Podgląd zdjęcia i z n". */
function lightbox(): HTMLElement {
	return screen.getByRole('dialog', { name: /Podgląd zdjęcia/ });
}

describe('AlbumDetailModal (client zone)', () => {
	beforeEach(() => {
		downloadZipMock.mockReset().mockResolvedValue(new Blob(['zip']));
		triggerDownloadMock.mockReset();
	});

	it('A hidden photo is not shown to the client, because visibility is what the photographer curates with', () => {
		// Given - one visible and one hidden photo
		renderModal(
			album({
				files: [
					file({ fileId: 'f1', fileName: 'widoczne.jpg', visible: true }),
					file({ fileId: 'f2', fileName: 'ukryte.jpg', visible: false }),
				],
			}),
		);

		// Then - the hidden one must not reach the client at all, not even as a thumbnail
		expect(screen.getByRole('img', { name: 'widoczne.jpg' })).toBeInTheDocument();
		expect(screen.queryByRole('img', { name: 'ukryte.jpg' })).not.toBeInTheDocument();
	});

	it('The photo count counts visible photos only, so the number matches what is on screen', () => {
		// Given
		renderModal(
			album({
				files: [
					file({ fileId: 'f1', visible: true }),
					file({ fileId: 'f2', visible: true }),
					file({ fileId: 'f3', visible: false }),
				],
			}),
		);

		// Then
		expect(screen.getByText('2 zdjęć')).toBeInTheDocument();
	});

	it('Downloading a ZIP asks for the visible photos only, so a hidden photo cannot leak through the archive', async () => {
		// Given
		renderModal(
			album({
				files: [
					file({ fileId: 'f1', fileName: 'widoczne.jpg', visible: true }),
					file({ fileId: 'f2', fileName: 'ukryte.jpg', visible: false }),
				],
			}),
		);

		// When
		await userEvent.click(screen.getByRole('button', { name: /Pobierz wszystkie/ }));

		// Then
		await vi.waitFor(() =>
			expect(downloadZipMock).toHaveBeenCalledWith('album-1', ['widoczne.jpg']),
		);
	});

	it('The archive is named after the album, so the client does not end up with a file named by an id', async () => {
		// Given
		renderModal(album({ name: 'sesja-lipiec', files: [file({ visible: true })] }));

		// When
		await userEvent.click(screen.getByRole('button', { name: /Pobierz wszystkie/ }));

		// Then
		await vi.waitFor(() =>
			expect(triggerDownloadMock).toHaveBeenCalledWith(
				expect.any(Blob),
				'sesja-lipiec.zip',
			),
		);
	});

	it('An album with nothing visible offers no download, because the ZIP would be empty', () => {
		// Given
		renderModal(album({ files: [file({ visible: false })] }));

		// Then
		expect(screen.getByRole('button', { name: /Pobierz wszystkie/ })).toBeDisabled();
	});

	it('Clicking a photo opens the preview at that photo, not at the first one', async () => {
		// Given
		renderModal(
			album({
				files: [
					file({ fileId: 'f1', fileName: 'pierwsze.jpg' }),
					file({ fileId: 'f2', fileName: 'drugie.jpg' }),
					file({ fileId: 'f3', fileName: 'trzecie.jpg' }),
				],
			}),
		);

		// When - the second photo
		await userEvent.click(screen.getByRole('img', { name: 'drugie.jpg' }));

		// Then
		expect(lightbox()).toBeInTheDocument();
		expect(within(lightbox()).getByText('2 / 3')).toBeInTheDocument();
	});

	it('Preview navigation wraps around, so the last photo leads back to the first', async () => {
		// Given
		renderModal(
			album({
				files: [
					file({ fileId: 'f1', fileName: 'pierwsze.jpg' }),
					file({ fileId: 'f2', fileName: 'drugie.jpg' }),
				],
			}),
		);
		await userEvent.click(screen.getByRole('img', { name: 'drugie.jpg' }));

		// When - forward from the last photo
		await userEvent.click(screen.getByRole('button', { name: 'Następne zdjęcie' }));

		// Then
		expect(within(lightbox()).getByText('1 / 2')).toBeInTheDocument();

		// When - and backwards from the first
		await userEvent.click(screen.getByRole('button', { name: 'Poprzednie zdjęcie' }));

		// Then
		expect(within(lightbox()).getByText('2 / 2')).toBeInTheDocument();
	});

	it('Arrow keys navigate the preview, because that is how a photo viewer is expected to work', async () => {
		// Given
		renderModal(
			album({
				files: [
					file({ fileId: 'f1', fileName: 'pierwsze.jpg' }),
					file({ fileId: 'f2', fileName: 'drugie.jpg' }),
					file({ fileId: 'f3', fileName: 'trzecie.jpg' }),
				],
			}),
		);
		await userEvent.click(screen.getByRole('img', { name: 'pierwsze.jpg' }));

		// When
		await userEvent.keyboard('{ArrowRight}');

		// Then
		expect(within(lightbox()).getByText('2 / 3')).toBeInTheDocument();

		// When
		await userEvent.keyboard('{ArrowLeft}');

		// Then
		expect(within(lightbox()).getByText('1 / 3')).toBeInTheDocument();
	});

	it('A single photo gets no navigation arrows, since there is nowhere to go', async () => {
		// Given
		renderModal(album({ files: [file({ fileName: 'jedyne.jpg' })] }));

		// When
		await userEvent.click(screen.getByRole('img', { name: 'jedyne.jpg' }));

		// Then
		expect(
			screen.queryByRole('button', { name: 'Następne zdjęcie' }),
		).not.toBeInTheDocument();
		expect(
			screen.queryByRole('button', { name: 'Poprzednie zdjęcie' }),
		).not.toBeInTheDocument();
	});

	it('Escape closes the preview first and keeps the album open, so one key does not undo two steps', async () => {
		// Given
		const { onClose } = renderModal(
			album({ files: [file({ fileName: 'foto.jpg' })] }),
		);
		await userEvent.click(screen.getByRole('img', { name: 'foto.jpg' }));

		// When
		await userEvent.keyboard('{Escape}');

		// Then - the preview is gone but the album modal stays
		expect(
			screen.queryByRole('dialog', { name: /Podgląd zdjęcia/ }),
		).not.toBeInTheDocument();
		expect(onClose).not.toHaveBeenCalled();

		// When - a second Escape, now with no preview open
		await userEvent.keyboard('{Escape}');

		// Then
		expect(onClose).toHaveBeenCalled();
	});

	it('Clicking the preview backdrop closes only the preview', async () => {
		// Given
		const { onClose } = renderModal(
			album({ files: [file({ fileName: 'foto.jpg' })] }),
		);
		await userEvent.click(screen.getByRole('img', { name: 'foto.jpg' }));

		// When - the backdrop is the lightbox container itself
		await userEvent.click(lightbox());

		// Then
		expect(
			screen.queryByRole('dialog', { name: /Podgląd zdjęcia/ }),
		).not.toBeInTheDocument();
		expect(onClose).not.toHaveBeenCalled();
	});

	it('The close button closes the album, which is the way out for a mouse user', async () => {
		// Given
		const { onClose } = renderModal(album({ files: [file({})] }));

		// When
		await userEvent.click(screen.getByRole('button', { name: 'Zamknij' }));

		// Then
		expect(onClose).toHaveBeenCalled();
	});

	it('Background scrolling is blocked while the album is open and restored on close, so the page behind does not drift', () => {
		// Given
		const { unmount } = render(
			<AlbumDetailModal album={album({ files: [file({})] })} onClose={vi.fn()} />,
		);

		// Then
		expect(document.body.style.overflow).toBe('hidden');
		expect(document.documentElement.style.overflow).toBe('hidden');

		// When
		unmount();

		// Then - leaving the page permanently unscrollable would be a trap
		expect(document.body.style.overflow).not.toBe('hidden');
		expect(document.documentElement.style.overflow).not.toBe('hidden');
	});

	it('The expiry date is shown when the album has one, so the client knows the deadline', () => {
		// Given
		renderModal(album({ ttd: '2026-12-01T00:00:00Z', files: [file({})] }));

		// Then
		expect(screen.getByText(/Wygasa/)).toBeInTheDocument();
	});

	it('An album without an expiry says nothing about one, instead of showing an empty date', () => {
		// Given
		renderModal(album({ ttd: null, files: [file({})] }));

		// Then
		expect(screen.queryByText(/Wygasa/)).not.toBeInTheDocument();
	});

	it('The grid asks for thumbnails rather than originals, so opening an album does not download full-size photos', () => {
		// Given
		renderModal(album({ files: [file({ fileName: 'foto.jpg' })] }));

		// Then
		expect(screen.getByRole('img', { name: 'foto.jpg' })).toHaveAttribute(
			'src',
			expect.stringContaining('width=600'),
		);
	});
});
