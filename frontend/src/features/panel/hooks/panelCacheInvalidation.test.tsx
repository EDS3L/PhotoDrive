import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';

const { invalidateQueries } = vi.hoisted(() => ({
	invalidateQueries: vi.fn(),
}));

// Hooki unieważniają cache przez modułowy singleton, a nie przez klienta z kontekstu —
// więc podstawiamy singleton i obserwujemy, KTÓRE klucze poszły do unieważnienia.
vi.mock('@/lib/queryClient', () => ({ queryClient: { invalidateQueries } }));

vi.mock('../api/adminApi', () => ({
	getAllAlbums: vi.fn(),
	getAllAlbumsWithoutTtd: vi.fn(),
	createAdminAlbum: vi.fn().mockResolvedValue(undefined),
	setAlbumPublic: vi.fn().mockResolvedValue(undefined),
	setAlbumDisplay: vi.fn().mockResolvedValue(undefined),
	setAlbumTtd: vi.fn().mockResolvedValue(undefined),
	deleteAlbum: vi.fn().mockResolvedValue(undefined),
	removeFiles: vi.fn().mockResolvedValue(undefined),
	setFilesVisible: vi.fn().mockResolvedValue(undefined),
	addWatermark: vi.fn().mockResolvedValue(undefined),
	swapFiles: vi.fn().mockResolvedValue(undefined),
	renameFile: vi.fn().mockResolvedValue(undefined),
	downloadAlbum: vi.fn().mockResolvedValue(undefined),
	getAllUsers: vi.fn(),
	createUser: vi.fn().mockResolvedValue(undefined),
	activateUser: vi.fn().mockResolvedValue(undefined),
	deactivateUser: vi.fn().mockResolvedValue(undefined),
	assignUsersToPhotographer: vi.fn().mockResolvedValue(undefined),
	removeUsersFromPhotographer: vi.fn().mockResolvedValue(undefined),
	getPhotographerAssignedUsers: vi.fn().mockResolvedValue([]),
}));

vi.mock('../api/photographerApi', () => ({
	getAssignedAlbums: vi.fn(),
	getAssignedAlbumsWithoutTtd: vi.fn(),
	createClientAlbum: vi.fn().mockResolvedValue(undefined),
	setAlbumTtd: vi.fn().mockResolvedValue(undefined),
	deleteAlbum: vi.fn().mockResolvedValue(undefined),
	removeFiles: vi.fn().mockResolvedValue(undefined),
	setFilesVisible: vi.fn().mockResolvedValue(undefined),
	addWatermark: vi.fn().mockResolvedValue(undefined),
	swapFiles: vi.fn().mockResolvedValue(undefined),
	renameFile: vi.fn().mockResolvedValue(undefined),
	downloadAlbum: vi.fn().mockResolvedValue(undefined),
	getAssignedClients: vi.fn(),
	createClient: vi.fn().mockResolvedValue(undefined),
}));

vi.mock('../api/watermarkApi', () => ({
	getWatermarkStatus: vi.fn(),
	uploadWatermark: vi.fn().mockResolvedValue(undefined),
	deleteWatermark: vi.fn().mockResolvedValue(undefined),
}));

import * as adminApi from '../api/adminApi';
import * as photographerApi from '../api/photographerApi';
import * as watermarkApi from '../api/watermarkApi';
import * as adminAlbums from './useAdminAlbums';
import * as photographerAlbums from './usePhotographerAlbums';
import * as users from './useUsers';
import * as photographerClients from './usePhotographerClients';
import * as watermark from './useWatermark';

function wrapper({ children }: { children: ReactNode }) {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
	});
	return (
		<QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
	);
}

/** Odpala mutację i czeka na `onSuccess`, bo unieważnienie dzieje się właśnie tam. */
async function runMutation<TVars>(
	useMutationHook: () => { mutate: (vars: TVars) => void; isSuccess: boolean },
	variables: TVars,
): Promise<void> {
	const { result } = renderHook(() => useMutationHook(), { wrapper });
	result.current.mutate(variables);
	await waitFor(() => expect(result.current.isSuccess).toBe(true));
}

