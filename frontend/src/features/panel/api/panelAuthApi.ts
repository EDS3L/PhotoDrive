import { apiClient } from '@/lib/apiClient';
import type { LoginRequest } from '@/shared/types/api';
import type { CurrentUserInfo } from '../types/panel';

export async function panelLogin(data: LoginRequest): Promise<void> {
	await apiClient.post('/auth/login', data);
}

export async function panelLogout(): Promise<void> {
	await apiClient.post('/auth/logout');
}

export async function getMe(): Promise<CurrentUserInfo> {
	const response = await apiClient.get<CurrentUserInfo>('/user/me');
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

export async function changeEmail(
	userId: string,
	newEmail: string,
): Promise<void> {
	await apiClient.patch(`/user/${userId}/changeEmail`, { newEmail });
}
