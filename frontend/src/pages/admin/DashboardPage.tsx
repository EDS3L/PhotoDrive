import { useAlbums, useAlbumsWithoutTtd } from '@/hooks/use-albums';
import { PhotoPlaceholder } from '@/components/ui/PhotoPlaceholder';
import { useUsers } from '@/hooks/use-users';
import { Link } from 'react-router';

export function AdminDashboardPage() {
	const { data: albums } = useAlbums();
	const { data: users } = useUsers();
	const { data: noTtd } = useAlbumsWithoutTtd();

	const albumCount = albums?.length ?? 0;
	const userCount = users?.length ?? 0;
	const noTtdCount = noTtd?.length ?? 0;
	const recentAlbums = albums?.slice(0, 5) ?? [];

	return (
		<div className='p-6 lg:p-10 max-w-7xl mx-auto'>
			<div className='mb-10'>
				<p className='label text-[11px] text-primary mb-1'>
					PANEL ADMINISTRATORA
				</p>
				<h1 className='font-display text-4xl text-on-surface'>Dashboard</h1>
			</div>

			<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-12'>
				<StatCard
					icon='photo_library'
					label='Wszystkie albumy'
					value={albumCount}
				/>
				<StatCard icon='group' label='Użytkownicy' value={userCount} />
				<StatCard
					icon='warning'
					label='Bez daty wygaśnięcia'
					value={noTtdCount}
					accent
				/>
				<StatCard
					icon='cloud_upload'
					label='Ostatnie uploady'
					value={recentAlbums.length}
				/>
			</div>

			<section>
				<div className='flex items-center justify-between mb-6'>
					<h2 className='font-display text-2xl text-on-surface'>
						Ostatnie albumy
					</h2>
					<Link
						to='/admin/albumy'
						className='text-primary text-sm flex items-center gap-1'
					>
						Wszystkie
						<span className='material-symbols-outlined text-[16px]'>
							arrow_forward
						</span>
					</Link>
				</div>

				{recentAlbums.length > 0 ? (
					<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4'>
						{recentAlbums.map((album) => (
							<Link
								key={album.albumId}
								to={`/admin/albumy/${album.albumId}`}
								className='group bg-surface-container-low hover:bg-surface-container transition-colors p-4'
							>
								<PhotoPlaceholder
									label='Okładka albumu'
									className='aspect-[4/3] mb-3'
								/>
								<p className='text-on-surface text-sm font-medium truncate'>
									{album.name}
								</p>
								<p className='text-on-surface-variant text-xs mt-0.5'>
									{album.photoCount} zdjęć
								</p>
							</Link>
						))}
					</div>
				) : (
					<EmptyState icon='photo_library' message='Brak albumów' />
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
		<div className='bg-surface-container-low p-6 flex items-start gap-4'>
			<span
				className={`material-symbols-outlined text-3xl ${accent ? 'text-warning' : 'text-primary'}`}
			>
				{icon}
			</span>
			<div>
				<p className='font-display text-3xl text-on-surface'>{value}</p>
				<p className='label text-[10px] text-on-surface-variant mt-1'>
					{label}
				</p>
			</div>
		</div>
	);
}

function EmptyState({ icon, message }: { icon: string; message: string }) {
	return (
		<div className='flex flex-col items-center justify-center py-16 text-center'>
			<span className='material-symbols-outlined text-5xl text-on-surface-variant/30 mb-3'>
				{icon}
			</span>
			<p className='text-on-surface-variant text-sm'>{message}</p>
		</div>
	);
}
