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
