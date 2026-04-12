import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { UserPlus, FolderOpen, Loader2 } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { Input } from '@/shared/components/ui/Input';
import { Modal } from '../../components/shared/Modal';
import { SearchInput } from '../../components/shared/SearchInput';
import { LoadingSpinner } from '../../components/shared/LoadingSpinner';
import { EmptyState } from '../../components/shared/EmptyState';
import {
	usePhotographerClients,
	useCreateClient,
} from '../../hooks/usePhotographerClients';
import { usePhotographerAlbums } from '../../hooks/usePhotographerAlbums';
import type { UserInfo } from '../../types/panel';

export default function PhotographerClients() {
	const { data: clients, isLoading: clientsLoading } = usePhotographerClients();
	const { data: albums, isLoading: albumsLoading } = usePhotographerAlbums();
	const createMutation = useCreateClient();

	const [search, setSearch] = useState('');
	const [addOpen, setAddOpen] = useState(false);
	const [newName, setNewName] = useState('');
	const [newEmail, setNewEmail] = useState('');
	const [newPassword, setNewPassword] = useState('');

	const filtered = useMemo(() => {
		if (!clients) return [];
		const q = search.toLowerCase();
		if (!q) return clients;
		return clients.filter(
			(c) =>
				c.name.toLowerCase().includes(q) ||
				c.email.value.toLowerCase().includes(q),
		);
	}, [clients, search]);

	const albumsByClient = useMemo(() => {
		if (!albums) return new Map<string, typeof albums>();
		const map = new Map<string, typeof albums>();
		for (const album of albums) {
			const existing = map.get(album.clientId) ?? [];
			existing.push(album);
			map.set(album.clientId, existing);
		}
		return map;
	}, [albums]);

	const handleCreate = () => {
		createMutation.mutate(
			{ name: newName, email: newEmail, password: newPassword },
			{
				onSuccess: () => {
					setAddOpen(false);
					setNewName('');
					setNewEmail('');
					setNewPassword('');
				},
			},
		);
	};

	if (clientsLoading || albumsLoading) return <LoadingSpinner />;

	return (
		<div>
			{/* Header */}
			<div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6'>
				<div>
					<h2 className='font-serif text-4xl font-light'>Moi klienci</h2>
					<p className='text-sm text-muted mt-1'>
						Klienci przypisani do Twojego konta
					</p>
				</div>
				<Button onClick={() => setAddOpen(true)}>
					<UserPlus className='w-4 h-4 mr-2' />
					Dodaj klienta
				</Button>
			</div>

			{/* Search */}
			<div className='mb-6'>
				<SearchInput
					value={search}
					onChange={setSearch}
					placeholder='Szukaj klienta...'
					className='w-full sm:w-80'
				/>
			</div>

			{/* Clients grid */}
			{filtered.length === 0 ? (
				<EmptyState
					icon={<UserPlus className='w-12 h-12' />}
					title='Brak klientów'
					description='Dodaj pierwszego klienta aby rozpocząć'
					action={
						<Button onClick={() => setAddOpen(true)} size='sm'>
							<UserPlus className='w-4 h-4 mr-2' />
							Dodaj klienta
						</Button>
					}
				/>
			) : (
				<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6'>
					{filtered.map((client) => (
						<ClientCard
							key={client.id.value}
							client={client}
							albums={albumsByClient.get(client.id.value) ?? []}
						/>
					))}
					{/* Add card */}
					<button
						onClick={() => setAddOpen(true)}
						className='border-2 border-dashed border-border hover:border-accent/50 transition-colors p-6 flex flex-col items-center justify-center gap-2 min-h-[200px]'
					>
						<UserPlus className='w-8 h-8 text-muted' />
						<span className='text-sm text-muted'>Dodaj klienta</span>
					</button>
				</div>
			)}

			{/* Add Client Modal */}
			<Modal
				open={addOpen}
				onClose={() => setAddOpen(false)}
				title='Nowy klient'
				maxWidth='max-w-md'
			>
				<div className='p-8 space-y-6'>
					<Input
						id='client-name'
						label='Imię i Nazwisko'
						placeholder='Imię i Nazwisko'
						value={newName}
						onChange={(e) => setNewName(e.target.value)}
					/>
					<Input
						id='client-email'
						label='Email'
						type='email'
						placeholder='email@example.pl'
						value={newEmail}
						onChange={(e) => setNewEmail(e.target.value)}
					/>
					<Input
						id='client-password'
						label='Hasło'
						type='password'
						placeholder='Min. 8 znaków'
						value={newPassword}
						onChange={(e) => setNewPassword(e.target.value)}
					/>
					<p className='text-xs text-muted'>
						Dane logowania zostaną wysłane na email klienta
					</p>
					<div className='bg-accent/5 border border-border p-3 flex items-start gap-2'>
						<span className='text-accent text-xs mt-0.5'>ℹ</span>
						<p className='text-xs text-muted'>
							Klient zostanie automatycznie przypisany do Twojego konta i będzie
							mógł się zalogować w Strefie Klienta
						</p>
					</div>
				</div>
				<div className='px-8 py-6 border-t border-border flex justify-end gap-3'>
					<Button variant='ghost' onClick={() => setAddOpen(false)}>
						Anuluj
					</Button>
					<Button
						onClick={handleCreate}
						disabled={
							createMutation.isPending || !newName || !newEmail || !newPassword
						}
					>
						{createMutation.isPending ? (
							<Loader2 className='w-4 h-4 mr,2 animate-spin' />
						) : null}
						Utwórz konto
					</Button>
				</div>
			</Modal>
		</div>
	);
}

