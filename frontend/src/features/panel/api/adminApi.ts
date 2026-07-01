import { apiClient } from '@/lib/apiClient';
import type { AlbumDto } from '@/shared/types/api';
import type { UserInfo } from '../types/panel';

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

export async function getAlbumFileNames(albumId: string): Promise<string[]> {
	const response = await apiClient.get<string[]>(`/album/${albumId}/file-names`);
	return response.data;
}

export async function getAllAlbumsWithoutTtd(): Promise<AlbumDto[]> {
	const response = await apiClient.get<AlbumDto[]>('/album/all/withoutTdd');
	return response.data;
}

export async function createUser(data: {
	name: string;
	email: string;
	password: string;
	role: string;
}): Promise<UserInfo> {
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

export async function setAlbumTtd(albumId: string, ttd: string): Promise<void> {
	await apiClient.patch(`/album/${albumId}/setTtd`, { ttd });
}

export async function deleteAlbum(albumId: string): Promise<void> {
	await apiClient.delete(`/album/${albumId}/delete`);
}

export async function uploadFiles(
	albumId: string,
	files: File[],
): Promise<void> {
	const formData = new FormData();
	files.forEach((file) => formData.append('files', file));
	await apiClient.post(`/album/upload/${albumId}/files`, formData, {
		headers: { 'Content-Type': 'multipart/form-data' },
	});
}

export async function removeFiles(
	albumId: string,
	fileIds: string[],
): Promise<void> {
	await apiClient.post(`/album/${albumId}/remove`, {
		fileIdList: fileIds,
	});
}

export async function setFilesVisible(
	albumId: string,
	fileIds: string[],
	visible: boolean,
): Promise<void> {
	await apiClient.patch(
		`/album/${albumId}/files/setVisible`,
		{ idList: fileIds },
		{
			params: { visible },
		},
	);
}

export async function addWatermark(
	albumId: string,
	fileIds: string[],
	hasWatermark: boolean,
): Promise<void> {
	await apiClient.post(
		`/album/${albumId}/files/addWatermark`,
		{ filesUUIDList: fileIds },
		{
			params: { hasWatermark },
		},
	);
}

export async function swapFiles(
	sourceAlbumId: string,
	targetAlbumId: string,
	fileIds: string[],
): Promise<void> {
	await apiClient.patch(`/album/${sourceAlbumId}/album/${targetAlbumId}/swap`, {
		fileIdList: fileIds,
	});
}

export async function renameFile(
	albumId: string,
	fileId: string,
	newName: string,
): Promise<void> {
	await apiClient.put(`/album/${albumId}/rename/${fileId}`, {
		newFileName: newName,
	});
}

export function getPhotoUrl(
	albumId: string,
	fileName: string,
	width?: number,
	height?: number,
): string {
	const params = new URLSearchParams();
	if (width) params.set('width', String(width));
	if (height) params.set('height', String(height));
	const query = params.toString();
	return `/api/album/${albumId}/photo/${encodeURIComponent(fileName)}${query ? `?${query}` : ''}`;
}

export async function downloadAlbum(
	albumId: string,
	fileList: string[],
): Promise<Blob> {
	const response = await apiClient.post(
		`/album/${albumId}/download`,
		{ fileList },
		{ responseType: 'arraybuffer' },
	);
	return new Blob([response.data], { type: 'application/zip' });
}
