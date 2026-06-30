import { useEffect } from 'react';
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import {
	useToastStore,
	type Toast as ToastData,
	type ToastVariant,
} from '@/shared/store/toastStore';

const TOAST_TTL_MS = 5000;

const variantStyles: Record<ToastVariant, string> = {
	error: 'border-error/40',
	success: 'border-accent/40',
	info: 'border-border',
};

const variantIcon: Record<ToastVariant, typeof Info> = {
	error: AlertCircle,
	success: CheckCircle2,
	info: Info,
};

const variantIconColor: Record<ToastVariant, string> = {
	error: 'text-error',
	success: 'text-accent',
	info: 'text-muted',
};

function ToastItem({ toast }: { toast: ToastData }) {
	const removeToast = useToastStore((s) => s.removeToast);

	useEffect(() => {
		const timer = setTimeout(() => removeToast(toast.id), TOAST_TTL_MS);
		return () => clearTimeout(timer);
	}, [toast.id, removeToast]);

	const Icon = variantIcon[toast.variant];

	return (
		<div
			role='status'
			aria-live='polite'
			className={cn(
				'pointer-events-auto flex items-start gap-3 bg-surface border px-4 py-3 shadow-lg max-w-sm',
				variantStyles[toast.variant],
			)}
		>
			<Icon
				className={cn('w-5 h-5 shrink-0 mt-0.5', variantIconColor[toast.variant])}
			/>
			<p className='text-sm text-foreground flex-1 break-words'>
				{toast.message}
			</p>
			<button
				type='button'
				onClick={() => removeToast(toast.id)}
				aria-label='Zamknij powiadomienie'
				className='text-muted hover:text-foreground transition-colors shrink-0'
			>
				<X className='w-4 h-4' />
			</button>
		</div>
	);
}

export function ToastViewport() {
	const toasts = useToastStore((s) => s.toasts);

	if (toasts.length === 0) return null;

	return (
		<div className='fixed bottom-6 right-6 z-[100] flex flex-col gap-3 pointer-events-none'>
			{toasts.map((toast) => (
				<ToastItem key={toast.id} toast={toast} />
			))}
		</div>
	);
}
