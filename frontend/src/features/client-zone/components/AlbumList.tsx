import { HelpCircle, Loader2 } from 'lucide-react';
import { AlbumCard } from './AlbumCard';
import { useAlbums } from '../hooks/useAlbums';
import type { AlbumDto } from '@/shared/types/api';

interface AlbumListProps {
	onOpenAlbum: (album: AlbumDto) => void;
}

export function AlbumList({ onOpenAlbum }: AlbumListProps) {
	const { data: albums, isLoading, isError } = useAlbums();

	if (isLoading) {
		return (
			<div className='flex items-center justify-center py-24'>
				<Loader2 className='w-8 h-8 text-accent animate-spin' />
			</div>
		);
	}

	if (isError) {
		return (
			<div className='text-center py-24'>
				<p className='text-error'>
					Nie udało się załadować albumów. Spróbuj ponownie.
				</p>
			</div>
		);
	}

	if (!albums || albums.length === 0) {
		return (
			<div className='text-center py-24'>
				<p className='font-serif text-2xl mb-2'>Brak albumów</p>
				<p className='text-muted'>
					Twoje albumy pojawią się tutaj po udostępnieniu przez fotografa.
				</p>
			</div>
		);
	}

	return (
		<div>
			<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6'>
				{albums.map((album) => (
					<AlbumCard key={album.albumId} album={album} onOpen={onOpenAlbum} />
				))}
			</div>

			{/* Help card */}
			<div className='mt-12 border border-border p-6 flex items-start gap-4 max-w-lg'>
				<HelpCircle className='w-6 h-6 text-accent shrink-0 mt-0.5' />
				<div>
					<p className='font-serif text-lg mb-1'>Potrzebujesz pomocy?</p>
					<p className='text-sm text-muted'>
						Jeśli masz problem z dostępem do swoich zdjęć, skontaktuj się ze mną
						pod adresem{' '}
						<a
							href='mailto:kontakt@photodrive.dev'
							className='text-accent hover:underline'
						>
							kontakt@photodrive.dev
						</a>
					</p>
				</div>
			</div>
		</div>
	);
}
