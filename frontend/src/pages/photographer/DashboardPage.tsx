import {
	useAssignedAlbums,
	useAssignedAlbumsWithoutTtd,
} from '@/hooks/use-albums';
import { PhotoPlaceholder } from '@/components/ui/PhotoPlaceholder';
import { useAssignedUsers } from '@/hooks/use-users';
import { Link } from 'react-router';

export function PhotographerDashboardPage() {
	const { data: albums } = useAssignedAlbums();
	const { data: clients } = useAssignedUsers();
	const { data: noTtd } = useAssignedAlbumsWithoutTtd();

	const albumCount = albums?.length ?? 0;
	const clientCount = clients?.length ?? 0;
	const noTtdCount = noTtd?.length ?? 0;
	const recentAlbums = albums?.slice(0, 6) ?? [];

	return (
		<div className='p-6 lg:p-10 max-w-7xl mx-auto'>
			<div className='mb-10'>
				<p className='label text-[11px] text-primary mb-1'>PANEL FOTOGRAFA</p>
				<h1 className='font-display text-4xl text-on-surface'>Dashboard</h1>
			</div>

			<div className='grid grid-cols-1 sm:grid-cols-3 gap-4 mb-12'>
				<StatCard icon='photo_library' label='Moje albumy' value={albumCount} />
				<StatCard icon='group' label='Klienci' value={clientCount} />
				<StatCard icon='warning' label='Bez TTD' value={noTtdCount} accent />
			</div>

			<section>
				<div className='flex items-center justify-between mb-6'>
					<h2 className='font-display text-2xl text-on-surface'>
						Ostatnie albumy
					</h2>
					<Link
						to='/fotograf/albumy'
						className='text-primary text-sm flex items-center gap-1'
					>
						Wszystkie
						<span className='material-symbols-outlined text-[16px]'>
							arrow_forward
						</span>
					</Link>
				</div>

				{recentAlbums.length > 0 ? (
					<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4'>
						{recentAlbums.map((album) => (
							<Link
								key={album.albumId}
								to={`/fotograf/albumy/${album.albumId}`}
								className='group bg-surface-container-low p-4 hover:bg-surface-container transition-colors'
							>
								<PhotoPlaceholder
									label='Okładka albumu'
									className='aspect-[16/10] mb-3'
								/>
								<p className='text-on-surface text-sm font-medium group-hover:text-primary transition-colors'>
									{album.name}
								</p>
								<p className='text-on-surface-variant text-xs mt-0.5'>
									{album.photoCount} zdjęć
								</p>
							</Link>
						))}
					</div>
				) : (
					<div className='text-center py-12'>
						<span className='material-symbols-outlined text-5xl text-on-surface-variant/30 mb-3 block'>
							photo_library
						</span>
						<p className='text-on-surface-variant text-sm'>
							Brak przypisanych albumów
						</p>
					</div>
				)}
			</section>
		</div>
	);
}

function StatCard({
	icon,
	label,
	value,
	accent,
}: {
	icon: string;
	label: string;
	value: number;
	accent?: boolean;
}) {
	return (
		<div className='bg-surface-container-low p-5'>
			<div className='flex items-center gap-3 mb-3'>
				<span
					className={`material-symbols-outlined text-xl ${accent ? 'text-warning' : 'text-primary'}`}
				>
					{icon}
				</span>
				<span className='label text-[10px] text-on-surface-variant'>
					{label}
				</span>
			</div>
			<p
				className={`font-display text-3xl ${accent ? 'text-warning' : 'text-on-surface'}`}
			>
				{value}
			</p>
		</div>
	);
}
