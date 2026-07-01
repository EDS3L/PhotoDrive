import { apiClient } from '@/lib/apiClient';
import type { AlbumDto, LoginRequest } from '@/shared/types/api';

export async function login(data: LoginRequest): Promise<void> {
	await apiClient.post('/auth/login', data);
}

export async function logout(): Promise<void> {
	await apiClient.post('/auth/logout');
}

export interface CurrentUser {
	id: string;
	name: string;
	email: string;
	roles: string[];
}

/** Ciche sprawdzenie sesji z cookie (bez redirectu przy 401). */
export async function getCurrentUser(): Promise<CurrentUser> {
	const response = await apiClient.get<CurrentUser>('/user/me', {
		skipAuthRedirect: true,
	});
	return response.data;
}

export async function getAssignedAlbums(): Promise<AlbumDto[]> {
	const response = await apiClient.get<AlbumDto[]>(
		'/album/getAllAssignedAlbums',
	);
	return response.data;
}

export async function requestPasswordToken(email: string): Promise<void> {
	await apiClient.post(
		`/auth/create/passwordToken/${encodeURIComponent(email)}`,
	);
}

export async function resetPassword(
	email: string,
	token: string,
	newPassword: string,
): Promise<void> {
	await apiClient.post('/auth/remindPassword', {
		email,
		token,
		newPassword,
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

export function getDownloadUrl(albumId: string): string {
	return `/api/album/${albumId}/download`;
}

export async function downloadAlbumZip(
	albumId: string,
	fileNames: string[],
): Promise<void> {
	const response = await apiClient.post(
		`/album/${albumId}/download`,
		{ fileList: fileNames },
		{ responseType: 'blob' },
	);
	const blob = new Blob([response.data], { type: 'application/zip' });
	const url = window.URL.createObjectURL(blob);
	const a = document.createElement('a');
	a.href = url;
	a.download = `${albumId}.zip`;
	document.body.appendChild(a);
	a.click();
	a.remove();
	window.URL.revokeObjectURL(url);
}
