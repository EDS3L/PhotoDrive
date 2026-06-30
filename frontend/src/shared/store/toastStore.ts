import { create } from 'zustand';

export type ToastVariant = 'error' | 'success' | 'info';

export interface Toast {
	id: string;
	message: string;
	variant: ToastVariant;
}

interface ToastState {
	toasts: Toast[];
	addToast: (message: string, variant?: ToastVariant) => void;
	removeToast: (id: string) => void;
}

export const useToastStore = create<ToastState>((set) => ({
	toasts: [],
	addToast: (message, variant = 'info') =>
		set((state) => ({
			toasts: [
				...state.toasts,
				{ id: crypto.randomUUID(), message, variant },
			],
		})),
	removeToast: (id) =>
		set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) })),
}));

/**
 * Skrót wywoływalny też poza Reactem (np. w globalnym handlerze błędów
 * React Query w `queryClient`). Zustand pozwala czytać/aktualizować store
 * przez `getState()` bez hooka.
 */
export const toast = {
	error: (message: string) => useToastStore.getState().addToast(message, 'error'),
	success: (message: string) =>
		useToastStore.getState().addToast(message, 'success'),
	info: (message: string) => useToastStore.getState().addToast(message, 'info'),
};
