import { create } from 'zustand';

interface ClientSession {
	email: string;
	userId: string;
	mustChangePassword: boolean;
	/** Hasło użyte przy logowaniu — trzymane w pamięci tylko po świeżym logowaniu
	 * (nie po F5), by nie prosić o nie ponownie na ekranie wymuszonej zmiany. */
	loginPassword?: string;
}

interface AuthState {
	isAuthenticated: boolean;
	email: string | null;
	userId: string | null;
	mustChangePassword: boolean;
	loginPassword: string | null;
	setSession: (session: ClientSession) => void;
	/** Zmiana hasła zakończona — zdejmujemy flagę i czyścimy hasło z pamięci. */
	completePasswordChange: () => void;
	clear: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
	isAuthenticated: false,
	email: null,
	userId: null,
	mustChangePassword: false,
	loginPassword: null,
	setSession: ({ email, userId, mustChangePassword, loginPassword }) =>
		set({
			isAuthenticated: true,
			email,
			userId,
			mustChangePassword,
			loginPassword: loginPassword ?? null,
		}),
	completePasswordChange: () =>
		set({ mustChangePassword: false, loginPassword: null }),
	clear: () =>
		set({
			isAuthenticated: false,
			email: null,
			userId: null,
			mustChangePassword: false,
			loginPassword: null,
		}),
}));
