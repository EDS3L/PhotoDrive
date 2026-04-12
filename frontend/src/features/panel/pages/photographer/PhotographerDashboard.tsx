import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
	FolderOpen,
	Image,
	UserCheck,
	AlertTriangle,
	Clock,
	FolderPlus,
	UserPlus,
} from 'lucide-react';
import { StatsCard } from '../../components/shared/StatsCard';
import { LoadingSpinner } from '../../components/shared/LoadingSpinner';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { usePhotographerClients } from '../../hooks/usePhotographerClients';
import {
	usePhotographerAlbums,
	usePhotographerAlbumsWithoutTtd,
} from '../../hooks/usePhotographerAlbums';

function getTtdUrgency(
	ttd: string | null,
): 'expired' | 'urgent' | 'warning' | 'ok' | null {
	if (!ttd) return null;
	const d = new Date(ttd);
	const now = new Date();
	const days = Math.ceil((d.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
	if (days < 0) return 'expired';
	if (days <= 3) return 'urgent';
	if (days <= 7) return 'warning';
	return 'ok';
}

function formatTtd(ttd: string) {
	return new Date(ttd).toLocaleDateString('pl-PL', {
		day: 'numeric',
		month: 'long',
		year: 'numeric',
	});
}

export default function PhotographerDashboard() {
	const { data: clients, isLoading: clientsLoading } = usePhotographerClients();
	const { data: albums, isLoading: albumsLoading } = usePhotographerAlbums();
	const { data: albumsNoTtd, isLoading: ttdLoading } =
		usePhotographerAlbumsWithoutTtd();

	const stats = useMemo(() => {
		if (!clients || !albums) return null;
		const totalPhotos = albums.reduce((s, a) => s + a.files.length, 0);
		return {
			clients: clients.length,
			albums: albums.length,
			photos: totalPhotos,
		};
	}, [clients, albums]);

	const ttdAlerts = useMemo(() => {
		if (!albums) return [];
		return albums
			.filter((a) => a.ttd)
			.map((a) => ({
				album: a,
				urgency: getTtdUrgency(a.ttd)!,
			}))
			.filter((a) => a.urgency !== 'ok')
			.sort((a, b) => {
				const order: Record<string, number> = {
					expired: 0,
					urgent: 1,
					warning: 2,
				};
				return (order[a.urgency] ?? 3) - (order[b.urgency] ?? 3);
			});
	}, [albums]);

	const recentAlbums = useMemo(() => {
		if (!albums) return [];
		return [...albums]
			.sort((a, b) => {
				const lastA = a.files.length
					? Math.max(...a.files.map((f) => new Date(f.uploadedAt).getTime()))
					: 0;
				const lastB = b.files.length
					? Math.max(...b.files.map((f) => new Date(f.uploadedAt).getTime()))
					: 0;
				return lastB - lastA;
			})
			.slice(0, 5);
	}, [albums]);

	if (clientsLoading || albumsLoading || ttdLoading) return <LoadingSpinner />;

	return (
		<div>
			<div className='mb-8'>
				<h2 className='font-serif text-4xl font-light'>Dashboard</h2>
				<p className='text-sm text-muted mt-1'>Przegląd Twoich zasobów</p>
			</div>

			{/* Stats */}
			{stats && (
				<div className='grid grid-cols-1 sm:grid-cols-3 gap-6 mb-8'>
					<StatsCard
						label='Klienci'
						value={stats.clients}
						icon={<UserCheck className='w-6 h-6' />}
					/>
					<StatsCard
						label='Albumy'
						value={stats.albums}
						icon={<FolderOpen className='w-6 h-6' />}
					/>
					<StatsCard
						label='Zdjęcia'
						value={stats.photos}
						icon={<Image className='w-6 h-6' />}
					/>
				</div>
			)}

			{/* TTD Alerts */}
			{ttdAlerts.length > 0 && (
				<div className='mb-8'>
					<h3 className='font-serif text-xl mb-3 flex items-center gap-2'>
						<AlertTriangle className='w-5 h-5 text-yellow-400' />
						Alerty wygaśnięcia
					</h3>
					<div className='space-y-2'>
						{ttdAlerts.map(({ album, urgency }) => (
							<Link
								key={album.albumId}
								to={`/photographer/albums/${album.albumId}`}
								className={`block p-4 border transition-colors hover:border-accent/30 ${
									urgency === 'expired'
										? 'bg-red-900/10 border-red-900/30'
										: urgency === 'urgent'
											? 'bg-orange-900/10 border-orange-900/30'
											: 'bg-yellow-900/10 border-yellow-900/30'
								}`}
							>
								<div className='flex items-center justify-between'>
									<div className='flex items-center gap-3'>
										<Clock
											className={`w-4 h-4 ${
												urgency === 'expired'
													? 'text-red-400'
													: urgency === 'urgent'
														? 'text-orange-400'
														: 'text-yellow-400'
											}`}
										/>
										<span className='text-sm'>{album.name}</span>
									</div>
									<StatusBadge
										variant={
											urgency === 'expired'
												? 'error'
												: urgency === 'urgent'
													? 'error'
													: 'warning'
										}
									>
										{urgency === 'expired'
											? 'Wygasł'
											: `Do: ${formatTtd(album.ttd!)}`}
									</StatusBadge>
								</div>
							</Link>
						))}
					</div>
				</div>
			)}

			{/* No-TTD warning */}
			{albumsNoTtd && albumsNoTtd.length > 0 && (
				<div className='mb-8 p-4 bg-yellow-900/10 border border-yellow-900/30 flex items-center gap-3'>
					<AlertTriangle className='w-5 h-5 text-yellow-400 flex-shrink-0' />
					<div>
						<p className='text-sm text-yellow-400 font-medium'>
							{albumsNoTtd.length}{' '}
							{albumsNoTtd.length === 1 ? 'album nie ma' : 'albumów nie ma'}{' '}
							ustawionego TTD
						</p>
						<Link
							to='/photographer/albums'
							className='text-xs text-yellow-400/70 hover:text-yellow-400'
						>
							Ustaw czas wygaśnięcia →
						</Link>
					</div>
				</div>
			)}

			<div className='grid grid-cols-1 lg:grid-cols-2 gap-8'>
				{/* Recent albums */}
				<div className='bg-surface border border-border p-6'>
					<h3 className='font-serif text-xl mb-4'>Ostatnie albumy</h3>
					{recentAlbums.length === 0 ? (
						<p className='text-sm text-muted'>Brak albumów</p>
					) : (
						<ul className='space-y-3'>
							{recentAlbums.map((album) => (
								<li key={album.albumId}>
									<Link
										to={`/photographer/albums/${album.albumId}`}
										className='flex items-center justify-between py-2 border-b border-border last:border-0 hover:text-accent transition-colors'
									>
										<div className='flex items-center gap-3'>
											<FolderOpen className='w-4 h-4 text-muted' />
											<span className='text-sm'>{album.name}</span>
										</div>
										<span className='text-xs text-muted'>
											{album.files.length} zdjęć
										</span>
									</Link>
								</li>
							))}
						</ul>
					)}
				</div>

				{/* Quick actions */}
				<div className='bg-surface border border-border p-6'>
					<h3 className='font-serif text-xl mb-4'>Szybkie akcje</h3>
					<div className='grid grid-cols-2 gap-3'>
						<Link
							to='/photographer/clients'
							className='flex items-center gap-3 p-4 border border-border hover:border-accent/50 transition-colors'
						>
							<UserPlus className='w-5 h-5 text-accent' />
							<span className='text-sm'>Nowy klient</span>
						</Link>
						<Link
							to='/photographer/albums'
							className='flex items-center gap-3 p-4 border border-border hover:border-accent/50 transition-colors'
						>
							<FolderPlus className='w-5 h-5 text-accent' />
							<span className='text-sm'>Nowy album</span>
						</Link>
					</div>
				</div>
			</div>
		</div>
	);
}
