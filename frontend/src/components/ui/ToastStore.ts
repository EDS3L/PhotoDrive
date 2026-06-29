import { create } from 'zustand';

export interface Toast {
	id: string;
	message: string;
	variant: 'success' | 'error' | 'info';
}

interface ToastState {
	toasts: Toast[];
	addToast: (message: string, variant?: Toast['variant']) => void;
	removeToast: (id: string) => void;
}

export const useToastStore = create<ToastState>((set) => ({
	toasts: [],
	addToast: (message, variant = 'info') => {
		const id = Math.random().toString(36).slice(2);
		set((s) => ({ toasts: [...s.toasts, { id, message, variant }] }));
		setTimeout(() => {
			set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }));
		}, 4000);
	},
	removeToast: (id) =>
		set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}));
