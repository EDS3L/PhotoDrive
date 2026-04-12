import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
	Users,
	FolderOpen,
	Image,
	UserCheck,
	AlertTriangle,
	UserPlus,
	FolderPlus,
	Globe,
	Settings,
} from 'lucide-react';
import { StatsCard } from '../../components/shared/StatsCard';
import { LoadingSpinner } from '../../components/shared/LoadingSpinner';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { useAllUsers } from '../../hooks/useUsers';
import {
	useAdminAlbums,
	useAdminAlbumsWithoutTtd,
} from '../../hooks/useAdminAlbums';
import type { AlbumDto } from '@/shared/types/api';
import type { UserInfo } from '../../types/panel';

function computeStats(
	users: UserInfo[],
	albums: AlbumDto[],
	albumsNoTtd: AlbumDto[],
) {
	const totalPhotos = albums.reduce((sum, a) => sum + a.files.length, 0);
	const activeUsers = users.filter((u) => u.isActive).length;
	return {
		totalUsers: users.length,
		totalAlbums: albums.length,
		totalPhotos,
		activeUsers,
		albumsWithoutTtd: albumsNoTtd.length,
	};
}

function getRecentActivity(_users: UserInfo[], albums: AlbumDto[]) {
	const items: { label: string; detail: string; type: 'user' | 'album' }[] = [];

	const sortedAlbums = [...albums]
		.filter((a) => a.files.length > 0)
		.sort((a, b) => {
			const lastA = Math.max(
				...a.files.map((f) => new Date(f.uploadedAt).getTime()),
			);
			const lastB = Math.max(
				...b.files.map((f) => new Date(f.uploadedAt).getTime()),
			);
			return lastB - lastA;
		})
		.slice(0, 5);

	for (const album of sortedAlbums) {
		const lastUpload = album.files.reduce((latest, f) =>
			new Date(f.uploadedAt) > new Date(latest.uploadedAt) ? f : latest,
		);
		items.push({
			label: `Nowe zdjęcie w "${album.name}"`,
			detail: new Date(lastUpload.uploadedAt).toLocaleDateString('pl-PL'),
			type: 'album',
		});
	}

	return items.slice(0, 5);
}

export default function AdminDashboard() {
	const { data: users, isLoading: usersLoading } = useAllUsers();
	const { data: albums, isLoading: albumsLoading } = useAdminAlbums();
	const { data: albumsNoTtd, isLoading: ttdLoading } =
		useAdminAlbumsWithoutTtd();

	const stats = useMemo(
		() =>
			users && albums && albumsNoTtd
				? computeStats(users, albums, albumsNoTtd)
				: null,
		[users, albums, albumsNoTtd],
	);

	const activity = useMemo(
		() => (users && albums ? getRecentActivity(users, albums) : []),
		[users, albums],
	);

	if (usersLoading || albumsLoading || ttdLoading) return <LoadingSpinner />;

	return (
		<div>
			<div className='mb-8'>
				<h2 className='font-serif text-4xl font-light'>Dashboard</h2>
				<p className='text-sm text-muted mt-1'>Przegląd systemu PhotoDrive</p>
			</div>

			{/* Stats */}
			{stats && (
				<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8'>
					<StatsCard
						label='Użytkownicy'
						value={stats.totalUsers}
						icon={<Users className='w-6 h-6' />}
					/>
					<StatsCard
						label='Albumy'
						value={stats.totalAlbums}
						icon={<FolderOpen className='w-6 h-6' />}
					/>
					<StatsCard
						label='Zdjęcia'
						value={stats.totalPhotos}
						icon={<Image className='w-6 h-6' />}
					/>
					<StatsCard
						label='Aktywni użytkownicy'
						value={stats.activeUsers}
						icon={<UserCheck className='w-6 h-6' />}
					/>
				</div>
			)}

			{/* TTD Warning */}
			{stats && stats.albumsWithoutTtd > 0 && (
				<div className='mb-8 p-4 bg-yellow-900/10 border border-yellow-900/30 flex items-center gap-3'>
					<AlertTriangle className='w-5 h-5 text-yellow-400 flex-shrink-0' />
					<div>
						<p className='text-sm text-yellow-400 font-medium'>
							{stats.albumsWithoutTtd}{' '}
							{stats.albumsWithoutTtd === 1 ? 'album nie ma' : 'albumów nie ma'}{' '}
							ustawionego czasu wygaśnięcia (TTD)
						</p>
						<Link
							to='/admin/albums'
							className='text-xs text-yellow-400/70 hover:text-yellow-400 transition-colors'
						>
							Przejdź do albumów →
						</Link>
					</div>
				</div>
			)}

			<div className='grid grid-cols-1 lg:grid-cols-2 gap-8'>
				{/* Recent activity */}
				<div className='bg-surface border border-border p-6'>
					<h3 className='font-serif text-xl mb-4'>Ostatnia aktywność</h3>
					{activity.length === 0 ? (
						<p className='text-sm text-muted'>Brak ostatniej aktywności</p>
					) : (
						<ul className='space-y-3'>
							{activity.map((item, i) => (
								<li
									key={i}
									className='flex items-center justify-between py-2 border-b border-border last:border-0'
								>
									<div className='flex items-center gap-3'>
										{item.type === 'album' ? (
											<FolderOpen className='w-4 h-4 text-muted' />
										) : (
											<Users className='w-4 h-4 text-muted' />
										)}
										<span className='text-sm'>{item.label}</span>
									</div>
									<StatusBadge variant='default'>{item.detail}</StatusBadge>
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
							to='/admin/users'
							className='flex items-center gap-3 p-4 border border-border hover:border-accent/50 transition-colors'
						>
							<UserPlus className='w-5 h-5 text-accent' />
							<span className='text-sm'>Dodaj użytkownika</span>
						</Link>
						<Link
							to='/admin/albums'
							className='flex items-center gap-3 p-4 border border-border hover:border-accent/50 transition-colors'
						>
							<FolderPlus className='w-5 h-5 text-accent' />
							<span className='text-sm'>Nowy album</span>
						</Link>
						<Link
							to='/admin/public-albums'
							className='flex items-center gap-3 p-4 border border-border hover:border-accent/50 transition-colors'
						>
							<Globe className='w-5 h-5 text-accent' />
							<span className='text-sm'>Albumy publiczne</span>
						</Link>
						<Link
							to='/admin/users'
							className='flex items-center gap-3 p-4 border border-border hover:border-accent/50 transition-colors'
						>
							<Settings className='w-5 h-5 text-accent' />
							<span className='text-sm'>Zarządzaj rolami</span>
						</Link>
					</div>
				</div>
			</div>
		</div>
	);
}