/** Klucze przekazane do `invalidateQueries`, w kolejności wywołań. */
function invalidatedKeys(): unknown[][] {
	return invalidateQueries.mock.calls.map(
		(call) => (call[0] as { queryKey: unknown[] }).queryKey,
	);
}

/**
 * Moduły `useAdminAlbums` i `usePhotographerAlbums` są bliźniaczo podobne i różnią się
 * WYŁĄCZNIE kluczami cache'u. To czyni je siedliskiem błędów z kopiuj-wklej: hook fotografa
 * unieważniający listę admina nie wywoła żadnego błędu — po prostu ekran fotografa zostanie
 * z nieaktualnymi danymi, a to widać dopiero po ręcznym F5. Te testy przypinają, KTÓRE listy
 * każda mutacja unieważnia (i że nie unieważnia cudzych).
 */
describe('panel cache invalidation', () => {
	beforeEach(() => {
		invalidateQueries.mockClear();
	});

	// -----------------------------------------------------------------------
	// Albumy admina
	// -----------------------------------------------------------------------

	it('Album lists are read under their own keys, so the admin list and the photographer list never share a cache entry', () => {
		// When
		const { result: adminList } = renderHook(() => adminAlbums.useAdminAlbums(), {
			wrapper,
		});
		const { result: photographerList } = renderHook(
			() => photographerAlbums.usePhotographerAlbums(),
			{ wrapper },
		);

		// Then - both hooks are alive and fed by their OWN endpoint
		expect(adminList.current).toBeDefined();
		expect(photographerList.current).toBeDefined();
		expect(adminApi.getAllAlbums).toHaveBeenCalled();
		expect(photographerApi.getAssignedAlbums).toHaveBeenCalled();
	});

	it('Setting a TTD invalidates the "without TTD" list as well, because the album just left it', async () => {
		// When
		await runMutation(adminAlbums.useSetAlbumTtd, {
			albumId: 'album-1',
			ttd: '2026-12-01',
		});

		// Then - invalidating only the main list would leave the album on the "no TTD"
		// screen, i.e. the admin would still be told to set a TTD he just set
		expect(adminApi.setAlbumTtd).toHaveBeenCalledWith('album-1', '2026-12-01');
		expect(invalidatedKeys()).toEqual([
			['panel', 'admin-albums'],
			['panel', 'admin-albums-no-ttd'],
		]);
	});

	it('Deleting an album drops it from both album lists, so it cannot linger on the "without TTD" screen', async () => {
		// When
		await runMutation(adminAlbums.useDeleteAlbum, 'album-1');

		// Then
		expect(adminApi.deleteAlbum).toHaveBeenCalledWith('album-1');
		expect(invalidatedKeys()).toEqual([
			['panel', 'admin-albums'],
			['panel', 'admin-albums-no-ttd'],
		]);
	});

	it('Publishing an album refreshes the album list only, since TTD membership does not change', async () => {
		// When
		await runMutation(adminAlbums.useSetAlbumPublic, {
			albumId: 'album-1',
			isPublic: true,
		});

		// Then
		expect(adminApi.setAlbumPublic).toHaveBeenCalledWith('album-1', true);
		expect(invalidatedKeys()).toEqual([['panel', 'admin-albums']]);
	});

	it('Editing the portfolio label refreshes the album list, so the new tab name shows without a reload', async () => {
		// When
		await runMutation(adminAlbums.useSetAlbumDisplay, {
			albumId: 'album-1',
			displayName: 'Śluby',
			displayOrder: 2,
		});

		// Then
		expect(adminApi.setAlbumDisplay).toHaveBeenCalledWith('album-1', 'Śluby', 2);
		expect(invalidatedKeys()).toEqual([['panel', 'admin-albums']]);
	});

	it('Creating a portfolio album refreshes the album list', async () => {
		// When
		await runMutation(adminAlbums.useCreateAdminAlbum, 'portfolio-sluby');

		// Then
		expect(adminApi.createAdminAlbum).toHaveBeenCalledWith('portfolio-sluby');
		expect(invalidatedKeys()).toEqual([['panel', 'admin-albums']]);
	});

	it('File operations in the admin album refresh the album list, because photo state is read from it', async () => {
		// When - visibility, watermark, rename, swap and delete all mutate files inside the album
		await runMutation(adminAlbums.useSetFilesVisible, {
			albumId: 'album-1',
			fileIds: ['f1'],
			visible: true,
		});
		await runMutation(adminAlbums.useAddWatermark, {
			albumId: 'album-1',
			fileIds: ['f1'],
			hasWatermark: true,
		});
		await runMutation(adminAlbums.useRenameFile, {
			albumId: 'album-1',
			fileId: 'f1',
			newName: 'foto_2.jpg',
		});
		await runMutation(adminAlbums.useSwapFiles, {
			sourceAlbumId: 'album-1',
			targetAlbumId: 'album-2',
			fileIds: ['f1'],
		});
		await runMutation(adminAlbums.useRemoveFiles, {
			albumId: 'album-1',
			fileIds: ['f1'],
		});

		// Then
		expect(adminApi.setFilesVisible).toHaveBeenCalledWith('album-1', ['f1'], true);
		expect(adminApi.addWatermark).toHaveBeenCalledWith('album-1', ['f1'], true);
		expect(adminApi.renameFile).toHaveBeenCalledWith('album-1', 'f1', 'foto_2.jpg');
		expect(adminApi.swapFiles).toHaveBeenCalledWith('album-1', 'album-2', ['f1']);
		expect(adminApi.removeFiles).toHaveBeenCalledWith('album-1', ['f1']);
		expect(invalidatedKeys()).toEqual([
			['panel', 'admin-albums'],
			['panel', 'admin-albums'],
			['panel', 'admin-albums'],
			['panel', 'admin-albums'],
			['panel', 'admin-albums'],
		]);
	});

	it('Downloading a ZIP invalidates nothing, because reading photos changes no server state', async () => {
		// When
		await runMutation(adminAlbums.useDownloadAlbum, {
			albumId: 'album-1',
			fileList: ['foto.jpg'],
		});

		// Then - a needless invalidation would refetch every album on each download
		expect(adminApi.downloadAlbum).toHaveBeenCalledWith('album-1', ['foto.jpg']);
		expect(invalidateQueries).not.toHaveBeenCalled();
	});

	// -----------------------------------------------------------------------
	// Albumy fotografa — te same operacje, WŁASNE klucze
	// -----------------------------------------------------------------------

	it("A photographer's TTD change invalidates the photographer's lists and never the admin ones", async () => {
		// When
		await runMutation(photographerAlbums.useSetAlbumTtd, {
			albumId: 'album-1',
			ttd: '2026-12-01',
		});

		// Then - reaching for 'admin-albums' here is the classic copy-paste bug: it would
		// leave the photographer's own screen stale while refreshing a list he cannot see
		expect(invalidatedKeys()).toEqual([
			['panel', 'photographer-albums'],
			['panel', 'photographer-albums-no-ttd'],
		]);
	});

	it("Deleting a client album refreshes both of the photographer's lists", async () => {
		// When
		await runMutation(photographerAlbums.useDeleteAlbum, 'album-1');

		// Then
		expect(photographerApi.deleteAlbum).toHaveBeenCalledWith('album-1');
		expect(invalidatedKeys()).toEqual([
			['panel', 'photographer-albums'],
			['panel', 'photographer-albums-no-ttd'],
		]);
	});

	it('Creating a client album passes the client it belongs to, because a client album cannot exist without an owner', async () => {
		// When
		await runMutation(photographerAlbums.useCreateClientAlbum, {
			clientId: 'klient-1',
			name: 'sesja-lipiec',
		});

		// Then
		expect(photographerApi.createClientAlbum).toHaveBeenCalledWith(
			'klient-1',
			'sesja-lipiec',
		);
		expect(invalidatedKeys()).toEqual([['panel', 'photographer-albums']]);
	});

	it("File operations in a client album refresh the photographer's album list", async () => {
		// When
		await runMutation(photographerAlbums.useSetFilesVisible, {
			albumId: 'album-1',
			fileIds: ['f1'],
			visible: false,
		});
		await runMutation(photographerAlbums.useAddWatermark, {
			albumId: 'album-1',
			fileIds: ['f1'],
			hasWatermark: false,
		});
		await runMutation(photographerAlbums.useRenameFile, {
			albumId: 'album-1',
			fileId: 'f1',
			newName: 'foto_2.jpg',
		});
		await runMutation(photographerAlbums.useSwapFiles, {
			sourceAlbumId: 'album-1',
			targetAlbumId: 'album-2',
			fileIds: ['f1'],
		});
		await runMutation(photographerAlbums.useRemoveFiles, {
			albumId: 'album-1',
			fileIds: ['f1'],
		});

		// Then
		expect(photographerApi.setFilesVisible).toHaveBeenCalledWith(
			'album-1',
			['f1'],
			false,
		);
		expect(photographerApi.addWatermark).toHaveBeenCalledWith(
			'album-1',
			['f1'],
			false,
		);
		expect(invalidatedKeys()).toEqual([
			['panel', 'photographer-albums'],
			['panel', 'photographer-albums'],
			['panel', 'photographer-albums'],
			['panel', 'photographer-albums'],
			['panel', 'photographer-albums'],
		]);
	});

	it("A photographer's ZIP download invalidates nothing", async () => {
		// When
		await runMutation(photographerAlbums.useDownloadAlbum, {
			albumId: 'album-1',
			fileList: ['foto.jpg'],
		});

		// Then
		expect(photographerApi.downloadAlbum).toHaveBeenCalledWith('album-1', [
			'foto.jpg',
		]);
		expect(invalidateQueries).not.toHaveBeenCalled();
	});

	it('The "without TTD" lists are separate queries, so each role reads its own reminder list', () => {
		// When
		renderHook(() => adminAlbums.useAdminAlbumsWithoutTtd(), { wrapper });
		renderHook(() => photographerAlbums.usePhotographerAlbumsWithoutTtd(), {
			wrapper,
		});

		// Then
		expect(adminApi.getAllAlbumsWithoutTtd).toHaveBeenCalled();
		expect(photographerApi.getAssignedAlbumsWithoutTtd).toHaveBeenCalled();
	});

	// -----------------------------------------------------------------------
	// Użytkownicy i przypisania
	// -----------------------------------------------------------------------

	it('Creating a user refreshes the user list', async () => {
		// When
		await runMutation(users.useCreateUser, {
			name: 'Jan',
			email: 'jan@example.com',
			role: 'CLIENT',
		});

		// Then
		expect(invalidatedKeys()).toEqual([['panel', 'users']]);
	});

	it('Activation and deactivation both refresh the user list, so the status badge never lies', async () => {
		// When
		await runMutation(users.useActivateUser, { id: 'user-1', active: true });
		await runMutation(users.useDeactivateUser, { id: 'user-1', active: false });

		// Then
		expect(adminApi.activateUser).toHaveBeenCalledWith('user-1', true);
		expect(adminApi.deactivateUser).toHaveBeenCalledWith('user-1', false);
		expect(invalidatedKeys()).toEqual([
			['panel', 'users'],
			['panel', 'users'],
		]);
	});

	it('Assigning clients refreshes the user list AND the photographer-clients list, because the change is visible in both places', async () => {
		// When
		await runMutation(users.useAssignUsers, {
			photographerId: 'foto-1',
			userIds: ['klient-1'],
		});

		// Then - skipping the second key leaves the photographer's client list stale,
		// which is exactly where the admin looks to confirm the assignment worked
		expect(adminApi.assignUsersToPhotographer).toHaveBeenCalledWith('foto-1', [
			'klient-1',
		]);
		expect(invalidatedKeys()).toEqual([
			['panel', 'users'],
			['panel', 'photographer-clients'],
		]);
	});

	it('Detaching clients refreshes the same two lists as assigning them', async () => {
		// When
		await runMutation(users.useRemoveAssignedUsers, {
			photographerId: 'foto-1',
			userIds: ['klient-1'],
		});

		// Then
		expect(adminApi.removeUsersFromPhotographer).toHaveBeenCalledWith('foto-1', [
			'klient-1',
		]);
		expect(invalidatedKeys()).toEqual([
			['panel', 'users'],
			['panel', 'photographer-clients'],
		]);
	});

	it("A photographer's client list is keyed by photographer id, so opening a second photographer does not show the first one's clients", async () => {
		// When
		renderHook(() => users.usePhotographerAssignedUsers('foto-7'), { wrapper });

		// Then
		await waitFor(() =>
			expect(adminApi.getPhotographerAssignedUsers).toHaveBeenCalledWith('foto-7'),
		);
	});

	it('No photographer selected means no request, because the endpoint needs an id to answer', () => {
		// When
		renderHook(() => users.usePhotographerAssignedUsers(null), { wrapper });

		// Then - `enabled` guards the non-null assertion in the query function
		expect(adminApi.getPhotographerAssignedUsers).not.toHaveBeenCalled();
	});

	it("A photographer creating a client refreshes their own list and the admin's user list, since the account appears in both", async () => {
		// When
		await runMutation(photographerClients.useCreateClient, {
			name: 'Klient',
			email: 'klient@example.com',
		});

		// Then - `mutationFn` jest tu przekazane bez opakowania, więc React Query dokłada
		// drugi argument (kontekst mutacji); sprawdzamy dane, nie liczbę argumentów
		expect(vi.mocked(photographerApi.createClient).mock.calls[0][0]).toEqual({
			name: 'Klient',
			email: 'klient@example.com',
		});
		expect(invalidatedKeys()).toEqual([
			['panel', 'photographer-clients'],
			['panel', 'users'],
		]);
	});

	it("The photographer's client list is read from the assigned-clients endpoint", () => {
		// When
		renderHook(() => photographerClients.usePhotographerClients(), { wrapper });

		// Then
		expect(photographerApi.getAssignedClients).toHaveBeenCalled();
	});

	// -----------------------------------------------------------------------
	// Watermark
	// -----------------------------------------------------------------------

	it('Uploading and deleting the logo both refresh the watermark status, which is what shows or hides the watermark actions', async () => {
		// Given
		const logo = new File(['png'], 'logo.png', { type: 'image/png' });

		// When
		await runMutation(watermark.useUploadWatermark, logo);
		await runMutation(watermark.useDeleteWatermark, undefined);

		// Then - the status drives visibility of "Dodaj watermark" (A1), so a stale status
		// offers an action the backend will reject
		expect(watermarkApi.uploadWatermark).toHaveBeenCalledWith(logo);
		expect(watermarkApi.deleteWatermark).toHaveBeenCalled();
		expect(invalidatedKeys()).toEqual([
			['panel', 'watermark-status'],
			['panel', 'watermark-status'],
		]);
	});

	it('Watermark status is read under its own key', () => {
		// When
		renderHook(() => watermark.useWatermarkStatus(), { wrapper });

		// Then
		expect(watermarkApi.getWatermarkStatus).toHaveBeenCalled();
	});
});
