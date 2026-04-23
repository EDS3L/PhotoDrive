import { Link } from 'react-router';
import { useAssignedAlbums } from '@/hooks/use-albums';
import { PhotoPlaceholder } from '@/components/ui/PhotoPlaceholder';

export function ClientDashboardPage() {
	const { data: albums, isLoading } = useAssignedAlbums();
	const allAlbums = albums ?? [];

	return (
		<div className='p-6 lg:p-10 max-w-7xl mx-auto'>
			<div className='mb-10'>
				<p className='label text-[11px] text-primary mb-1'>STREFA KLIENTA</p>
				<h1 className='font-display text-4xl text-on-surface'>Twoje albumy</h1>
			</div>

			{isLoading ? (
				<div className='flex items-center justify-center py-20'>
					<span className='material-symbols-outlined text-primary animate-spin text-3xl'>
						progress_activity
					</span>
				</div>
			) : allAlbums.length > 0 ? (
				<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4'>
					{allAlbums.map((album) => (
						<Link
							key={album.albumId}
							to={`/klient/album/${album.albumId}`}
							className='group bg-surface-container-low hover:bg-surface-container transition-colors'
						>
							<PhotoPlaceholder
								label='Okładka albumu'
								className='aspect-[4/3]'
							/>
							<div className='p-4'>
								<p className='text-on-surface text-sm font-medium group-hover:text-primary transition-colors'>
									{album.name}
								</p>
								<p className='text-on-surface-variant text-xs mt-0.5'>
									{album.photoCount} zdjęć
								</p>
							</div>
						</Link>
					))}
				</div>
			) : (
				<div className='flex flex-col items-center justify-center py-24 text-center'>
					<span className='material-symbols-outlined text-5xl text-on-surface-variant/30 mb-3'>
						photo_library
					</span>
					<p className='text-on-surface-variant text-sm'>
						Brak albumów do wyświetlenia
					</p>
				</div>
			)}
		</div>
	);
}
