import { useState } from 'react';
import { useAlbums, useDeleteAlbum } from '@/hooks/use-albums';
import { Button } from '@/components/ui/Button';

import { useUiStore } from '@/lib/stores/ui-store';
import type { Album } from '@/types/album';
import { PhotoPlaceholder } from '@/components/ui/PhotoPlaceholder';

export function PublicAlbumsPage() {
	const { data: albums, isLoading } = useAlbums();
	const deleteAlbum = useDeleteAlbum();
	const openModal = useUiStore((s) => s.openModal);
	const [activeCategory, setActiveCategory] = useState('all');

	const publicAlbums = (albums ?? []).filter((a) => a.isPublic);

	const filtered =
		activeCategory === 'all'
			? publicAlbums
			: publicAlbums.filter((a) =>
					a.name.toLowerCase().includes(activeCategory.toLowerCase()),
				);

	const categories = [
		{ key: 'all', label: 'Wszystkie' },
		{ key: 'sluby', label: 'Śluby' },
		{ key: 'plenerowe', label: 'Plenerowe' },
		{ key: 'reportaz', label: 'Reportaż' },
		{ key: 'portret', label: 'Portret' },
	];

	return (
		<div className='p-6 lg:p-10 max-w-7xl mx-auto'>
			<div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-10'>
				<div>
					<p className='label text-[11px] text-primary mb-1'>ZARZĄDZANIE</p>
					<h1 className='font-display text-4xl text-on-surface'>
						Albumy publiczne
					</h1>
				</div>
				<Button onClick={() => openModal('create-public-album')}>
					<span className='material-symbols-outlined text-[18px] mr-2'>
						public
					</span>
					Nowy album publiczny
				</Button>
			</div>

			<div className='flex gap-2 mb-8 overflow-x-auto'>
				{categories.map((cat) => (
					<button
						key={cat.key}
						onClick={() => setActiveCategory(cat.key)}
						className={`px-4 py-2 label text-[10px] whitespace-nowrap transition-colors ${
							activeCategory === cat.key
								? 'bg-primary text-on-primary'
								: 'bg-surface-container-low text-on-surface-variant hover:text-on-surface'
						}`}
					>
						{cat.label}
					</button>
				))}
			</div>

			{isLoading ? (
				<div className='flex items-center justify-center py-20'>
					<span className='material-symbols-outlined text-primary animate-spin text-3xl'>
						progress_activity
					</span>
				</div>
			) : filtered.length > 0 ? (
				<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4'>
					{filtered.map((album) => (
						<PublicAlbumCard
							key={album.albumId}
							album={album}
							onDelete={() => deleteAlbum.mutate(album.albumId)}
						/>
					))}
				</div>
			) : (
				<div className='text-center py-20'>
					<span className='material-symbols-outlined text-5xl text-on-surface-variant/30 mb-3 block'>
						public_off
					</span>
					<p className='text-on-surface-variant text-sm'>
						Brak albumów publicznych
					</p>
				</div>
			)}
		</div>
	);
}

function PublicAlbumCard({
	album,
	onDelete,
}: {
	album: Album;
	onDelete: () => void;
}) {
	return (
		<div className='bg-surface-container-low p-4'>
			<PhotoPlaceholder
				label='Okładka publiczna'
				className='aspect-[16/9] mb-3'
			/>
			<p className='text-on-surface text-sm font-medium'>{album.name}</p>
			<p className='text-on-surface-variant text-xs mt-0.5'>
				{album.photoCount} zdjęć
			</p>
			<div className='flex gap-2 mt-3'>
				<Button size='sm' variant='danger' onClick={onDelete}>
					<span className='material-symbols-outlined text-[14px] mr-1'>
						delete
					</span>
					Usuń
				</Button>
			</div>
		</div>
	);
}
