export type Role = 'ADMIN' | 'PHOTOGRAPHER' | 'CLIENT';

export interface UserDto {
	id: string | { value: string };
	name: string;
	email: string | { value: string };
	roles: Role[];
	active: boolean;
	changePasswordOnNextLogin: boolean;
	assignedUsers: (string | { value: string })[];
}

/** Flattened user for frontend consumption */
export interface User {
	id: string;
	name: string;
	email: string;
	roles: Role[];
	isActive: boolean;
	changePasswordOnNextLogin: boolean;
	assignedUsers: string[];
}

export function normalizeUser(dto: UserDto): User {
	return {
		id: typeof dto.id === 'string' ? dto.id : dto.id.value,
		name: dto.name,
		email: typeof dto.email === 'string' ? dto.email : dto.email.value,
		roles: dto.roles,
		isActive: dto.active,
		changePasswordOnNextLogin: dto.changePasswordOnNextLogin,
		assignedUsers: dto.assignedUsers.map((u) =>
			typeof u === 'string' ? u : u.value,
		),
	};
}

export interface LoginRequest {
	email: string;
	password: string;
}

export interface LoginResponse {
	changePasswordOnNextLogin: boolean;
}

export interface CreateUserRequest {
	name: string;
	email: string;
	password?: string;
	role: Role;
}

export interface RoleRequest {
	role: Role;
}

export interface PasswordRequest {
	currentPassword?: string;
	newPassword: string;
}

export interface EmailRequest {
	newEmail: string;
}

export interface RemindPasswordRequest {
	email: string;
	token: string;
	newPassword: string;
}

export interface AssignUserRequest {
	userIdList: string[];
}
