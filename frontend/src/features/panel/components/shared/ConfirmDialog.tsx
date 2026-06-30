import { Loader2 } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { cn } from '@/lib/utils';
import { Modal } from './Modal';

interface ConfirmDialogProps {
	open: boolean;
	title: string;
	message: string;
	confirmLabel?: string;
	cancelLabel?: string;
	/** Akcent „niebezpieczny" (czerwony przycisk) — domyślnie true (akcje usuwania). */
	danger?: boolean;
	isPending?: boolean;
	onConfirm: () => void;
	onClose: () => void;
}

export function ConfirmDialog({
	open,
	title,
	message,
	confirmLabel = 'Potwierdź',
	cancelLabel = 'Anuluj',
	danger = true,
	isPending = false,
	onConfirm,
	onClose,
}: ConfirmDialogProps) {
	return (
		<Modal open={open} onClose={onClose} title={title} maxWidth='max-w-md'>
			<div className='p-8'>
				<p className='text-sm text-muted'>{message}</p>
			</div>
			<div className='px-8 py-6 border-t border-border flex justify-end gap-3'>
				<Button variant='ghost' onClick={onClose} disabled={isPending}>
					{cancelLabel}
				</Button>
				<Button
					onClick={onConfirm}
					disabled={isPending}
					className={cn(
						danger && 'bg-red-500 text-background hover:bg-red-600',
					)}
				>
					{isPending && <Loader2 className='w-4 h-4 mr-2 animate-spin' />}
					{confirmLabel}
				</Button>
			</div>
		</Modal>
	);
}
