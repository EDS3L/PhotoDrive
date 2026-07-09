import { useState, useCallback, useMemo, useRef, type DragEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
	Upload,
	Download,
	Image,
	Eye,
	EyeOff,
	Trash2,
	Pencil,
	Droplets,
	ArrowRightLeft,
	MoreVertical,
	CheckSquare,
	X,
	Loader2,
	Clock,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { Input } from '@/shared/components/ui/Input';
import { Modal } from '../../components/shared/Modal';
import { ConfirmDialog } from '../../components/shared/ConfirmDialog';
import { SwapRenameDialog } from '../../components/shared/SwapRenameDialog';
import { LoadingSpinner } from '../../components/shared/LoadingSpinner';
import { StatusBadge } from '../../components/shared/StatusBadge';
import {
	useAdminAlbums,
	useDeleteAlbum,
	useUploadFiles,
	useRemoveFiles,
	useSetFilesVisible,
	useAddWatermark,
	useSwapFiles,
	useRenameFile,
	useSetAlbumPublic,
	useSetAlbumTtd,
	useDownloadAlbum,
} from '../../hooks/useAdminAlbums';
import { getPhotoUrl, getAlbumFileNames } from '../../api/adminApi';
import { useSwapWithRename } from '../../hooks/useSwapWithRename';
import { usePhotoSelection } from '../../hooks/usePhotoSelection';
import type { AlbumDto, FileDto } from '@/shared/types/api';

type VisibilityFilter = 'ALL' | 'VISIBLE' | 'HIDDEN';

function isAdminAlbum(album: AlbumDto) {
	return album.photographId === album.clientId;
}

export default function AdminAlbumDetail() {
	const { albumId } = useParams<{ albumId: string }>();
	const navigate = useNavigate();
	const { data: albums, isLoading } = useAdminAlbums();
	const album = albums?.find((a) => a.albumId === albumId);

	const uploadMutation = useUploadFiles();
	const removeMutation = useRemoveFiles();
	const visibilityMutation = useSetFilesVisible();
	const watermarkMutation = useAddWatermark();
	const swapMutation = useSwapFiles();
	const renameMutation = useRenameFile();
	const publicMutation = useSetAlbumPublic();
	const ttdMutation = useSetAlbumTtd();
	const deleteMutation = useDeleteAlbum();
	const downloadMutation = useDownloadAlbum();

	const fileInputRef = useRef<HTMLInputElement>(null);
	const {
		selected,
		clearSelection,
		selectOne,
		handleItemClick,
		toggleAll,
	} = usePhotoSelection();
	const [batchMode, setBatchMode] = useState(false);
	const [visFilter, setVisFilter] = useState<VisibilityFilter>('ALL');
	const [dragging, setDragging] = useState(false);
	const [contextMenu, setContextMenu] = useState<{
		file: FileDto;
		x: number;
		y: number;
	} | null>(null);

	// Modals
	const [renameModal, setRenameModal] = useState<FileDto | null>(null);
	const [renameValue, setRenameValue] = useState('');
	const [swapModal, setSwapModal] = useState(false);
	const [swapTarget, setSwapTarget] = useState<string | null>(null);
	const [ttdModal, setTtdModal] = useState(false);
	const [ttdValue, setTtdValue] = useState('');
	const [confirm, setConfirm] = useState<{
		title: string;
		message: string;
		confirmLabel: string;
		action: () => void;
	} | null>(null);

	const swapFlow = useSwapWithRename({
		getFileNames: getAlbumFileNames,
		rename: (v) => renameMutation.mutateAsync(v),
		swap: (v) => swapMutation.mutateAsync(v),
		onDone: clearSelection,
	});

	const filteredFiles = useMemo(() => {
		if (!album) return [];
		return album.files.filter((f) => {
			if (visFilter === 'VISIBLE') return f.visible;
			if (visFilter === 'HIDDEN') return !f.visible;
			return true;
		});
	}, [album, visFilter]);

	// Kolejność widocznych plików napędza „zaznacz wszystkie" i zakres z Shift
	// — zawsze względem aktualnego filtra widoczności.
	const orderedIds = useMemo(
		() => filteredFiles.map((f) => f.fileID),
		[filteredFiles],
	);
	const allSelected =
		orderedIds.length > 0 && orderedIds.every((id) => selected.has(id));

	// B.19: chowamy akcje wsadowe nieadekwatne do zaznaczenia (na bazie idempotencji A4).
	const selectedFiles =
		album?.files.filter((f) => selected.has(f.fileID)) ?? [];
	const canShowSelected = selectedFiles.some((f) => !f.visible);
	const canHideSelected = selectedFiles.some((f) => f.visible);
	const canWatermarkSelected = selectedFiles.some((f) => !f.hasWatermark);

	const otherAlbums = useMemo(
		() => albums?.filter((a) => a.albumId !== albumId) ?? [],
		[albums, albumId],
	);

	const handleUpload = useCallback(
		(files: FileList | File[]) => {
			if (!albumId) return;
			const arr = Array.from(files);
			uploadMutation.mutate({ albumId, files: arr });
		},
		[albumId, uploadMutation],
	);

	const handleDrop = (e: DragEvent) => {
		e.preventDefault();
		setDragging(false);
		if (e.dataTransfer.files.length) {
			handleUpload(e.dataTransfer.files);
		}
	};

	const handleBatchVisibility = (visible: boolean) => {
		if (!albumId || selected.size === 0) return;
		visibilityMutation.mutate(
			{ albumId, fileIds: [...selected], visible },
			{ onSuccess: clearSelection },
		);
	};

	const handleBatchWatermark = () => {
		if (!albumId || selected.size === 0) return;
		watermarkMutation.mutate(
			{ albumId, fileIds: [...selected], hasWatermark: true },
			{ onSuccess: clearSelection },
		);
	};

	const handleBatchDelete = () => {
		if (!albumId || selected.size === 0) return;
		const count = selected.size;
		const ids = [...selected];
		setConfirm({
			title: `Usunąć ${count} zdjęć?`,
			message: `Zaznaczone pliki (${count}) zostaną trwale usunięte. Tej operacji nie można cofnąć.`,
			confirmLabel: `Usuń (${count})`,
			action: () =>
				removeMutation.mutate(
					{ albumId, fileIds: ids },
					{
						onSuccess: () => {
							clearSelection();
							setBatchMode(false);
						},
					},
				),
		});
	};

	const handleSwap = () => {
		if (!albumId || !swapTarget || selected.size === 0 || !album) return;
		const files = album.files
			.filter((f) => selected.has(f.fileID))
			.map((f) => ({ fileID: f.fileID, fileName: f.fileName }));
		const targetId = swapTarget;
		setSwapModal(false);
		setSwapTarget(null);
		swapFlow.start({ sourceAlbumId: albumId, targetAlbumId: targetId, files });
	};

	const handleRename = () => {
		if (!albumId || !renameModal || !renameValue.trim()) return;
		renameMutation.mutate(
			{ albumId, fileId: renameModal.fileID, newName: renameValue },
			{ onSuccess: () => setRenameModal(null) },
		);
	};

	const handleTtdSave = () => {
		if (!albumId || !ttdValue) return;
		ttdMutation.mutate(
			{ albumId, ttd: ttdValue },
			{ onSuccess: () => setTtdModal(false) },
		);
	};

	const handleDeleteAlbum = () => {
		if (!albumId || !album) return;
		setConfirm({
			title: 'Usunąć album?',
			message: `Album „${album.name}" wraz z ${album.files.length} zdjęciami zostanie trwale usunięty. Tej operacji nie można cofnąć.`,
			confirmLabel: 'Usuń album',
			action: () =>
				deleteMutation.mutate(albumId, {
					onSuccess: () => navigate('/admin/albums'),
				}),
		});
	};

	if (isLoading) return <LoadingSpinner />;
	if (!album) {
		return (
			<div className='text-center py-16'>
				<p className='text-muted'>Album nie został znaleziony</p>
				<Button
					variant='ghost'
					className='mt-4'
					onClick={() => navigate('/admin/albums')}
				>
					← Powrót do albumów
				</Button>
			</div>
		);
	}

	const admin = isAdminAlbum(album);
	const expired = album.ttd ? new Date(album.ttd) < new Date() : false;
	const visibleCount = album.files.filter((f) => f.visible).length;
	const totalSize = album.files.reduce((s, f) => s + f.sizeBytes, 0);
	const sizeMB = (totalSize / 1024 / 1024).toFixed(1);

	return (
		<div
			onDragOver={(e) => {
				e.preventDefault();
				setDragging(true);
			}}
			onDragLeave={() => setDragging(false)}
			onDrop={handleDrop}
		>
			{/* Header */}
			<div className='flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4 mb-6'>
				<div>
					<h2 className='font-serif text-4xl font-light'>{album.name}</h2>
					<div className='flex flex-wrap items-center gap-3 mt-2 text-sm text-muted'>
						<span className='flex items-center gap-1'>
							<Image className='w-3.5 h-3.5' />
							{album.files.length} zdjęć
						</span>
						<span>·</span>
						<span className='flex items-center gap-1'>
							<Eye className='w-3.5 h-3.5' />
							{visibleCount} widoczne
						</span>
						{album.ttd && (
							<>
								<span>·</span>
								<span
									className={`flex items-center gap-1 ${expired ? 'text-red-400' : ''}`}
								>
									<Clock className='w-3.5 h-3.5' />
									{expired
										? 'Wygasł'
										: `Do: ${new Date(album.ttd).toLocaleDateString('pl-PL')}`}
								</span>
							</>
						)}
					</div>
				</div>
				<div className='flex items-center gap-3'>
					{!admin && (
						<Button variant='ghost' size='sm' onClick={() => setTtdModal(true)}>
							<Clock className='w-4 h-4 mr-1' />
							TTD
						</Button>
					)}
					<Button
						variant='outline'
						size='sm'
						disabled={downloadMutation.isPending || album.files.length === 0}
						onClick={() => {
							downloadMutation.mutate(
								{
									albumId: album.albumId,
									fileList: album.files.map((f) => f.fileName),
								},
								{
									onSuccess: (blob) => {
										const url = URL.createObjectURL(blob);
										const a = document.createElement('a');
										a.href = url;
										a.download = `${album.name}.zip`;
										document.body.appendChild(a);
										a.click();
										document.body.removeChild(a);
										setTimeout(() => URL.revokeObjectURL(url), 1000);
									},
								},
							);
						}}
					>
						{downloadMutation.isPending ? (
							<Loader2 className='w-4 h-4 mr-1 animate-spin' />
						) : (
							<Download className='w-4 h-4 mr-1' />
						)}
						Pobierz
					</Button>
					<Button size='sm' onClick={() => fileInputRef.current?.click()}>
						<Upload className='w-4 h-4 mr-1' />
						Dodaj zdjęcia
					</Button>
					<input
						ref={fileInputRef}
						type='file'
						multiple
						accept='image/jpeg,image/png'
						className='hidden'
						onChange={(e) => e.target.files && handleUpload(e.target.files)}
					/>
				</div>
			</div>

			{/* Album info card */}
			<div className='bg-surface border border-border p-5 mb-6'>
				<div className='flex flex-wrap gap-8'>
					<div>
						<p className='text-[10px] uppercase tracking-widest text-muted'>
							Typ
						</p>
						<StatusBadge variant={admin ? 'accent' : 'default'}>
							{admin ? 'Administracyjny' : 'Klienta'}
						</StatusBadge>
					</div>
					<div>
						<p className='text-[10px] uppercase tracking-widest text-muted'>
							Zdjęcia
						</p>
						<p className='text-sm'>
							{album.files.length} ({visibleCount} widoczne)
						</p>
					</div>
					<div>
						<p className='text-[10px] uppercase tracking-widest text-muted'>
							Rozmiar
						</p>
						<p className='text-sm'>{sizeMB} MB</p>
					</div>
					{admin && (
						<div>
							<p className='text-[10px] uppercase tracking-widest text-muted mb-1'>
								Publiczny
							</p>
							<button
								onClick={() =>
									publicMutation.mutate({
										albumId: album.albumId,
										isPublic: !album.isPublic,
									})
								}
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
					)}
				</div>
			</div>

			{/* Toolbar */}
			<div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4'>
				<div className='flex gap-1'>
					{(['ALL', 'VISIBLE', 'HIDDEN'] as VisibilityFilter[]).map((f) => (
						<button
							key={f}
							onClick={() => setVisFilter(f)}
							className={`px-3 py-1.5 text-xs uppercase tracking-wider transition-colors ${
								visFilter === f
									? 'bg-accent/10 text-accent'
									: 'text-muted hover:text-foreground'
							}`}
						>
							{f === 'ALL'
								? 'Wszystkie'
								: f === 'VISIBLE'
									? 'Widoczne'
									: 'Ukryte'}
						</button>
					))}
				</div>
				<div className='flex items-center gap-3'>
					{batchMode && filteredFiles.length > 0 && (
						<Button
							variant='ghost'
							size='sm'
							onClick={() => toggleAll(orderedIds)}
						>
							<CheckSquare className='w-3.5 h-3.5 mr-1' />
							{allSelected ? 'Odznacz wszystkie' : 'Zaznacz wszystkie'}
						</Button>
					)}
					{batchMode && selected.size > 0 && (
						<>
							<span className='text-xs text-muted'>
								Zaznaczono {selected.size} z {album.files.length}
							</span>
							{canShowSelected && (
								<Button
									variant='ghost'
									size='sm'
									onClick={() => handleBatchVisibility(true)}
								>
									<Eye className='w-3.5 h-3.5 mr-1' />
									Pokaż
								</Button>
							)}
							{canHideSelected && (
								<Button
									variant='ghost'
									size='sm'
									onClick={() => handleBatchVisibility(false)}
								>
									<EyeOff className='w-3.5 h-3.5 mr-1' />
									Ukryj
								</Button>
							)}
							{canWatermarkSelected && (
								<Button variant='ghost' size='sm' onClick={handleBatchWatermark}>
									<Droplets className='w-3.5 h-3.5 mr-1' />
									Watermark
								</Button>
							)}
							<Button
								variant='ghost'
								size='sm'
								onClick={() => setSwapModal(true)}
							>
								<ArrowRightLeft className='w-3.5 h-3.5 mr-1' />
								Przenieś
							</Button>
							<Button
								variant='ghost'
								size='sm'
								onClick={handleBatchDelete}
								className='text-red-400 hover:text-red-300'
							>
								<Trash2 className='w-3.5 h-3.5 mr-1' />
								Usuń ({selected.size})
							</Button>
						</>
					)}
					<Button
						variant={batchMode ? 'outline' : 'ghost'}
						size='sm'
						onClick={() => {
							setBatchMode(!batchMode);
							clearSelection();
						}}
					>
						{batchMode ? (
							<>
								<X className='w-3.5 h-3.5 mr-1' />
								Anuluj
							</>
						) : (
							<>
								<CheckSquare className='w-3.5 h-3.5 mr-1' />
								Zaznacz
							</>
						)}
					</Button>
				</div>
			</div>

			{/* Drag overlay */}
			{dragging && (
				<div className='fixed inset-0 z-40 bg-black/60 flex items-center justify-center pointer-events-none'>
					<div className='border-2 border-dashed border-accent p-16 text-center'>
						<Upload className='w-12 h-12 text-accent mx-auto mb-4' />
						<p className='text-lg font-serif'>Upuść pliki tutaj</p>
						<p className='text-xs text-muted mt-2'>
							JPG, PNG (maks. 100MB)
						</p>
					</div>
				</div>
			)}

			{/* Upload progress */}
			{uploadMutation.isPending && (
				<div className='mb-4 p-4 bg-surface border border-border flex items-center gap-3'>
					<Loader2 className='w-5 h-5 text-accent animate-spin' />
					<span className='text-sm'>Przesyłanie zdjęć...</span>
				</div>
			)}

			{/* Photo grid */}
			{filteredFiles.length === 0 ? (
				<div
					className='border-2 border-dashed border-border py-24 text-center cursor-pointer hover:border-accent/50 transition-colors'
					onClick={() => fileInputRef.current?.click()}
				>
					<Upload className='w-10 h-10 text-muted mx-auto mb-3' />
					<p className='text-muted'>Brak zdjęć — kliknij aby dodać</p>
				</div>
			) : (
				<div className='grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-1 select-none'>
					{filteredFiles.map((file) => (
						<div
							key={file.fileID}
							className={`relative aspect-square group cursor-pointer ${
								!file.visible ? 'opacity-50' : ''
							} ${selected.has(file.fileID) ? 'ring-2 ring-accent' : ''}`}
							onClick={(e) => batchMode && handleItemClick(file.fileID, orderedIds, e.shiftKey)}
						>
							<img
								src={getPhotoUrl(album.albumId, file.fileName, 300)}
								alt={file.fileName}
								className='w-full h-full object-cover'
							/>

							{/* Hover overlay */}
							<div className='absolute inset-0 bg-black/0 group-hover:bg-black/40 transition-colors flex items-end'>
								<div className='w-full p-2 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-between'>
									<span className='text-[11px] text-white truncate max-w-[70%]'>
										{file.fileName}
									</span>
									{!batchMode && (
										<button
											onClick={(e) => {
												e.stopPropagation();
												setContextMenu({
													file,
													x: e.clientX,
													y: e.clientY,
												});
											}}
											className='p-1 hover:bg-white/20'
										>
											<MoreVertical className='w-4 h-4 text-white' />
										</button>
									)}
								</div>
							</div>

							{/* Status icons */}
							{!file.visible && (
								<div className='absolute top-2 left-2'>
									<EyeOff className='w-4 h-4 text-white/80' />
								</div>
							)}
							{file.hasWatermark && (
								<div className='absolute top-2 right-2'>
									<Droplets className='w-4 h-4 text-white/80' />
								</div>
							)}

							{/* Batch checkbox */}
							{batchMode && (
								<div className='absolute top-2 left-2'>
									<div
										className={`w-5 h-5 border flex items-center justify-center ${
											selected.has(file.fileID)
												? 'bg-accent border-accent'
												: 'border-white/60 bg-black/30'
										}`}
									>
										{selected.has(file.fileID) && (
											<span className='text-background text-xs'>✓</span>
										)}
									</div>
								</div>
							)}
						</div>
					))}
				</div>
			)}

			{/* Context menu */}
			{contextMenu && (
				<>
					<div
						className='fixed inset-0 z-50'
						onClick={() => setContextMenu(null)}
					/>
					<div
						className='fixed z-50 bg-surface border border-border py-1 min-w-[200px] shadow-lg'
						style={{ left: contextMenu.x, top: contextMenu.y }}
					>
						<button
							className='w-full px-4 py-2 text-sm text-left hover:bg-surface-light flex items-center gap-2'
							onClick={() => {
								setRenameValue(contextMenu.file.fileName);
								setRenameModal(contextMenu.file);
								setContextMenu(null);
							}}
						>
							<Pencil className='w-3.5 h-3.5' />
							Zmień nazwę
						</button>
						<button
							className='w-full px-4 py-2 text-sm text-left hover:bg-surface-light flex items-center gap-2'
							onClick={() => {
								if (!albumId) return;
								visibilityMutation.mutate({
									albumId,
									fileIds: [contextMenu.file.fileID],
									visible: !contextMenu.file.visible,
								});
								setContextMenu(null);
							}}
						>
							{contextMenu.file.visible ? (
								<>
									<EyeOff className='w-3.5 h-3.5' />
									Ukryj
								</>
							) : (
								<>
									<Eye className='w-3.5 h-3.5' />
									Pokaż klientowi
								</>
							)}
						</button>
						<button
							className='w-full px-4 py-2 text-sm text-left hover:bg-surface-light flex items-center gap-2'
							onClick={() => {
								if (!albumId) return;
								watermarkMutation.mutate({
									albumId,
									fileIds: [contextMenu.file.fileID],
									hasWatermark: !contextMenu.file.hasWatermark,
								});
								setContextMenu(null);
							}}
						>
							<Droplets className='w-3.5 h-3.5' />
							{contextMenu.file.hasWatermark
								? 'Usuń watermark'
								: 'Dodaj watermark'}
						</button>
						<button
							className='w-full px-4 py-2 text-sm text-left hover:bg-surface-light flex items-center gap-2'
							onClick={() => {
								selectOne(contextMenu.file.fileID);
								setSwapModal(true);
								setContextMenu(null);
							}}
						>
							<ArrowRightLeft className='w-3.5 h-3.5' />
							Przenieś do albumu...
						</button>
						<div className='border-t border-border my-1' />
						<button
							className='w-full px-4 py-2 text-sm text-left hover:bg-surface-light flex items-center gap-2 text-red-400'
							onClick={() => {
								if (!albumId) return;
								const file = contextMenu.file;
								setContextMenu(null);
								setConfirm({
									title: 'Usunąć zdjęcie?',
									message: `Plik „${file.fileName}" zostanie trwale usunięty. Tej operacji nie można cofnąć.`,
									confirmLabel: 'Usuń',
									action: () =>
										removeMutation.mutate({
											albumId,
											fileIds: [file.fileID],
										}),
								});
							}}
						>
							<Trash2 className='w-3.5 h-3.5' />
							Usuń
						</button>
					</div>
				</>
			)}

			{/* Rename modal */}
			<Modal
				open={!!renameModal}
				onClose={() => setRenameModal(null)}
				title='Zmień nazwę pliku'
				maxWidth='max-w-md'
			>
				<div className='p-8'>
					<Input
						id='rename-file'
						label='Nowa nazwa'
						value={renameValue}
						onChange={(e) => setRenameValue(e.target.value)}
					/>
				</div>
				<div className='px-8 py-6 border-t border-border flex justify-end gap-3'>
					<Button variant='ghost' onClick={() => setRenameModal(null)}>
						Anuluj
					</Button>
					<Button onClick={handleRename} disabled={renameMutation.isPending}>
						Zapisz
					</Button>
				</div>
			</Modal>

			{/* Swap modal */}
			<Modal
				open={swapModal}
				onClose={() => {
					setSwapModal(false);
					setSwapTarget(null);
				}}
				title={`Przenieś ${selected.size} zdjęć`}
				maxWidth='max-w-lg'
			>
				<div className='p-8'>
					<p className='text-xs uppercase tracking-widest text-muted mb-3'>
						Wybierz album docelowy
					</p>
					<div className='space-y-2 max-h-64 overflow-y-auto'>
						{otherAlbums.map((a) => (
							<button
								key={a.albumId}
								onClick={() => setSwapTarget(a.albumId)}
								className={`w-full p-4 border text-left transition-colors flex items-center justify-between ${
									swapTarget === a.albumId
										? 'border-accent bg-accent/5'
										: 'border-border hover:border-accent/30'
								}`}
							>
								<div>
									<p className='text-sm font-medium'>{a.name}</p>
									<p className='text-xs text-muted'>{a.files.length} zdjęć</p>
								</div>
								{swapTarget === a.albumId && (
									<span className='text-accent'>✓</span>
								)}
							</button>
						))}
					</div>
				</div>
				<div className='px-8 py-6 border-t border-border flex justify-end gap-3'>
					<Button
						variant='ghost'
						onClick={() => {
							setSwapModal(false);
							setSwapTarget(null);
						}}
					>
						Anuluj
					</Button>
					<Button
						onClick={handleSwap}
						disabled={!swapTarget || swapFlow.isChecking}
					>
						Przenieś ({selected.size})
					</Button>
				</div>
			</Modal>

			{/* TTD modal */}
			<Modal
				open={ttdModal}
				onClose={() => setTtdModal(false)}
				title='Zmień czas wygaśnięcia'
				maxWidth='max-w-md'
			>
				<div className='p-8 space-y-4'>
					{album.ttd && (
						<p className='text-sm'>
							Obecne: {new Date(album.ttd).toLocaleDateString('pl-PL')}
						</p>
					)}
					<Input
						id='ttd-date'
						label='Nowa data wygaśnięcia'
						type='date'
						value={ttdValue}
						onChange={(e) => setTtdValue(e.target.value)}
					/>
					<p className='text-xs text-muted'>Data musi być w przyszłości</p>
				</div>
				<div className='px-8 py-6 border-t border-border flex justify-end gap-3'>
					<Button variant='ghost' onClick={() => setTtdModal(false)}>
						Anuluj
					</Button>
					<Button
						onClick={handleTtdSave}
						disabled={!ttdValue || ttdMutation.isPending}
					>
						Zapisz
					</Button>
				</div>
			</Modal>

			{/* Delete album */}
			<div className='mt-12 pt-8 border-t border-border'>
				<Button
					variant='ghost'
					size='sm'
					className='text-red-400 hover:text-red-300'
					onClick={handleDeleteAlbum}
					disabled={deleteMutation.isPending}
				>
					<Trash2 className='w-4 h-4 mr-2' />
					Usuń album
				</Button>
			</div>

			{/* Potwierdzenie akcji destrukcyjnych */}
			<ConfirmDialog
				open={!!confirm}
				title={confirm?.title ?? ''}
				message={confirm?.message ?? ''}
				confirmLabel={confirm?.confirmLabel}
				onClose={() => setConfirm(null)}
				onConfirm={() => {
					confirm?.action();
					setConfirm(null);
				}}
			/>

			{/* Kolizja nazw przy przenoszeniu — zmiana nazw przed swapem */}
			<SwapRenameDialog
				open={!!swapFlow.renames}
				renames={swapFlow.renames ?? []}
				onChange={swapFlow.setNewName}
				onConfirm={swapFlow.confirm}
				onCancel={swapFlow.cancel}
				isPending={swapFlow.isSubmitting}
			/>
		</div>
	);
}
