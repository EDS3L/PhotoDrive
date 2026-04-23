import { create } from 'zustand';

export type Role = 'ADMIN' | 'PHOTOGRAPHER' | 'CLIENT';

export interface AuthUser {
	id: string;
	name: string;
	email: string;
	roles: Role[];
	isActive: boolean;
	changePasswordOnNextLogin: boolean;
}

interface AuthState {
	user: AuthUser | null;
	isAuthenticated: boolean;
	isLoading: boolean;
	setUser: (user: AuthUser | null) => void;
	setLoading: (loading: boolean) => void;
	logout: () => void;
	hasRole: (role: Role) => boolean;
	primaryRole: () => Role | null;
}

export const useAuthStore = create<AuthState>((set, get) => ({
	user: null,
	isAuthenticated: false,
	isLoading: true,

	setUser: (user) =>
		set({
			user,
			isAuthenticated: user !== null,
			isLoading: false,
		}),

	setLoading: (isLoading) => set({ isLoading }),

	logout: () =>
		set({
			user: null,
			isAuthenticated: false,
			isLoading: false,
		}),

	hasRole: (role) => {
		const { user } = get();
		return user?.roles.includes(role) ?? false;
	},

	primaryRole: () => {
		const { user } = get();
		if (!user) return null;
		if (user.roles.includes('ADMIN')) return 'ADMIN';
		if (user.roles.includes('PHOTOGRAPHER')) return 'PHOTOGRAPHER';
		if (user.roles.includes('CLIENT')) return 'CLIENT';
		return null;
	},
}));
