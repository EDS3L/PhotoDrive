import { create } from 'zustand';

interface Toast {
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

const VARIANT_STYLES = {
	success: 'border-l-2 border-success text-success',
	error: 'border-l-2 border-error text-error',
	info: 'border-l-2 border-primary text-primary',
} as const;

export function ToastContainer() {
	const toasts = useToastStore((s) => s.toasts);
	const removeToast = useToastStore((s) => s.removeToast);

	if (toasts.length === 0) return null;

	return (
		<div className='fixed bottom-6 right-6 z-[100] flex flex-col gap-2'>
			{toasts.map((toast) => (
				<div
					key={toast.id}
					className={`bg-surface-container p-4 min-w-[280px] max-w-sm flex items-start gap-3 ${VARIANT_STYLES[toast.variant]}`}
				>
					<span className='material-symbols-outlined text-[18px]'>
						{toast.variant === 'success'
							? 'check_circle'
							: toast.variant === 'error'
								? 'error'
								: 'info'}
					</span>
					<p className='text-on-surface text-sm flex-1'>{toast.message}</p>
					<button
						onClick={() => removeToast(toast.id)}
						className='text-on-surface-variant hover:text-on-surface transition-colors'
					>
						<span className='material-symbols-outlined text-[16px]'>close</span>
					</button>
				</div>
			))}
		</div>
	);
}
