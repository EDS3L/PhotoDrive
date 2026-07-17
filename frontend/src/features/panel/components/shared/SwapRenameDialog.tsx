import { Loader2, ArrowRightLeft } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { Input } from '@/shared/components/ui/Input';
import { Modal } from './Modal';
import type { RenameEntry } from '../../hooks/useSwapWithRename';

interface SwapRenameDialogProps {
	open: boolean;
	renames: RenameEntry[];
	onChange: (fileId: string, value: string) => void;
	onConfirm: () => void;
	onCancel: () => void;
	isPending: boolean;
}

export function SwapRenameDialog({
	open,
	renames,
	onChange,
	onConfirm,
	onCancel,
	isPending,
}: SwapRenameDialogProps) {
	const trimmed = renames.map((r) => r.newName.trim());
	const hasEmpty = trimmed.some((n) => n.length === 0);
	const hasDuplicate = new Set(trimmed).size !== trimmed.length;
	const invalid = hasEmpty || hasDuplicate;

	return (
		<Modal
			open={open}
			onClose={onCancel}
			title='Kolizja nazw plików'
			maxWidth='max-w-lg'
		>
			<div className='p-8 space-y-5'>
				<p className='text-sm text-muted'>
					Album docelowy zawiera już {renames.length}{' '}
					{renames.length === 1 ? 'plik o tej nazwie' : 'pliki o tych nazwach'}.
					Nadaj nowe nazwy — zostaną zmienione przy przenoszeniu.
				</p>

				<div className='space-y-4 max-h-72 overflow-y-auto'>
					{renames.map((r) => (
						<div key={r.fileId}>
							<p className='text-xs text-muted mb-1 truncate'>
								Oryginał: <span className='text-foreground'>{r.originalName}</span>
							</p>
							<Input
								id={`rename-${r.fileId}`}
								value={r.newName}
								onChange={(e) => onChange(r.fileId, e.target.value)}
							/>
						</div>
					))}
				</div>

				{hasDuplicate && (
					<p className='text-xs text-error'>
						Nazwy muszą się różnić między sobą.
					</p>
				)}
			</div>

			<div className='px-8 py-6 border-t border-border flex justify-end gap-3'>
				<Button variant='ghost' onClick={onCancel} disabled={isPending}>
					Anuluj
				</Button>
				<Button onClick={onConfirm} disabled={isPending || invalid}>
					{isPending ? (
						<Loader2 className='w-4 h-4 mr-2 animate-spin' />
					) : (
						<ArrowRightLeft className='w-4 h-4 mr-2' />
					)}
					Zmień nazwy i przenieś
				</Button>
			</div>
		</Modal>
	);
}