function ClientCard({
	client,
	albums,
}: {
	client: UserInfo;
	albums: import('@/shared/types/api').AlbumDto[];
}) {
	const totalPhotos = albums.reduce((s, a) => s + a.files.length, 0);
	const initials = client.name
		.split(' ')
		.map((w) => w[0])
		.join('')
		.slice(0, 2)
		.toUpperCase();

	return (
		<div className='bg-surface border border-border p-6'>
			{/* Top */}
			<div className='flex items-center gap-4 mb-5'>
				<div className='w-12 h-12 bg-border flex items-center justify-center flex-shrink-0'>
					<span className='text-sm text-muted'>{initials}</span>
				</div>
				<div className='min-w-0'>
					<p className='font-medium truncate'>{client.name}</p>
					<p className='text-xs text-muted truncate'>{client.email.value}</p>
				</div>
				<div className='ml-auto'>
					<span
						className={`inline-block w-2 h-2 rounded-full ${
							client.isActive ? 'bg-green-500' : 'bg-red-500'
						}`}
					/>
				</div>
			</div>

			<div className='border-t border-border pt-5 mb-4'>
				<div className='flex gap-8'>
					<div>
						<p className='text-[10px] uppercase tracking-widest text-muted'>
							Albumy
						</p>
						<p className='text-2xl font-light'>{albums.length}</p>
					</div>
					<div>
						<p className='text-[10px] uppercase tracking-widest text-muted'>
							Zdjęcia
						</p>
						<p className='text-2xl font-light'>{totalPhotos}</p>
					</div>
				</div>
			</div>

			{/* Album list (max 3) */}
			{albums.length > 0 && (
				<div className='space-y-1 mb-4'>
					{albums.slice(0, 3).map((album) => (
						<Link
							key={album.albumId}
							to={`/photographer/albums/${album.albumId}`}
							className='flex items-center gap-2 text-xs text-muted hover:text-foreground transition-colors'
						>
							<FolderOpen className='w-3 h-3' />
							<span className='truncate'>{album.name}</span>
							<span>·</span>
							<span>{album.files.length} zdjęć</span>
						</Link>
					))}
					{albums.length > 3 && (
						<p className='text-xs text-muted'>+{albums.length - 3} więcej...</p>
					)}
				</div>
			)}

			{albums.length === 0 && (
				<p className='text-xs text-muted mb-4'>Brak albumów</p>
			)}

			{/* Actions */}
			<div className='border-t border-border pt-4 flex gap-4'>
				<Link
					to='/photographer/albums'
					className='text-xs uppercase tracking-widest text-muted hover:text-foreground transition-colors'
				>
					Albumy →
				</Link>
				<Link
					to='/photographer/albums'
					className='text-xs uppercase tracking-widest text-muted hover:text-accent transition-colors'
				>
					Nowy album
				</Link>
			</div>
		</div>
	);
}
