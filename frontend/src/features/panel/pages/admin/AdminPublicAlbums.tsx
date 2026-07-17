import { useMemo, useState } from 'react';
import { Globe, FolderPlus, Info, Loader2, Pencil } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { Input } from '@/shared/components/ui/Input';
import { Modal } from '../../components/shared/Modal';
import { LoadingSpinner } from '../../components/shared/LoadingSpinner';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { EmptyState } from '../../components/shared/EmptyState';
import {
	useAdminAlbums,
	useCreateAdminAlbum,
	useSetAlbumPublic,
	useSetAlbumDisplay,
} from '../../hooks/useAdminAlbums';
import { toast } from '@/shared/store/toastStore';
import { albumNameError } from '../../lib/albumName';
import type { AlbumDto } from '@/shared/types/api';

/**
 * Publiczny album admina = zakładka w portfolio na stronie. Koniec magicznych nazw
 * (`portfolio-sluby`): o tym, co i w jakiej kolejności widzi gość, decydują etykieta
 * i kolejność ustawiane tutaj, nie konwencja nazewnicza.
 */
export default function AdminPublicAlbums() {
	const { data: albums, isLoading } = useAdminAlbums();
	const createMutation = useCreateAdminAlbum();
	const publicMutation = useSetAlbumPublic();
	const displayMutation = useSetAlbumDisplay();

	const [createOpen, setCreateOpen] = useState(false);
	const [newName, setNewName] = useState('');
	const [editing, setEditing] = useState<AlbumDto | null>(null);
	const [editLabel, setEditLabel] = useState('');
	const [editOrder, setEditOrder] = useState('0');

	// Kandydaci na zakładki: wyłącznie albumy admina (fotograf==klient); porządek jak na stronie.
	const adminAlbums = useMemo(
		() =>
			(albums ?? [])
				.filter((a) => a.photographId !== null && a.photographId === a.clientId)
				.sort(
					(a, b) =>
						a.displayOrder - b.displayOrder || a.name.localeCompare(b.name),
				),
		[albums],
	);

	const publicCount = useMemo(
		() => adminAlbums.filter((a) => a.isPublic).length,
		[adminAlbums],
	);

	const handleCreate = () => {
		createMutation.mutate(newName, {
			onSuccess: (album) => {
				publicMutation.mutate(
					{ albumId: album.albumId, isPublic: true },
					{
						onSettled: () => {
							setCreateOpen(false);
							setNewName('');
						},
					},
				);
			},
		});
	};

	const togglePublic = (album: AlbumDto) => {
		publicMutation.mutate({
			albumId: album.albumId,
			isPublic: !album.isPublic,
		});
	};

	const openEdit = (album: AlbumDto) => {
		setEditing(album);
		setEditLabel(album.displayName ?? '');
		setEditOrder(String(album.displayOrder));
	};

	const handleSaveDisplay = () => {
		if (!editing) return;
		displayMutation.mutate(
			{
				albumId: editing.albumId,
				// Pusta etykieta = wróć do nazwy technicznej (backend zapisuje null).
				displayName: editLabel.trim() === '' ? null : editLabel.trim(),
				displayOrder: Number(editOrder) || 0,
			},
			{
				onSuccess: () => {
					setEditing(null);
					toast.success('Ustawienia zakładki zapisane');
				},
			},
		);
	};

	if (isLoading) return <LoadingSpinner />;

	return (
		<div>
			{/* Header */}
			<div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6'>
				<div>
					<h2 className='font-serif text-4xl font-light'>Albumy publiczne</h2>
					<p className='text-sm text-muted mt-1'>
						Każdy publiczny album to zakładka w portfolio na stronie
					</p>
				</div>
				<Button onClick={() => setCreateOpen(true)}>
					<FolderPlus className='w-4 h-4 mr-2' />
					Nowy album
				</Button>
			</div>

			{/* Info banner */}
			<div className='bg-accent/5 border border-border p-4 mb-8 flex items-start gap-3'>
				<Info className='w-5 h-5 text-accent shrink-0 mt-0.5' />
				<div>
					<p className='text-sm'>
						Gość widzi zakładki w ustawionej tu kolejności, podpisane etykietą
						(może zawierać polskie znaki). Album bez etykiety pokazuje nazwę
						techniczną; album bez zdjęć nie dostaje zakładki.
					</p>
					<p className='text-xs text-muted mt-1'>
						Pojedyncze zdjęcia sekcji strony (hero, „o mnie") żyją osobno w{' '}
						<span className='text-accent'>Stronie wizytówce</span>.
					</p>
				</div>
			</div>

			{/* Stats */}
			<div className='mb-8'>
				<span className='text-xs text-muted'>
					{publicCount} albumów publicznych
				</span>
			</div>

			{/* Albums */}
			{adminAlbums.length === 0 ? (
				<EmptyState
					icon={<Globe className='w-12 h-12' />}
					title='Brak albumów portfolio'
					description='Utwórz album i włącz widoczność publiczną'
				/>
			) : (
				<div className='space-y-2'>
					{adminAlbums.map((album) => (
						<div
							key={album.albumId}
							className='bg-surface border border-border p-4 flex items-center justify-between'
						>
							<div className='flex items-center gap-4 min-w-0'>
								<div className='w-12 h-12 bg-surface-light flex items-center justify-center shrink-0'>
									<Globe
										className={`w-5 h-5 ${
											album.isPublic ? 'text-accent' : 'text-muted/30'
										}`}
									/>
								</div>
								<div className='min-w-0'>
									<p className='text-sm font-medium truncate'>
										{album.displayName ?? album.name}
									</p>
									<p className='text-xs text-muted truncate'>
										{album.name} · {album.files.length} zdjęć · kolejność{' '}
										{album.displayOrder}
									</p>
								</div>
							</div>
							<div className='flex items-center gap-4 shrink-0'>
								<Button
									size='sm'
									variant='ghost'
									aria-label={`Ustawienia zakładki — ${album.displayName ?? album.name}`}
									onClick={() => openEdit(album)}
								>
									<Pencil className='w-4 h-4' />
								</Button>
								<StatusBadge variant={album.isPublic ? 'success' : 'default'}>
									{album.isPublic ? 'Publiczny' : 'Ukryty'}
								</StatusBadge>
								<button
									onClick={() => togglePublic(album)}
									aria-label={`Przełącz widoczność — ${album.displayName ?? album.name}`}
									className={`relative inline-flex h-6 w-11 items-center transition-colors ${
										album.isPublic ? 'bg-accent' : 'bg-border'
									}`}
								>
									<span
										className={`inline-block h-4 w-4 bg-foreground transition-transform ${
											album.isPublic ? 'translate-x-6' : 'translate-x-1'
										}`}
									/>
								</button>
							</div>
						</div>
					))}
				</div>
			)}

			{/* Create modal */}
			<Modal
				open={createOpen}
				onClose={() => setCreateOpen(false)}
				title='Nowy album publiczny'
			>
				<div className='p-8 space-y-6'>
					<Input
						id='public-album-name'
						label='Nazwa techniczna (bez polskich znaków)'
						placeholder='np. sluby-2026'
						value={newName}
						onChange={(e) => setNewName(e.target.value)}
						error={albumNameError(newName)}
					/>
					<p className='text-xs text-muted'>
						Album zostanie utworzony jako administracyjny i od razu ustawiony
						jako publiczny. Etykietę zakładki (np. „Śluby") i kolejność ustawisz
						po utworzeniu ołówkiem przy albumie.
					</p>
				</div>
				<div className='px-8 py-6 border-t border-border flex justify-end gap-3'>
					<Button variant='ghost' onClick={() => setCreateOpen(false)}>
						Anuluj
					</Button>
					<Button
						onClick={handleCreate}
						disabled={
							createMutation.isPending ||
							!newName.trim() ||
							albumNameError(newName) !== undefined
						}
					>
						{createMutation.isPending ? (
							<Loader2 className='w-4 h-4 mr-2 animate-spin' />
						) : (
							<FolderPlus className='w-4 h-4 mr-2' />
						)}
						Utwórz album
					</Button>
				</div>
			</Modal>

			{/* Display settings modal */}
			<Modal
				open={editing !== null}
				onClose={() => setEditing(null)}
				title='Ustawienia zakładki'
			>
				<div className='p-8 space-y-6'>
					<Input
						id='album-display-name'
						label='Etykieta zakładki'
						placeholder={editing?.name ?? ''}
						value={editLabel}
						onChange={(e) => setEditLabel(e.target.value)}
					/>
					<Input
						id='album-display-order'
						label='Kolejność (mniejsza = wcześniej)'
						type='number'
						value={editOrder}
						onChange={(e) => setEditOrder(e.target.value)}
					/>
					<p className='text-xs text-muted'>
						Etykieta może zawierać polskie znaki — to ją widzi gość na stronie.
						Pozostaw pustą, aby wrócić do nazwy technicznej albumu.
					</p>
				</div>
				<div className='px-8 py-6 border-t border-border flex justify-end gap-3'>
					<Button variant='ghost' onClick={() => setEditing(null)}>
						Anuluj
					</Button>
					<Button
						onClick={handleSaveDisplay}
						disabled={displayMutation.isPending}
					>
						{displayMutation.isPending ? (
							<Loader2 className='w-4 h-4 mr-2 animate-spin' />
						) : null}
						Zapisz
					</Button>
				</div>
			</Modal>
		</div>
	);
}
