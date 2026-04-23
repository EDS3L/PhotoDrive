import { useState, useCallback } from 'react';
import { useParams } from 'react-router';
import {
	useAlbumPhotos,
	useUploadFiles,
	useRemoveFiles,
	useChangeVisibility,
	useAddWatermark,
	useDownloadFiles,
} from '@/hooks/use-albums';
import { Button } from '@/components/ui/Button';
import { FileDropzone } from '@/components/ui/FileDropzone';
import { ProgressBar } from '@/components/ui/ProgressBar';
import { useUiStore } from '@/lib/stores/ui-store';

export function AdminAlbumDetailPage() {
	const { id } = useParams<{ id: string }>();
	const albumId = id!;
	const { data: photoUrls, isLoading } = useAlbumPhotos(albumId);
	const uploadFiles = useUploadFiles();
	const removeFiles = useRemoveFiles();
	const changeVisibility = useChangeVisibility();
	const addWatermark = useAddWatermark();
	const downloadFiles = useDownloadFiles();

	const {
		selectedFiles,
		toggleFile,
		clearSelection,
		selectionMode,
		toggleSelectionMode,
	} = useUiStore();
	const openModal = useUiStore((s) => s.openModal);

	const [uploading, setUploading] = useState(false);
	const [uploadProgress, setUploadProgress] = useState(0);
	const [showUploadZone, setShowUploadZone] = useState(false);

	const photos = photoUrls ?? [];

	const handleUpload = useCallback(
		async (files: File[]) => {
			setUploading(true);
			setUploadProgress(0);
			try {
				await uploadFiles.mutateAsync({ albumId, files });
				setShowUploadZone(false);
			} finally {
				setUploading(false);
				setUploadProgress(100);
			}
		},
		[albumId, uploadFiles],
	);

	const handleDeleteSelected = () => {
		if (selectedFiles.length === 0) return;
		removeFiles.mutate({ albumId, fileIdList: selectedFiles });
		clearSelection();
	};

	const handleDownloadSelected = () => {
		if (selectedFiles.length === 0) return;
		downloadFiles.mutate({ albumId, fileList: selectedFiles });
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
					<h1 className='font-display text-4xl text-on-surface'>
						Album #{albumId.slice(0, 8)}
					</h1>
					<p className='text-on-surface-variant text-xs mt-2'>
						{photos.length} zdjęć
					</p>
				</div>
				<div className='flex gap-2 flex-wrap'>
					<Button
						variant='outline'
						size='sm'
						onClick={() => setShowUploadZone(!showUploadZone)}
					>
						<span className='material-symbols-outlined text-[16px] mr-1'>
							cloud_upload
						</span>
						Upload
					</Button>
					<Button variant='outline' size='sm' onClick={toggleSelectionMode}>
						<span className='material-symbols-outlined text-[16px] mr-1'>
							{selectionMode ? 'close' : 'select_all'}
						</span>
						{selectionMode ? 'Anuluj' : 'Zaznacz'}
					</Button>
					<Button
						variant='outline'
						size='sm'
						onClick={() =>
							changeVisibility.mutate({
								albumId,
								idList: photos,
								visible: true,
							})
						}
					>
						<span className='material-symbols-outlined text-[16px] mr-1'>
							visibility
						</span>
						Pokaż
					</Button>
					<Button
						variant='outline'
						size='sm'
						onClick={() => openModal('set-ttd', { albumId })}
					>
						<span className='material-symbols-outlined text-[16px] mr-1'>
							schedule
						</span>
						TTD
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
					<Button size='sm' variant='outline' onClick={handleDownloadSelected}>
						<span className='material-symbols-outlined text-[16px] mr-1'>
							download
						</span>
						Pobierz
					</Button>
					<Button
						size='sm'
						variant='outline'
						onClick={() =>
							addWatermark.mutate({
								albumId,
								filesUUIDList: selectedFiles,
								hasWatermark: true,
							})
						}
					>
						<span className='material-symbols-outlined text-[16px] mr-1'>
							branding_watermark
						</span>
						Watermark
					</Button>
					<Button size='sm' variant='danger' onClick={handleDeleteSelected}>
						<span className='material-symbols-outlined text-[16px] mr-1'>
							delete
						</span>
						Usuń
					</Button>
				</div>
			)}

			{showUploadZone && (
				<div className='mb-8'>
					<FileDropzone onFiles={handleUpload} />
					{uploading && <ProgressBar value={uploadProgress} className='mt-4' />}
				</div>
			)}

			{photos.length > 0 ? (
				<div className='grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-1'>
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
							{!selectionMode && (
								<div className='absolute inset-0 bg-background/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center'>
									<button className='text-on-surface hover:text-primary transition-colors'>
										<span className='material-symbols-outlined text-3xl'>
											open_in_full
										</span>
									</button>
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
					<Button
						variant='outline'
						size='sm'
						className='mt-4'
						onClick={() => setShowUploadZone(true)}
					>
						Dodaj zdjęcia
					</Button>
				</div>
			)}
		</div>
	);
}
