import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { FolderPlus, Image, Clock, AlertTriangle, Loader2 } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { Input } from '@/shared/components/ui/Input';
import { Select } from '@/shared/components/ui/Select';
import { Modal } from '../../components/shared/Modal';
import { SearchInput } from '../../components/shared/SearchInput';
import { LoadingSpinner } from '../../components/shared/LoadingSpinner';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { EmptyState } from '../../components/shared/EmptyState';
import {
	usePhotographerAlbums,
	useCreateClientAlbum,
} from '../../hooks/usePhotographerAlbums';
import { usePhotographerClients } from '../../hooks/usePhotographerClients';
import { getPhotoUrl } from '../../api/photographerApi';
import type { AlbumDto } from '@/shared/types/api';

type TtdFilter = 'ALL' | 'WITH' | 'WITHOUT' | 'EXPIRED';

function isTtdExpired(ttd: string | null): boolean {
	if (!ttd) return false;
	return new Date(ttd) < new Date();
}

function formatDate(date: string) {
	return new Date(date).toLocaleDateString('pl-PL');
}

export default function PhotographerAlbums() {
	const { data: albums, isLoading: albumsLoading } = usePhotographerAlbums();
	const { data: clients, isLoading: clientsLoading } = usePhotographerClients();
	const createMutation = useCreateClientAlbum();

	const [search, setSearch] = useState('');
	const [clientFilter, setClientFilter] = useState('ALL');
	const [ttdFilter, setTtdFilter] = useState<TtdFilter>('ALL');
	const [createOpen, setCreateOpen] = useState(false);
	const [newName, setNewName] = useState('');
	const [newClientId, setNewClientId] = useState('');
	const [newTtd, setNewTtd] = useState('');

	const filtered = useMemo(() => {
		if (!albums) return [];
		return albums.filter((a) => {
			const q = search.toLowerCase();
			if (q && !a.name.toLowerCase().includes(q)) return false;
			if (clientFilter !== 'ALL' && a.clientId !== clientFilter) return false;
			if (ttdFilter === 'WITH' && !a.ttd) return false;
			if (ttdFilter === 'WITHOUT' && a.ttd) return false;
			if (ttdFilter === 'EXPIRED' && !isTtdExpired(a.ttd)) return false;
			return true;
		});
	}, [albums, search, clientFilter, ttdFilter]);

	const clientMap = useMemo(() => {
		const map = new Map<string, string>();
		clients?.forEach((c) => map.set(c.id.value, c.name));
		return map;
	}, [clients]);

	const handleCreate = () => {
		if (!newClientId || !newName) return;
		createMutation.mutate(
			{
				clientId: newClientId,
				name: newName,
			},
			{
				onSuccess: () => {
					setCreateOpen(false);
					setNewName('');
					setNewClientId('');
					setNewTtd('');
				},
			},
		);
	};

	if (albumsLoading || clientsLoading) return <LoadingSpinner />;

	return (
		<div>
			{/* Header */}
			<div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6'>
				<div>
					<h2 className='font-serif text-4xl font-light'>Albumy</h2>
					<p className='text-sm text-muted mt-1'>Albumy Twoich klientów</p>
				</div>
				<Button onClick={() => setCreateOpen(true)}>
					<FolderPlus className='w-4 h-4 mr-2' />
					Nowy album
				</Button>
			</div>

			{/* Filters */}
			<div className='flex flex-col sm:flex-row gap-4 mb-6'>
				<SearchInput
					value={search}
					onChange={setSearch}
					placeholder='Szukaj albumu...'
					className='w-full sm:w-64'
				/>
				<Select
					value={clientFilter}
					onChange={(e) => setClientFilter(e.target.value)}
					className='w-full sm:w-48'
				>
					<option value='ALL'>Wszyscy klienci</option>
					{clients?.map((c) => (
						<option key={c.id.value} value={c.id.value}>
							{c.name}
						</option>
					))}
				</Select>
				<Select
					value={ttdFilter}
					onChange={(e) => setTtdFilter(e.target.value as TtdFilter)}
					className='w-full sm:w-48'
				>
					<option value='ALL'>Wszystkie TTD</option>
					<option value='WITH'>Z datą wygaśnięcia</option>
					<option value='WITHOUT'>Bez daty</option>
					<option value='EXPIRED'>Wygasłe</option>
				</Select>
			</div>

			{/* Albums grid */}
			{filtered.length === 0 ? (
				<EmptyState
					icon={<FolderPlus className='w-12 h-12' />}
					title='Brak albumów'
					description='Utwórz album dla klienta'
					action={
						<Button onClick={() => setCreateOpen(true)} size='sm'>
							<FolderPlus className='w-4 h-4 mr-2' />
							Nowy album
						</Button>
					}
				/>
			) : (
				<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6'>
					{filtered.map((album) => (
						<AlbumCard
							key={album.albumId}
							album={album}
							clientName={album.clientId ? clientMap.get(album.clientId) : undefined}
						/>
					))}
				</div>
			)}

			{/* Create modal */}
			<Modal
				open={createOpen}
				onClose={() => setCreateOpen(false)}
				title='Nowy album'
				maxWidth='max-w-md'
			>
				<div className='p-8 space-y-6'>
					<div>
						<label className='text-[10px] uppercase tracking-widest text-muted block mb-2'>
							Klient
						</label>
						<Select
							value={newClientId}
							onChange={(e) => setNewClientId(e.target.value)}
							className='w-full'
						>
							<option value=''>Wybierz klienta</option>
							{clients?.map((c) => (
								<option key={c.id.value} value={c.id.value}>
									{c.name} ({c.email.value})
								</option>
							))}
						</Select>
					</div>
					<Input
						id='album-name'
						label='Nazwa albumu'
						placeholder='np. Ślub 12.06.2025'
						value={newName}
						onChange={(e) => setNewName(e.target.value)}
					/>
					<Input
						id='album-ttd'
						label='Data wygaśnięcia (opcjonalna)'
						type='date'
						value={newTtd}
						onChange={(e) => setNewTtd(e.target.value)}
					/>
					<p className='text-xs text-muted'>
						Po wygaśnięciu klient nie będzie miał dostępu do albumu
					</p>
				</div>
				<div className='px-8 py-6 border-t border-border flex justify-end gap-3'>
					<Button variant='ghost' onClick={() => setCreateOpen(false)}>
						Anuluj
					</Button>
					<Button
						onClick={handleCreate}
						disabled={
							!newClientId || !newName.trim() || createMutation.isPending
						}
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

function AlbumCard({
	album,
	clientName,
}: {
	album: AlbumDto;
	clientName?: string;
}) {
	const expired = isTtdExpired(album.ttd);
	const coverFile = album.files[0];

	return (
		<Link
			to={`/photographer/albums/${album.albumId}`}
			className='group bg-surface border border-border overflow-hidden hover:border-accent/30 transition-colors'
		>
			{/* Cover */}
			<div className='aspect-[4/3] bg-surface-light relative overflow-hidden'>
				{coverFile ? (
					<img
						src={getPhotoUrl(album.albumId, coverFile.fileName, 400)}
						alt={album.name}
						className='w-full h-full object-cover group-hover:scale-105 transition-transform duration-500'
					/>
				) : (
					<div className='w-full h-full flex items-center justify-center'>
						<Image className='w-10 h-10 text-muted/20' />
					</div>
				)}

				{expired && (
					<div className='absolute inset-0 bg-red-900/40 flex items-center justify-center'>
						<StatusBadge variant='error'>Wygasł</StatusBadge>
					</div>
				)}
			</div>

			{/* Info */}
			<div className='p-4'>
				<h4 className='font-medium text-sm truncate'>{album.name}</h4>
				{clientName && (
					<p className='text-xs text-muted mt-1 truncate'>{clientName}</p>
				)}
				<div className='flex items-center justify-between mt-3'>
					<span className='text-xs text-muted flex items-center gap-1'>
						<Image className='w-3 h-3' />
						{album.files.length}
					</span>
					{album.ttd && !expired && (
						<span className='text-xs text-muted flex items-center gap-1'>
							<Clock className='w-3 h-3' />
							{formatDate(album.ttd)}
						</span>
					)}
					{!album.ttd && (
						<span className='text-xs text-yellow-400 flex items-center gap-1'>
							<AlertTriangle className='w-3 h-3' />
							Brak TTD
						</span>
					)}
				</div>
			</div>
		</Link>
	);
}
