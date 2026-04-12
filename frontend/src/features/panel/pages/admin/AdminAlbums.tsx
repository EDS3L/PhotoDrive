import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
	FolderPlus,
	FolderOpen,
	Image,
	Eye,
	Clock,
	AlertTriangle,
	Globe,
	Loader2,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { Input } from '@/shared/components/ui/Input';
import { Modal } from '../../components/shared/Modal';
import { SearchInput } from '../../components/shared/SearchInput';
import { LoadingSpinner } from '../../components/shared/LoadingSpinner';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { EmptyState } from '../../components/shared/EmptyState';
import {
	useAdminAlbums,
	useCreateAdminAlbum,
} from '../../hooks/useAdminAlbums';
import { getPhotoUrl } from '../../api/adminApi';
import type { AlbumDto } from '@/shared/types/api';

type TypeFilter = 'ALL' | 'ADMIN' | 'CLIENT';
type TtdFilter = 'ALL' | 'WITH' | 'WITHOUT' | 'EXPIRED';

function isAdminAlbum(album: AlbumDto) {
	return album.photographId === album.clientId;
}

function isTtdExpired(ttd: string | null) {
	if (!ttd) return false;
	return new Date(ttd) < new Date();
}

function formatDate(d: string | null) {
	if (!d) return null;
	return new Date(d).toLocaleDateString('pl-PL', {
		day: 'numeric',
		month: 'long',
		year: 'numeric',
	});
}

export default function AdminAlbums() {
	const { data: albums, isLoading } = useAdminAlbums();
	const createMutation = useCreateAdminAlbum();

	const [search, setSearch] = useState('');
	const [typeFilter, setTypeFilter] = useState<TypeFilter>('ALL');
	const [ttdFilter, setTtdFilter] = useState<TtdFilter>('ALL');
	const [createOpen, setCreateOpen] = useState(false);
	const [newAlbumName, setNewAlbumName] = useState('');

	const filtered = useMemo(() => {
		if (!albums) return [];
		return albums.filter((a) => {
			const q = search.toLowerCase();
			const matchSearch = !q || a.name.toLowerCase().includes(q);
			const admin = isAdminAlbum(a);
			const matchType =
				typeFilter === 'ALL' || (typeFilter === 'ADMIN' ? admin : !admin);
			const matchTtd =
				ttdFilter === 'ALL' ||
				(ttdFilter === 'WITH' && a.ttd) ||
				(ttdFilter === 'WITHOUT' && !a.ttd) ||
				(ttdFilter === 'EXPIRED' && isTtdExpired(a.ttd));
			return matchSearch && matchType && matchTtd;
		});
	}, [albums, search, typeFilter, ttdFilter]);

	const handleCreate = () => {
		createMutation.mutate(newAlbumName, {
			onSuccess: () => {
				setCreateOpen(false);
				setNewAlbumName('');
			},
		});
	};

	if (isLoading) return <LoadingSpinner />;

	return (
		<div>
			{/* Header */}
			<div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6'>
				<div>
					<h2 className='font-serif text-4xl font-light'>Albumy</h2>
					<p className='text-sm text-muted mt-1'>Wszystkie albumy w systemie</p>
				</div>
				<Button onClick={() => setCreateOpen(true)}>
					<FolderPlus className='w-4 h-4 mr-2' />
					Nowy album
				</Button>
			</div>

			{/* Filters */}
			<div className='bg-surface border border-border p-4 mb-6 flex flex-col sm:flex-row gap-4 items-start sm:items-center'>
				<SearchInput
					value={search}
					onChange={setSearch}
					placeholder='Szukaj albumu...'
					className='w-full sm:w-64'
				/>
				<select
					value={typeFilter}
					onChange={(e) => setTypeFilter(e.target.value as TypeFilter)}
					className='bg-transparent border-b border-border py-2 text-sm text-foreground focus:border-accent focus:outline-none'
				>
					<option value='ALL' className='bg-surface'>
						Wszystkie typy
					</option>
					<option value='ADMIN' className='bg-surface'>
						Administracyjne
					</option>
					<option value='CLIENT' className='bg-surface'>
						Klientów
					</option>
				</select>
				<select
					value={ttdFilter}
					onChange={(e) => setTtdFilter(e.target.value as TtdFilter)}
					className='bg-transparent border-b border-border py-2 text-sm text-foreground focus:border-accent focus:outline-none'
				>
					<option value='ALL' className='bg-surface'>
						TTD: Wszystkie
					</option>
					<option value='WITH' className='bg-surface'>
						Z TTD
					</option>
					<option value='WITHOUT' className='bg-surface'>
						Bez TTD
					</option>
					<option value='EXPIRED' className='bg-surface'>
						Wygasłe
					</option>
				</select>
				<span className='text-xs text-muted ml-auto'>
					{filtered.length} albumów
				</span>
			</div>

			{/* Albums grid */}
			{filtered.length === 0 ? (
				<EmptyState
					icon={<FolderOpen className='w-12 h-12' />}
					title='Brak albumów'
					description='Nie znaleziono albumów spełniających kryteria'
				/>
			) : (
				<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6'>
					{filtered.map((album) => (
						<AlbumCard key={album.albumId} album={album} />
					))}
				</div>
			)}

			{/* Create Album Modal */}
			<Modal
				open={createOpen}
				onClose={() => setCreateOpen(false)}
				title='Nowy album administracyjny'
			>
				<div className='p-8 space-y-6'>
					<Input
						id='album-name'
						label='Nazwa albumu'
						placeholder='np. Portfolio — Śluby'
						value={newAlbumName}
						onChange={(e) => setNewAlbumName(e.target.value)}
					/>
					<p className='text-xs text-muted'>
						Album administracyjny — możesz go później ustawić jako publiczny.
					</p>
				</div>
				<div className='px-8 py-6 border-t border-border flex justify-end gap-3'>
					<Button variant='ghost' onClick={() => setCreateOpen(false)}>
						Anuluj
					</Button>
					<Button
						onClick={handleCreate}
						disabled={createMutation.isPending || !newAlbumName.trim()}
					>
						{createMutation.isPending ? (
							<Loader2 className='w-4 h-4 mr-2 animate-spin' />
						) : (
							<FolderPlus className='w-4 h-4 mr-2' />
						)}
						Utwórz album
					</Button>
				</div>
			</Modal>
		</div>
	);
}

