import { describe, it, expect, vi, beforeEach } from 'vitest';

const { publicClientMock } = vi.hoisted(() => ({
	publicClientMock: {
		get: vi.fn(),
		post: vi.fn(),
	},
}));

// publicApi.ts tworzy własną instancję przez axios.create(...) — podstawiamy ją mockiem.
// Buildery URL-i poniżej i tak nie dotykają klienta, więc mock im nie szkodzi.
vi.mock('axios', () => ({
	default: { create: () => publicClientMock },
}));

import {
	getPublicPhotoUrl,
	getSiteSlotPhotoUrl,
	sendContactMessage,
	getPublicAlbums,
	getPublicPhotosByAlbumName,
	getPublicSiteSlots,
	PUBLIC_PHOTO_SIZE,
} from './publicApi';

describe('getPublicPhotoUrl', () => {
	it('Every public photo URL carries a size, so the portfolio never requests the untouched original', () => {
		// Given / When
		const url = getPublicPhotoUrl('album-1', 'foto.jpg');

		// Then - even without an explicit size the URL asks for a variant
		expect(url).toContain('width=');
		expect(url).toBe(
			`/api/public/album/album-1/photo/foto.jpg?width=${PUBLIC_PHOTO_SIZE.full}`,
		);
	});

	it('Portfolio tiles ask for a small variant, so the grid does not download large photos', () => {
		// Given / When
		const url = getPublicPhotoUrl('album-1', 'foto.jpg', PUBLIC_PHOTO_SIZE.tile);

		// Then
		expect(url).toContain(`width=${PUBLIC_PHOTO_SIZE.tile}`);
		expect(PUBLIC_PHOTO_SIZE.tile).toBeLessThan(PUBLIC_PHOTO_SIZE.full);
	});

	it('File names are encoded, so a photo with spaces or Polish letters still resolves', () => {
		// Given / When
		const url = getPublicPhotoUrl('album-1', 'zdjęcie ślubne.jpg');

		// Then
		expect(url).toContain('zdj%C4%99cie%20%C5%9Blubne.jpg');
	});
});

describe('getSiteSlotPhotoUrl', () => {
	it('Two versions of the same slot give two different URLs, so an immutable-cached photo cannot survive a swap', () => {
		// Given / When
		const before = getSiteSlotPhotoUrl('HOME_HERO', 111);
		const after = getSiteSlotPhotoUrl('HOME_HERO', 222);

		// Then
		expect(before).not.toBe(after);
		expect(before).toContain('v=111');
		expect(after).toContain('v=222');
	});
});

describe('public reads (anonymous visitor)', () => {
	beforeEach(() => {
		publicClientMock.get.mockReset().mockResolvedValue({ data: [] });
	});

	it('The portfolio tab list is taken as the backend ordered it, because displayOrder is decided in the panel and not in the browser', async () => {
		// Given - the backend answers sorted by displayOrder, then by name (8.4)
		publicClientMock.get.mockResolvedValue({
			data: [
				{ albumId: 'a2', name: 'sluby', displayName: 'Śluby', photoCount: 4 },
				{ albumId: 'a1', name: 'plener', displayName: null, photoCount: 2 },
			],
		});

		// When
		const albums = await getPublicAlbums();

		// Then - re-sorting here (e.g. alphabetically) would silently override the admin's
		// chosen tab order, and the panel would look broken
		expect(publicClientMock.get).toHaveBeenCalledWith('/album/all');
		expect(albums.map((album) => album.albumId)).toEqual(['a2', 'a1']);
	});

	it('An album is fetched by name with the name encoded, so a tab label with a space or slash cannot break the path', async () => {
		// Given
		publicClientMock.get.mockResolvedValue({
			data: { albumId: 'a1', name: 'sesje plenerowe', photos: [] },
		});

		// When
		await getPublicPhotosByAlbumName('sesje plenerowe');

		// Then
		expect(publicClientMock.get).toHaveBeenCalledWith(
			'/album/by-name/sesje%20plenerowe',
		);
	});

	it('Site slots are read in a single request, so one page load does not fire one request per section', async () => {
		// Given
		publicClientMock.get.mockResolvedValue({
			data: [{ slot: 'HOME_HERO', version: 17 }],
		});

		// When
		const slots = await getPublicSiteSlots();

		// Then
		expect(publicClientMock.get).toHaveBeenCalledTimes(1);
		expect(publicClientMock.get).toHaveBeenCalledWith('/site/slots');
		expect(slots).toEqual([{ slot: 'HOME_HERO', version: 17 }]);
	});
});

describe('sendContactMessage (public contact contract, 8.1)', () => {
	beforeEach(() => {
		publicClientMock.post.mockReset().mockResolvedValue({ data: undefined });
	});

	it('posts the enquiry to /contact with the exact payload the backend expects', async () => {
		// When
		await sendContactMessage({
			name: 'Jan',
			email: 'jan@example.com',
			phone: '+48 111 222 333',
			sessionType: 'Fotografia ślubna',
			message: 'Dzień dobry, pytanie o termin.',
		});

		// Then - a drift in URL or field names would silently break the whole form
		expect(publicClientMock.post).toHaveBeenCalledWith('/contact', {
			name: 'Jan',
			email: 'jan@example.com',
			phone: '+48 111 222 333',
			sessionType: 'Fotografia ślubna',
			message: 'Dzień dobry, pytanie o termin.',
		});
	});
});
