export type PanelRole = 'ADMIN' | 'PHOTOGRAPHER';

export interface CurrentUserInfo {
	id: string;
	name: string;
	email: string;
	roles: string[];
}

export interface UserInfo {
	id: { value: string };
	name: string;
	email: { value: string };
	roles: string[];
	isActive: boolean;
	changePasswordOnNextLogin: boolean;
	assignedUsers: { value: string }[];
}

export interface AdminStats {
	totalUsers: number;
	totalAlbums: number;
	totalPhotos: number;
	activeUsers: number;
	albumsWithoutTtd: number;
}
