import { describe, it, expect, vi, beforeEach } from 'vitest';

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
	setAlbumPublic,
	setAlbumDisplay,
	createAdminAlbum,
	assignUsersToPhotographer,
	removeUsersFromPhotographer,
	getPhotographerAssignedUsers,
	activateUser,
	deactivateUser,
	createUser,
} from './adminApi';
import { uploadWatermark, getWatermarkImageUrl } from './watermarkApi';
import { uploadSiteSlotImage, getSiteSlotPreviewUrl } from './siteSlotsApi';

/**
 * Kontrakt z backendem utrzymywany jest RĘCZNIE (nie ma generacji z OpenAPI — B.2), więc
 * literówka w URL-u albo parametr wysłany w ciele zamiast w query jest błędem <b>cichym</b>:
 * TypeScript go nie zobaczy, a objawi się dopiero jako 400/404 w działającej aplikacji.
 * Te testy przypinają dokładne adresy i kształt żądań.
 */
describe('panel API contract', () => {
	beforeEach(() => {
		apiClientMock.get.mockReset().mockResolvedValue({ data: [] });
		apiClientMock.post.mockReset().mockResolvedValue({ data: {} });
		apiClientMock.patch.mockReset().mockResolvedValue({ data: undefined });
		apiClientMock.put.mockReset().mockResolvedValue({ data: undefined });
		apiClientMock.delete.mockReset().mockResolvedValue({ data: undefined });
	});

	it('Publishing an album sends isPublic as a query parameter, because the endpoint reads it from the query and not from the body', async () => {
		// When
		await setAlbumPublic('album-1', true);

		// Then - passing it in the body would leave the flag unset and the album unpublished
		expect(apiClientMock.patch).toHaveBeenCalledWith(
			'/album/album-1/setPublic',
			null,
			{ params: { isPublic: true } },
		);
	});

	it('Clearing a portfolio label sends null rather than an empty string, so the backend falls back to the technical name', async () => {
		// When - the admin empties the label field
		await setAlbumDisplay('album-1', null, 3);

		// Then
		expect(apiClientMock.patch).toHaveBeenCalledWith('/album/album-1/display', {
			displayName: null,
			displayOrder: 3,
		});
	});

	it('A portfolio label keeps full Unicode, because the label is not a path and diacritics are the whole point of having it', async () => {
		// When
		await setAlbumDisplay('album-1', 'Śluby', 1);

		// Then - the technical album name stays ASCII; only displayName carries "Śluby" (8.4)
		expect(apiClientMock.patch).toHaveBeenCalledWith('/album/album-1/display', {
			displayName: 'Śluby',
			displayOrder: 1,
		});
	});

	it('Creating a user never sends a password, because the start password is generated server-side', async () => {
		// When
		await createUser({ name: 'Jan', email: 'jan@example.com', role: 'CLIENT' });

		// Then - a password field here would reintroduce plaintext choosing (A5)
		expect(apiClientMock.post).toHaveBeenCalledWith('/user/add', {
			name: 'Jan',
			email: 'jan@example.com',
			role: 'CLIENT',
		});
		const body = apiClientMock.post.mock.calls[0][1] as Record<string, unknown>;
		expect(body).not.toHaveProperty('password');
	});

	it('Activation and deactivation hit their own endpoints, so the two are not silently interchangeable', async () => {
		// When
		await activateUser('user-1', true);
		await deactivateUser('user-2', false);

		// Then
		expect(apiClientMock.patch).toHaveBeenCalledWith('/user/user-1/activateUser', true);
		expect(apiClientMock.patch).toHaveBeenCalledWith('/user/user-2/deactivateUser', false);
	});

	it('Assigning and detaching clients both wrap the ids in userIdList, matching the request record on the backend', async () => {
		// When
		await assignUsersToPhotographer('foto-1', ['klient-1', 'klient-2']);
		await removeUsersFromPhotographer('foto-1', ['klient-1']);

		// Then - a bare array would deserialize to an empty list and silently assign nobody
		expect(apiClientMock.patch).toHaveBeenCalledWith('/user/foto-1/assignUsers', {
			userIdList: ['klient-1', 'klient-2'],
		});
		expect(apiClientMock.patch).toHaveBeenCalledWith('/user/foto-1/removeUsers', {
			userIdList: ['klient-1'],
		});
	});

	it("Reading a photographer's clients targets that photographer's id, so the admin never sees somebody else's list", async () => {
		// When
		await getPhotographerAssignedUsers('foto-7');

		// Then
		expect(apiClientMock.get).toHaveBeenCalledWith('/user/foto-7/assignedUsers');
	});

	it('Creating a portfolio album posts to the admin route, which is the only one allowed to make an album publishable', async () => {
		// When
		await createAdminAlbum('portfolio-sluby');

		// Then
		expect(apiClientMock.post).toHaveBeenCalledWith('/album/admin/create', {
			name: 'portfolio-sluby',
		});
	});

	it('The platform logo is uploaded as multipart under the field name the backend reads', async () => {
		// Given
		const logo = new File(['png'], 'logo.png', { type: 'image/png' });

		// When
		await uploadWatermark(logo);

		// Then - a mismatched field name fails as "required part missing", not as a type error
		const [url, body, config] = apiClientMock.put.mock.calls[0];
		expect(url).toBe('/watermark');
		expect((body as FormData).get('file')).toBe(logo);
		expect(config).toEqual({
			headers: { 'Content-Type': 'multipart/form-data' },
		});
	});

	it('A section photo is uploaded to its own slot, so choosing one section cannot overwrite another', async () => {
		// Given
		const photo = new File(['jpg'], 'hero.jpg', { type: 'image/jpeg' });

		// When
		await uploadSiteSlotImage('ABOUT_BIO', photo);

		// Then
		const [url, body] = apiClientMock.put.mock.calls[0];
		expect(url).toBe('/site/slots/ABOUT_BIO');
		expect((body as FormData).get('file')).toBe(photo);
	});

	// -----------------------------------------------------------------------
	// URL-e z wersją (cache-busting)
	// -----------------------------------------------------------------------

	it('The logo preview URL carries the version, because the response is cached and only a changed URL refetches it', () => {
		// When
		const url = getWatermarkImageUrl('2026-07-25T10:00:00Z');

		// Then
		expect(url).toBe('/api/watermark?v=2026-07-25T10%3A00%3A00Z');
	});

	it('Without a version the logo URL stays bare, so no literal "null" is appended', () => {
		// When / Then
		expect(getWatermarkImageUrl(null)).toBe('/api/watermark');
	});

	it('The slot preview URL is version-stamped as well, since the public endpoint answers with immutable', () => {
		// When
		const url = getSiteSlotPreviewUrl('HOME_HERO', '2026-07-25T10:00:00Z');

		// Then - the timestamp must be encoded; a raw colon in a query value is fragile
		expect(url).toBe(
			'/api/public/site/photo/HOME_HERO?v=2026-07-25T10%3A00%3A00Z',
		);
	});
});
