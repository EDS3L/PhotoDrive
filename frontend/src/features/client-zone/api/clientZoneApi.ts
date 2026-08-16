import { apiClient } from '@/lib/apiClient';
import type { AlbumDto, LoginRequest } from '@/shared/types/api';

export {
	getPhotoUrl,
	downloadAlbum as downloadAlbumZip,
} from '@/shared/api/albumFilesApi';

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
	changePasswordOnNextLogin: boolean;
}

export async function getCurrentUser(): Promise<CurrentUser> {
	const response = await apiClient.get<CurrentUser>('/user/me', {
		skipAuthRedirect: true,
	});
	return response.data;
}

export async function changePassword(
	userId: string,
	currentPassword: string,
	newPassword: string,
): Promise<void> {
	await apiClient.patch(`/user/${userId}/changePassword`, {
		currentPassword,
		newPassword,
	});
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

