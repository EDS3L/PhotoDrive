import { useState } from 'react';
import { Link } from 'react-router';
import { useAssignedAlbums } from '@/hooks/use-albums';
import { useUiStore } from '@/lib/stores/ui-store';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import type { Album } from '@/types/album';
import { PhotoPlaceholder } from '@/components/ui/PhotoPlaceholder';

export function AlbumsListPage() {
	const { data: albums, isLoading } = useAssignedAlbums();
	const openModal = useUiStore((s) => s.openModal);
	const [search, setSearch] = useState('');

	const filtered = (albums ?? []).filter((a) =>
		a.name.toLowerCase().includes(search.toLowerCase()),
	);

	return (
		<div className='p-6 lg:p-10 max-w-7xl mx-auto'>
			<div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8'>
				<div>
					<p className='label text-[11px] text-primary mb-1'>ALBUMY</p>
					<h1 className='font-display text-4xl text-on-surface'>Moje albumy</h1>
				</div>
				<Button onClick={() => openModal('new-album')}>
					<span className='material-symbols-outlined text-[18px] mr-2'>
						add_photo_alternate
					</span>
					Nowy album
				</Button>
			</div>

			<Input
				placeholder='Szukaj albumu...'
				value={search}
				onChange={(e) => setSearch(e.target.value)}
				className='max-w-sm mb-8'
			/>

			{isLoading ? (
				<div className='flex items-center justify-center py-20'>
					<span className='material-symbols-outlined text-primary animate-spin text-3xl'>
						progress_activity
					</span>
				</div>
			) : filtered.length > 0 ? (
				<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4'>
					{filtered.map((album) => (
						<AlbumCard key={album.albumId} album={album} />
					))}
				</div>
			) : (
				<div className='text-center py-20'>
					<span className='material-symbols-outlined text-5xl text-on-surface-variant/30 mb-3 block'>
						photo_library
					</span>
					<p className='text-on-surface-variant text-sm'>Brak albumów</p>
				</div>
			)}
		</div>
	);
}

function AlbumCard({ album }: { album: Album }) {
	return (
		<Link
			to={`/fotograf/albumy/${album.albumId}`}
			className='group bg-surface-container-low hover:bg-surface-container transition-colors'
		>
			<PhotoPlaceholder label='Okładka albumu' className='aspect-[4/3]' />
			<div className='p-4'>
				<p className='text-on-surface text-sm font-medium group-hover:text-primary transition-colors'>
					{album.name}
				</p>
				<p className='text-on-surface-variant text-xs mt-0.5'>
					{album.photoCount} zdjęć
				</p>
				{album.ttd && (
					<p className='text-on-surface-variant/60 text-xs mt-1'>
						Wygasa: {new Date(album.ttd).toLocaleDateString('pl-PL')}
					</p>
				)}
			</div>
		</Link>
	);
}
