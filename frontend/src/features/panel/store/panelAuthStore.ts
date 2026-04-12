import { create } from 'zustand';
import type { CurrentUserInfo, PanelRole } from '../types/panel';

interface PanelAuthState {
	isAuthenticated: boolean;
	user: CurrentUserInfo | null;
	role: PanelRole | null;
	setUser: (user: CurrentUserInfo) => void;
	clear: () => void;
}

function resolveRole(roles: string[]): PanelRole | null {
	if (roles.includes('ADMIN')) return 'ADMIN';
	if (roles.includes('PHOTOGRAPHER')) return 'PHOTOGRAPHER';
	return null;
}

export const usePanelAuthStore = create<PanelAuthState>((set) => ({
	isAuthenticated: false,
	user: null,
	role: null,
	setUser: (user) =>
		set({
			isAuthenticated: true,
			user,
			role: resolveRole(user.roles),
		}),
	clear: () => set({ isAuthenticated: false, user: null, role: null }),
}));
