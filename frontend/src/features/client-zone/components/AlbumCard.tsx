import { Lock } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { getPhotoUrl } from '../api/clientZoneApi';
import type { AlbumDto } from '@/shared/types/api';
import { placeholder } from '@/lib/placeholder';

interface AlbumCardProps {
	album: AlbumDto;
	onOpen: (album: AlbumDto) => void;
}

function isExpired(ttd: string | null): boolean {
	if (!ttd) return false;
	return new Date(ttd) < new Date();
}

export function AlbumCard({ album, onOpen }: AlbumCardProps) {
	const expired = isExpired(album.ttd);
	const visibleFiles = album.files.filter((file) => file.visible);
	const coverFile = visibleFiles[0];
	const coverSrc = coverFile
		? getPhotoUrl(album.albumId, coverFile.fileName, 400)
		: placeholder(400, 300, album.name);

	return (
		<div className='border border-border overflow-hidden group'>
			{/* Cover */}
			<div className='relative aspect-[4/3] overflow-hidden'>
				<img
					src={coverSrc}
					alt={album.name}
					className='w-full h-full object-cover transition-transform duration-500 group-hover:scale-[1.02]'
				/>
				{expired && (
					<div className='absolute inset-0 bg-black/60 flex flex-col items-center justify-center'>
						<Lock className='w-8 h-8 text-muted mb-2' />
						<span className='text-xs uppercase tracking-widest text-muted'>
							Link wygasł
						</span>
					</div>
				)}
			</div>

			{/* Info */}
			<div className='p-5'>
				<h3 className='font-serif text-lg mb-1'>{album.name}</h3>
				<p className='text-xs text-muted mb-4'>
					{visibleFiles.length} zdjęć
					{album.ttd && (
						<>
							{' '}
							&middot;{' '}
							{expired
								? 'Wygasł'
								: `Dostępny do ${new Date(album.ttd).toLocaleDateString('pl-PL')}`}
						</>
					)}
				</p>
				{!expired ? (
					<Button
						variant='outline'
						size='sm'
						className='w-full'
						onClick={() => onOpen(album)}
					>
						Otwórz album
					</Button>
				) : (
					<Button
						variant='ghost'
						size='sm'
						className='w-full opacity-50 cursor-not-allowed'
						disabled
					>
						Archiwalne
					</Button>
				)}
			</div>
		</div>
	);
}
