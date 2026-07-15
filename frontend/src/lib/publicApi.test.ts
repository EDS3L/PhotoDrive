import { describe, it, expect } from 'vitest';
import { getPublicPhotoUrl, PUBLIC_PHOTO_SIZE } from './publicApi';

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
