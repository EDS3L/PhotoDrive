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
import {
	getAllUsers,
	getAllAlbums,
	getAllAlbumsWithoutTtd,
} from './adminApi';
import {
	getAssignedClients,
	getAssignedAlbums,
	getAssignedAlbumsWithoutTtd,
	createClient,
	createClientAlbum,
} from './photographerApi';
import {
	panelLogin,
	panelLogout,
	getMe,
	changePassword,
	changeEmail,
} from './panelAuthApi';
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
	// Listy: admin czyta wszystko, fotograf tylko swoje
	// -----------------------------------------------------------------------

	it('The admin reads every user and every album, while the photographer reads only what is assigned to them', async () => {
		// When
		await getAllUsers();
		await getAllAlbums();
		await getAssignedClients();
		await getAssignedAlbums();

		// Then - mixing these up is not a type error but a privilege leak: the photographer's
		// screen would ask for the full list and answer 403 instead of showing their clients
		expect(apiClientMock.get).toHaveBeenCalledWith('/user/all');
		expect(apiClientMock.get).toHaveBeenCalledWith('/album/all');
		expect(apiClientMock.get).toHaveBeenCalledWith('/user/getAssignedUsers');
		expect(apiClientMock.get).toHaveBeenCalledWith('/album/getAllAssignedAlbums');
	});

	it('The "without TTD" reminder lists are separate endpoints per role, so each sees only albums it may set a TTD on', async () => {
		// When
		await getAllAlbumsWithoutTtd();
		await getAssignedAlbumsWithoutTtd();

		// Then
		expect(apiClientMock.get).toHaveBeenCalledWith('/album/all/withoutTtd');
		expect(apiClientMock.get).toHaveBeenCalledWith(
			'/album/allAssignedAlbum/withoutTtd',
		);
	});

	it('A photographer creating an account always sends role CLIENT, because that is the only role they may create', async () => {
		// When - the form collects a name and an e-mail, never a role
		await createClient({ name: 'Klient', email: 'klient@example.com' });

		// Then - the backend enforces this too, but sending anything else would turn a
		// clear form into a 400 the photographer cannot explain
		expect(apiClientMock.post).toHaveBeenCalledWith('/user/add', {
			name: 'Klient',
			email: 'klient@example.com',
			role: 'CLIENT',
		});
		const body = apiClientMock.post.mock.calls[0][1] as Record<string, unknown>;
		expect(body).not.toHaveProperty('password');
	});

	it("A client album is created under its client's id, so it can never land in the portfolio by accident", async () => {
		// When
		await createClientAlbum('klient-1', 'sesja-lipiec');

		// Then - the admin route (/album/admin/create) is the publishable one; a client
		// album must go through the client path
		expect(apiClientMock.post).toHaveBeenCalledWith('/album/client/klient-1/create', {
			name: 'sesja-lipiec',
		});
	});

	// -----------------------------------------------------------------------
	// Sesja panelu i dane konta
	// -----------------------------------------------------------------------

	it('Panel login and logout use the shared auth endpoints, because the session cookie is the same one the client zone uses', async () => {
		// When
		await panelLogin({ email: 'admin@photodrive.pl', password: 'tajne' });
		await panelLogout();

		// Then
		expect(apiClientMock.post).toHaveBeenCalledWith('/auth/login', {
			email: 'admin@photodrive.pl',
			password: 'tajne',
		});
		expect(apiClientMock.post).toHaveBeenCalledWith('/auth/logout');
	});

	it('The panel identity probe is NOT silent, so an expired cookie sends the user to the login screen', async () => {
		// When
		await getMe();

		// Then - unlike the client zone probe, no skipAuthRedirect flag: in the panel a 401
		// must trigger the interceptor redirect rather than render an empty panel
		expect(apiClientMock.get).toHaveBeenCalledWith('/user/me');
		expect(apiClientMock.get.mock.calls[0]).toHaveLength(1);
	});

	it('Changing a password sends the current one alongside the new one, which is what proves it is the owner asking', async () => {
		// When
		await changePassword('user-1', 'stare', 'nowe');

		// Then
		expect(apiClientMock.patch).toHaveBeenCalledWith('/user/user-1/changePassword', {
			currentPassword: 'stare',
			newPassword: 'nowe',
		});
	});

	it('Changing an e-mail targets its own endpoint, so it cannot be mistaken for a password change', async () => {
		// When
		await changeEmail('user-1', 'nowy@example.com');

		// Then
		expect(apiClientMock.patch).toHaveBeenCalledWith('/user/user-1/changeEmail', {
			newEmail: 'nowy@example.com',
		});
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
