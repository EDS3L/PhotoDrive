import { useState } from 'react';
import { Link } from 'react-router';
import { useAlbums, useDeleteAlbum } from '@/hooks/use-albums';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';

import { useUiStore } from '@/lib/stores/ui-store';
import type { Album } from '@/types/album';
import { PhotoPlaceholder } from '@/components/ui/PhotoPlaceholder';

export function AlbumsManagementPage() {
	const [search, setSearch] = useState('');
	const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

	const { data: albums, isLoading } = useAlbums();
	const deleteAlbum = useDeleteAlbum();
	const openModal = useUiStore((s) => s.openModal);

	const filtered = (albums ?? [])
		.filter((a) => !a.isPublic)
		.filter((a) => a.name.toLowerCase().includes(search.toLowerCase()));

	return (
		<div className='p-6 lg:p-10 max-w-7xl mx-auto'>
			<div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-10'>
				<div>
					<p className='label text-[11px] text-primary mb-1'>ZARZĄDZANIE</p>
					<h1 className='font-display text-4xl text-on-surface'>Albumy</h1>
				</div>
				<Button onClick={() => openModal('new-album')}>
					<span className='material-symbols-outlined text-[18px] mr-2'>
						add_photo_alternate
					</span>
					Nowy album
				</Button>
			</div>

			<div className='flex flex-col sm:flex-row gap-4 mb-8 items-start sm:items-center justify-between'>
				<Input
					placeholder='Szukaj albumu...'
					value={search}
					onChange={(e) => setSearch(e.target.value)}
					className='max-w-sm'
				/>
				<div className='flex gap-1'>
					<button
						onClick={() => setViewMode('grid')}
						className={`p-2 transition-colors ${viewMode === 'grid' ? 'text-primary' : 'text-on-surface-variant'}`}
					>
						<span className='material-symbols-outlined text-[20px]'>
							grid_view
						</span>
					</button>
					<button
						onClick={() => setViewMode('list')}
						className={`p-2 transition-colors ${viewMode === 'list' ? 'text-primary' : 'text-on-surface-variant'}`}
					>
						<span className='material-symbols-outlined text-[20px]'>
							view_list
						</span>
					</button>
				</div>
			</div>

			{isLoading ? (
				<div className='flex items-center justify-center py-20'>
					<span className='material-symbols-outlined text-primary animate-spin text-3xl'>
						progress_activity
					</span>
				</div>
			) : filtered.length > 0 ? (
				viewMode === 'grid' ? (
					<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4'>
						{filtered.map((album) => (
							<AlbumCard
								key={album.albumId}
								album={album}
								onDelete={() => deleteAlbum.mutate(album.albumId)}
							/>
						))}
					</div>
				) : (
					<div className='flex flex-col gap-2'>
						{filtered.map((album) => (
							<AlbumRow
								key={album.albumId}
								album={album}
								onDelete={() => deleteAlbum.mutate(album.albumId)}
							/>
						))}
					</div>
				)
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

function AlbumCard({
	album,
	onDelete,
}: {
	album: Album;
	onDelete: () => void;
}) {
	return (
		<Link
			to={`/admin/albumy/${album.albumId}`}
			className='group bg-surface-container-low hover:bg-surface-container transition-colors'
		>
			<PhotoPlaceholder label='Okładka albumu' className='aspect-[4/3]' />
			<div className='p-4'>
				<p className='text-on-surface text-sm font-medium truncate'>
					{album.name}
				</p>
				<div className='flex items-center justify-between mt-2'>
					<span className='text-on-surface-variant text-xs'>
						{album.photoCount} zdjęć
					</span>
					<button
						onClick={(e) => {
							e.preventDefault();
							e.stopPropagation();
							onDelete();
						}}
						className='text-on-surface-variant hover:text-error transition-colors p-1'
					>
						<span className='material-symbols-outlined text-[16px]'>
							delete
						</span>
					</button>
				</div>
			</div>
		</Link>
	);
}

function AlbumRow({ album, onDelete }: { album: Album; onDelete: () => void }) {
	return (
		<Link
			to={`/admin/albumy/${album.albumId}`}
			className='flex items-center gap-4 bg-surface-container-low hover:bg-surface-container transition-colors p-3'
		>
			<div className='w-16 h-12 bg-surface-container flex-shrink-0' />
			<div className='flex-1 min-w-0'>
				<p className='text-on-surface text-sm font-medium truncate'>
					{album.name}
				</p>
				<p className='text-on-surface-variant text-xs'>
					{album.photoCount} zdjęć
				</p>
			</div>
			<button
				onClick={(e) => {
					e.preventDefault();
					e.stopPropagation();
					onDelete();
				}}
				className='text-on-surface-variant hover:text-error transition-colors p-1'
			>
				<span className='material-symbols-outlined text-[16px]'>delete</span>
			</button>
		</Link>
	);
}
