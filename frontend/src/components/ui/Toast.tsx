import { useToastStore } from './ToastStore';

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
