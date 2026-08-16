import { apiClient } from '@/lib/apiClient';
import type { AlbumDto } from '@/shared/types/api';
import type { UserInfo } from '../types/panel';

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

export async function getAssignedClients(): Promise<UserInfo[]> {
	const response = await apiClient.get<UserInfo[]>('/user/getAssignedUsers');
	return response.data;
}

export async function createClient(data: {
	name: string;
	email: string;
}): Promise<UserInfo> {
	const response = await apiClient.post<UserInfo>('/user/add', {
		...data,
		role: 'CLIENT',
	});
	return response.data;
}

export async function getAssignedAlbums(): Promise<AlbumDto[]> {
	const response = await apiClient.get<AlbumDto[]>(
		'/album/getAllAssignedAlbums',
	);
	return response.data;
}

export async function getAssignedAlbumsWithoutTtd(): Promise<AlbumDto[]> {
	const response = await apiClient.get<AlbumDto[]>(
		'/album/allAssignedAlbum/withoutTtd',
	);
	return response.data;
}

export async function createClientAlbum(
	clientId: string,
	name: string,
): Promise<AlbumDto> {
	const response = await apiClient.post<AlbumDto>(
		`/album/client/${clientId}/create`,
		{ name },
	);
	return response.data;
}
