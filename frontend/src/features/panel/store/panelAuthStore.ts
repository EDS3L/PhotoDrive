import { create } from 'zustand';
import type { CurrentUserInfo, PanelRole } from '../types/panel';

interface PanelAuthState {
	isAuthenticated: boolean;
	user: CurrentUserInfo | null;
	role: PanelRole | null;
	/** Hasło użyte przy logowaniu — w pamięci, tylko po świeżym logowaniu, by nie
	 * prosić o nie ponownie na ekranie wymuszonej zmiany hasła. */
	loginPassword: string | null;
	setUser: (user: CurrentUserInfo) => void;
	setLoginPassword: (password: string | null) => void;
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
	loginPassword: null,
	setUser: (user) =>
		set({
			isAuthenticated: true,
			user,
			role: resolveRole(user.roles),
		}),
	setLoginPassword: (password) => set({ loginPassword: password }),
	clear: () =>
		set({ isAuthenticated: false, user: null, role: null, loginPassword: null }),
}));
