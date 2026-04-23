import { useParams } from 'react-router';
import { useAlbumPhotos, useDownloadFiles } from '@/hooks/use-albums';
import { useUiStore } from '@/lib/stores/ui-store';
import { Button } from '@/components/ui/Button';

export function ClientAlbumDetailPage() {
	const { id } = useParams<{ id: string }>();
	const albumId = id!;
	const { data: photoUrls, isLoading } = useAlbumPhotos(albumId, {
		showOnlyVisible: true,
	});
	const downloadFiles = useDownloadFiles();
	const {
		selectedFiles,
		toggleFile,
		clearSelection,
		selectionMode,
		toggleSelectionMode,
	} = useUiStore();

	const photos = photoUrls ?? [];

	const handleDownloadAll = () => {
		downloadFiles.mutate({ albumId, fileList: [] });
	};

	const handleDownloadSelected = () => {
		if (selectedFiles.length === 0) return;
		downloadFiles.mutate({ albumId, fileList: selectedFiles });
		clearSelection();
	};

	if (isLoading) {
		return (
			<div className='flex items-center justify-center py-20'>
				<span className='material-symbols-outlined text-primary animate-spin text-3xl'>
					progress_activity
				</span>
			</div>
		);
	}

	return (
		<div className='p-6 lg:p-10 max-w-7xl mx-auto'>
			<div className='flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 mb-6'>
				<div>
					<p className='label text-[11px] text-primary mb-1'>ALBUM</p>
					<h1 className='font-display text-4xl text-on-surface'>Galeria</h1>
					<p className='text-on-surface-variant text-xs mt-2'>
						{photos.length} zdjęć
					</p>
				</div>
				<div className='flex gap-2'>
					<Button variant='outline' size='sm' onClick={toggleSelectionMode}>
						<span className='material-symbols-outlined text-[16px] mr-1'>
							{selectionMode ? 'close' : 'select_all'}
						</span>
						{selectionMode ? 'Anuluj' : 'Zaznacz'}
					</Button>
					<Button size='sm' onClick={handleDownloadAll}>
						<span className='material-symbols-outlined text-[16px] mr-1'>
							download
						</span>
						Pobierz wszystkie
					</Button>
				</div>
			</div>

			{selectionMode && selectedFiles.length > 0 && (
				<div className='flex items-center gap-3 mb-6 p-3 bg-surface-container-low'>
					<span className='text-on-surface-variant text-sm'>
						Zaznaczono:{' '}
						<strong className='text-on-surface'>{selectedFiles.length}</strong>
					</span>
					<div className='flex-1' />
					<Button size='sm' onClick={handleDownloadSelected}>
						<span className='material-symbols-outlined text-[16px] mr-1'>
							download
						</span>
						Pobierz zaznaczone
					</Button>
				</div>
			)}

			{photos.length > 0 ? (
				<div className='grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-1'>
					{photos.map((url, idx) => (
						<div
							key={idx}
							className={`group relative aspect-square overflow-hidden cursor-pointer ${
								selectionMode && selectedFiles.includes(String(idx))
									? 'ring-2 ring-primary'
									: ''
							}`}
							onClick={() => {
								if (selectionMode) toggleFile(String(idx));
							}}
						>
							<img
								src={url}
								alt=''
								className='w-full h-full object-cover'
								loading='lazy'
							/>
							{selectionMode && (
								<div
									className={`absolute top-2 left-2 w-5 h-5 border-2 flex items-center justify-center ${
										selectedFiles.includes(String(idx))
											? 'bg-primary border-primary'
											: 'border-on-surface/60 bg-background/40'
									}`}
								>
									{selectedFiles.includes(String(idx)) && (
										<span className='material-symbols-outlined text-[14px] text-on-primary'>
											check
										</span>
									)}
								</div>
							)}
						</div>
					))}
				</div>
			) : (
				<div className='flex flex-col items-center justify-center py-24 text-center'>
					<span className='material-symbols-outlined text-5xl text-on-surface-variant/30 mb-3'>
						photo_library
					</span>
					<p className='text-on-surface-variant text-sm'>Album jest pusty</p>
				</div>
			)}
		</div>
	);
}
