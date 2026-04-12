import { X, Download, Loader2 } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/shared/components/ui/Button';
import { PhotoGrid, PhotoGridItem } from '@/shared/components/PhotoGrid';
import { getPhotoUrl, downloadAlbumZip } from '../api/clientZoneApi';
import type { AlbumDto } from '@/shared/types/api';

interface AlbumDetailModalProps {
	album: AlbumDto;
	onClose: () => void;
}

export function AlbumDetailModal({ album, onClose }: AlbumDetailModalProps) {
	const [downloading, setDownloading] = useState(false);

	const handleDownload = async () => {
		const visibleFiles = album.files.filter((f) => f.visible);
		if (visibleFiles.length === 0) return;
		setDownloading(true);
		try {
			await downloadAlbumZip(
				album.albumId,
				visibleFiles.map((f) => f.fileName),
			);
		} finally {
			setDownloading(false);
		}
	};

	return (
		<div className='fixed inset-0 z-[70] bg-background/95 overflow-y-auto'>
			{/* Header */}
			<div className='sticky top-0 bg-surface/95 backdrop-blur-md border-b border-border z-10'>
				<div className='max-w-7xl mx-auto px-6 py-4 flex items-center justify-between'>
					<div>
						<p className='text-xs uppercase tracking-[0.2em] text-accent'>
							Collection
						</p>
						<h2 className='font-serif text-2xl'>{album.name}</h2>
						<p className='text-xs text-muted'>
							{album.files.length} photographs
							{album.ttd && (
								<>
									{' '}
									&middot; Archived{' '}
									{new Date(album.ttd).toLocaleDateString('en-US', {
										month: 'long',
										year: 'numeric',
									})}
								</>
							)}
						</p>
					</div>
					<button
						onClick={onClose}
						className='text-foreground/60 hover:text-foreground transition-colors'
						aria-label='Zamknij'
					>
						<X className='w-6 h-6' />
					</button>
				</div>
			</div>

			{/* Photo grid */}
			<div className='max-w-7xl mx-auto px-6 py-8'>
				<PhotoGrid columns={4}>
					{album.files
						.filter((f) => f.visible)
						.map((file) => (
							<PhotoGridItem
								key={file.fileID}
								src={getPhotoUrl(album.albumId, file.fileName, 600)}
								alt={file.fileName}
							/>
						))}
				</PhotoGrid>

				{/* Download all */}
				<div className='flex justify-end mt-8'>
					<Button
						variant='outline'
						size='lg'
						onClick={handleDownload}
						disabled={downloading}
					>
						{downloading ? (
							<Loader2 className='w-4 h-4 mr-2 animate-spin' />
						) : (
							<Download className='w-4 h-4 mr-2' />
						)}
						Pobierz wszystkie
					</Button>
				</div>
			</div>
		</div>
	);
}
