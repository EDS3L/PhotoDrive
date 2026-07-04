export type PanelRole = 'ADMIN' | 'PHOTOGRAPHER';

export interface CurrentUserInfo {
	id: string;
	name: string;
	email: string;
	roles: string[];
	changePasswordOnNextLogin: boolean;
}

export interface UserInfo {
	id: string;
	name: string;
	email: string;
	roles: string[];
	isActive: boolean;
	changePasswordOnNextLogin: boolean;
	assignedUsers: string[];
}

export interface AdminStats {
	totalUsers: number;
	totalAlbums: number;
	totalPhotos: number;
	activeUsers: number;
	albumsWithoutTtd: number;
}