function AlbumCard({ album }: { album: AlbumDto }) {
	const admin = isAdminAlbum(album);
	const expired = isTtdExpired(album.ttd);
	const visibleCount = album.files.filter((f) => f.visible).length;
	const coverFile = album.files[0];

	return (
		<Link
			to={`/admin/albums/${album.albumId}`}
			className='bg-surface border border-border overflow-hidden hover:border-accent/30 transition-colors block'
		>
			{/* Cover */}
			<div className='aspect-[16/10] relative bg-surface-light'>
				{coverFile ? (
					<img
						src={getPhotoUrl(album.albumId, coverFile.fileName, 400)}
						alt={album.name}
						className='w-full h-full object-cover'
					/>
				) : (
					<div className='w-full h-full flex items-center justify-center'>
						<FolderOpen className='w-10 h-10 text-muted/30' />
					</div>
				)}
				{expired && (
					<div className='absolute inset-0 bg-black/60 flex items-center justify-center'>
						<span className='text-sm font-medium tracking-wider text-red-400'>
							WYGASŁ
						</span>
					</div>
				)}
				{!album.ttd && !admin && (
					<div className='absolute top-2 right-2 bg-black/60 p-1'>
						<AlertTriangle className='w-3.5 h-3.5 text-yellow-400' />
					</div>
				)}
				{album.isPublic && (
					<div className='absolute top-2 left-2 bg-black/60 px-2 py-0.5'>
						<Globe className='w-3 h-3 text-accent inline mr-1' />
						<span className='text-[10px] text-accent'>Publiczny</span>
					</div>
				)}
			</div>

			{/* Info */}
			<div className='p-5'>
				<div className='flex items-start justify-between gap-2 mb-2'>
					<h3 className='text-[15px] font-medium leading-tight'>
						{album.name}
					</h3>
					<StatusBadge variant={admin ? 'accent' : 'default'}>
						{admin ? 'Admin' : 'Klient'}
					</StatusBadge>
				</div>

				<div className='flex items-center gap-3 text-xs text-muted'>
					<span className='flex items-center gap-1'>
						<Image className='w-3 h-3' />
						{album.files.length} zdjęć
					</span>
					<span className='flex items-center gap-1'>
						<Eye className='w-3 h-3' />
						{visibleCount} widoczne
					</span>
				</div>

				{album.ttd && (
					<div className='mt-2 flex items-center gap-1 text-xs'>
						<Clock
							className={`w-3 h-3 ${expired ? 'text-red-400' : 'text-muted'}`}
						/>
						<span className={expired ? 'text-red-400' : 'text-muted'}>
							{expired ? 'Wygasł' : `Do: ${formatDate(album.ttd)}`}
						</span>
					</div>
				)}
				{!album.ttd && !admin && (
					<div className='mt-2 flex items-center gap-1 text-xs text-yellow-400'>
						<AlertTriangle className='w-3 h-3' />
						Brak TTD
					</div>
				)}
			</div>
		</Link>
	);
}
