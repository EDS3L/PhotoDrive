import { apiClient } from '@/lib/apiClient';
import type { AlbumDto } from '@/shared/types/api';
import type { UserInfo } from '../types/panel';

// Wspólne operacje na plikach albumu żyją w jednym miejscu (F.2); tu je tylko re-eksportujemy,
// żeby konsumenci panelu importowali dalej z `adminApi`.
export {
	getAlbumFileNames,
	setAlbumTtd,
	deleteAlbum,
	uploadFiles,
	removeFiles,
	setFilesVisible,
	addWatermark,
	swapFiles,
	renameFile,
	getPhotoUrl,
	downloadAlbum,
} from '@/shared/api/albumFilesApi';

export async function getAllUsers(): Promise<UserInfo[]> {
	const response = await apiClient.get<UserInfo[]>('/user/all');
	return response.data;
}

export async function getActiveUsers(): Promise<UserInfo[]> {
	const response = await apiClient.get<UserInfo[]>('/user/activeUsers');
	return response.data;
}

export async function getAllAlbums(): Promise<AlbumDto[]> {
	const response = await apiClient.get<AlbumDto[]>('/album/all');
	return response.data;
}

export async function getAllAlbumsWithoutTtd(): Promise<AlbumDto[]> {
	const response = await apiClient.get<AlbumDto[]>('/album/all/withoutTtd');
	return response.data;
}

export async function createUser(data: {
	name: string;
	email: string;
	role: string;
}): Promise<UserInfo> {
	// Hasło startowe generuje backend i wysyła mailem — nie podajemy go z formularza.
	const response = await apiClient.post<UserInfo>('/user/add', data);
	return response.data;
}

export async function activateUser(id: string, active: boolean): Promise<void> {
	await apiClient.patch(`/user/${id}/activateUser`, active);
}

export async function deactivateUser(
	id: string,
	active: boolean,
): Promise<void> {
	await apiClient.patch(`/user/${id}/deactivateUser`, active);
}

export async function assignUsersToPhotographer(
	photographerId: string,
	userIds: string[],
): Promise<void> {
	await apiClient.patch(`/user/${photographerId}/assignUsers`, {
		userIdList: userIds,
	});
}

export async function removeUsersFromPhotographer(
	photographerId: string,
	userIds: string[],
): Promise<void> {
	await apiClient.patch(`/user/${photographerId}/removeUsers`, {
		userIdList: userIds,
	});
}

export async function getPhotographerAssignedUsers(
	photographerId: string,
): Promise<UserInfo[]> {
	const response = await apiClient.get<UserInfo[]>(
		`/user/${photographerId}/assignedUsers`,
	);
	return response.data;
}

export async function createAdminAlbum(name: string): Promise<AlbumDto> {
	const response = await apiClient.post<AlbumDto>('/album/admin/create', {
		name,
	});
	return response.data;
}

export async function setAlbumPublic(
	albumId: string,
	isPublic: boolean,
): Promise<void> {
	await apiClient.patch(`/album/${albumId}/setPublic`, null, {
		params: { isPublic },
	});
}

/** Etykieta (Unicode) i kolejność zakładki portfolio; pusta etykieta = wróć do nazwy technicznej. */
export async function setAlbumDisplay(
	albumId: string,
	displayName: string | null,
	displayOrder: number,
): Promise<void> {
	await apiClient.patch(`/album/${albumId}/display`, {
		displayName,
		displayOrder,
	});
}
