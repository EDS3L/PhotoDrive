import { create } from 'zustand';

interface AuthState {
	isAuthenticated: boolean;
	email: string | null;
	setAuthenticated: (authenticated: boolean, email?: string) => void;
	clear: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
	isAuthenticated: false,
	email: null,
	setAuthenticated: (authenticated, email) =>
		set({ isAuthenticated: authenticated, email: email ?? null }),
	clear: () => set({ isAuthenticated: false, email: null }),
}));
