import { Upload } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { Input } from '@/shared/components/ui/Input';
import { Modal } from './Modal';
import type {
	CollisionEntry,
	CollisionAction,
} from '../../hooks/useUploadWithCollisionCheck';

interface UploadCollisionDialogProps {
	open: boolean;
	collisions: CollisionEntry[];
	onSetAction: (id: string, action: CollisionAction) => void;
	onSetName: (id: string, value: string) => void;
	onConfirm: () => void;
	onCancel: () => void;
}

/**
 * Modal kolizji nazw przy uploadzie: dla każdego pliku o nazwie już obecnej
 * w albumie fotograf wybiera „Zmień nazwę" (nowa nazwa) albo „Pomiń".
 * Analogiczny do `SwapRenameDialog`, ale z opcją pominięcia pliku.
 */
export function UploadCollisionDialog({
	open,
	collisions,
	onSetAction,
	onSetName,
	onConfirm,
	onCancel,
}: UploadCollisionDialogProps) {
	const renameEntries = collisions.filter((e) => e.action === 'rename');
	const renameNames = renameEntries.map((e) => e.newName.trim());
	const hasEmpty = renameNames.some((n) => n.length === 0);
	const hasDuplicate = new Set(renameNames).size !== renameNames.length;
	const invalid = hasEmpty || hasDuplicate;
	const skipCount = collisions.length - renameEntries.length;

	const toggleClass = (active: boolean) =>
		`px-3 py-1.5 text-xs uppercase tracking-wider transition-colors ${
			active
				? 'bg-accent/10 text-accent'
				: 'text-muted hover:text-foreground'
		}`;

	return (
		<Modal
			open={open}
			onClose={onCancel}
			title='Kolizja nazw plików'
			maxWidth='max-w-xl'
		>
			<div className='p-8 space-y-5'>
				<p className='text-sm text-muted'>
					Album zawiera już {collisions.length}{' '}
					{collisions.length === 1
						? 'plik o tej nazwie'
						: 'plików o tych nazwach'}
					. Dla każdego wybierz: zmień nazwę albo pomiń plik.
				</p>

				<div className='space-y-3 max-h-80 overflow-y-auto'>
					{collisions.map((e) => (
						<div key={e.id} className='border border-border p-3'>
							<p className='text-xs text-muted mb-2 truncate'>
								Plik:{' '}
								<span className='text-foreground'>{e.originalName}</span>
							</p>
							<div className='flex gap-1 mb-2'>
								<button
									type='button'
									onClick={() => onSetAction(e.id, 'rename')}
									className={toggleClass(e.action === 'rename')}
								>
									Zmień nazwę
								</button>
								<button
									type='button'
									onClick={() => onSetAction(e.id, 'skip')}
									className={toggleClass(e.action === 'skip')}
								>
									Pomiń
								</button>
							</div>
							{e.action === 'rename' ? (
								<Input
									id={`collision-${e.id}`}
									value={e.newName}
									onChange={(ev) => onSetName(e.id, ev.target.value)}
								/>
							) : (
								<p className='text-xs text-muted italic'>
									Plik zostanie pominięty — nie doda się do albumu.
								</p>
							)}
						</div>
					))}
				</div>

				{hasDuplicate && (
					<p className='text-xs text-error'>
						Nowe nazwy muszą się różnić między sobą.
					</p>
				)}
			</div>

			<div className='px-8 py-6 border-t border-border flex items-center justify-between gap-3'>
				<span className='text-xs text-muted tabular-nums'>
					{renameEntries.length} do zmiany · {skipCount} pominięte
				</span>
				<div className='flex gap-3'>
					<Button variant='ghost' onClick={onCancel}>
						Anuluj
					</Button>
					<Button onClick={onConfirm} disabled={invalid}>
						<Upload className='w-4 h-4 mr-2' />
						Wgraj
					</Button>
				</div>
			</div>
		</Modal>
	);
}
