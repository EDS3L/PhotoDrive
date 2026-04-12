import { useState, useMemo } from 'react';
import { Globe, FolderPlus, Info, Loader2 } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { Input } from '@/shared/components/ui/Input';
import { Modal } from '../../components/shared/Modal';
import { LoadingSpinner } from '../../components/shared/LoadingSpinner';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { EmptyState } from '../../components/shared/EmptyState';
import {
	useAdminAlbums,
	useCreateAdminAlbum,
	useSetAlbumPublic,
} from '../../hooks/useAdminAlbums';
import type { AlbumDto } from '@/shared/types/api';

interface SectionGroup {
	title: string;
	prefix: string;
	albums: AlbumDto[];
}

const SECTION_TAGS = [
	'home-hero',
	'home-intro',
	'home-cta',
	'portfolio-sluby',
	'portfolio-plener',
	'portfolio-portret',
	'portfolio-reportaz',
	'about-bio',
	'about-equipment',
];

function groupBySection(albums: AlbumDto[]): SectionGroup[] {
	const publicAlbums = albums.filter(
		(a) => a.isPublic || a.photographId === a.clientId,
	);
	const adminAlbums = publicAlbums.filter((a) => a.photographId === a.clientId);

	const sections: SectionGroup[] = [
		{
			title: 'Strona główna',
			prefix: 'home-',
			albums: adminAlbums.filter((a) => a.name.startsWith('home-')),
		},
		{
			title: 'Portfolio',
			prefix: 'portfolio-',
			albums: adminAlbums.filter((a) => a.name.startsWith('portfolio-')),
		},
		{
			title: 'O mnie',
			prefix: 'about-',
			albums: adminAlbums.filter((a) => a.name.startsWith('about-')),
		},
		{
			title: 'Inne',
			prefix: '',
			albums: adminAlbums.filter(
				(a) =>
					!a.name.startsWith('home-') &&
					!a.name.startsWith('portfolio-') &&
					!a.name.startsWith('about-'),
			),
		},
	];

	return sections.filter((s) => s.albums.length > 0 || s.prefix !== '');
}

export default function AdminPublicAlbums() {
	const { data: albums, isLoading } = useAdminAlbums();
	const createMutation = useCreateAdminAlbum();
	const publicMutation = useSetAlbumPublic();

	const [createOpen, setCreateOpen] = useState(false);
	const [newName, setNewName] = useState('');

	const sections = useMemo(
		() => (albums ? groupBySection(albums) : []),
		[albums],
	);

	const publicCount = useMemo(
		() => albums?.filter((a) => a.isPublic).length ?? 0,
		[albums],
	);

	const handleCreate = () => {
		createMutation.mutate(newName, {
			onSuccess: (album) => {
				publicMutation.mutate(
					{ albumId: album.albumId, isPublic: true },
					{
						onSettled: () => {
							setCreateOpen(false);
							setNewName('');
						},
					},
				);
			},
		});
	};

	const togglePublic = (album: AlbumDto) => {
		publicMutation.mutate({
			albumId: album.albumId,
			isPublic: !album.isPublic,
		});
	};

	if (isLoading) return <LoadingSpinner />;

	return (
		<div>
			{/* Header */}
			<div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6'>
				<div>
					<h2 className='font-serif text-4xl font-light'>Albumy publiczne</h2>
					<p className='text-sm text-muted mt-1'>
						Zarządzaj albumami widocznymi na stronie
					</p>
				</div>
				<Button onClick={() => setCreateOpen(true)}>
					<FolderPlus className='w-4 h-4 mr-2' />
					Nowy album
				</Button>
			</div>

			{/* Info banner */}
			<div className='bg-accent/5 border border-border p-4 mb-8 flex items-start gap-3'>
				<Info className='w-5 h-5 text-accent flex-shrink-0 mt-0.5' />
				<div>
					<p className='text-sm'>
						Albumy publiczne są wyświetlane na stronie głównej i w portfolio.
						Nazwy albumów powinny odpowiadać sekcjom strony.
					</p>
					<p className='text-xs text-muted mt-1'>
						Konwencja nazewnictwa:{' '}
						<code className='text-accent'>home-hero</code>,{' '}
						<code className='text-accent'>portfolio-sluby</code>,{' '}
						<code className='text-accent'>about-bio</code>
					</p>
				</div>
			</div>

			{/* Stats */}
			<div className='mb-8'>
				<span className='text-xs text-muted'>
					{publicCount} albumów publicznych
				</span>
			</div>

			{/* Sections */}
			{sections.every((s) => s.albums.length === 0) ? (
				<EmptyState
					icon={<Globe className='w-12 h-12' />}
					title='Brak albumów publicznych'
					description='Utwórz album i włącz widoczność publiczną'
				/>
			) : (
				<div className='space-y-10'>
					{sections.map((section) => (
						<div key={section.title}>
							<h3 className='font-serif text-xl mb-4 flex items-center gap-2'>
								{section.title}
								<StatusBadge variant='default'>
									{section.albums.length}
								</StatusBadge>
							</h3>
							{section.albums.length === 0 ? (
								<p className='text-sm text-muted py-4'>
									Brak albumów w tej sekcji
								</p>
							) : (
								<div className='space-y-2'>
									{section.albums.map((album) => (
										<div
											key={album.albumId}
											className='bg-surface border border-border p-4 flex items-center justify-between'
										>
											<div className='flex items-center gap-4'>
												<div className='w-12 h-12 bg-surface-light flex items-center justify-center'>
													<Globe
														className={`w-5 h-5 ${
															album.isPublic ? 'text-accent' : 'text-muted/30'
														}`}
													/>
												</div>
												<div>
													<p className='text-sm font-medium'>{album.name}</p>
													<p className='text-xs text-muted'>
														{album.files.length} zdjęć
													</p>
												</div>
											</div>
											<div className='flex items-center gap-4'>
												<StatusBadge
													variant={album.isPublic ? 'success' : 'default'}
												>
													{album.isPublic ? 'Publiczny' : 'Ukryty'}
												</StatusBadge>
												<button
													onClick={() => togglePublic(album)}
													className={`relative inline-flex h-6 w-11 items-center transition-colors ${
														album.isPublic ? 'bg-accent' : 'bg-border'
													}`}
												>
													<span
														className={`inline-block h-4 w-4 bg-foreground transition-transform ${
															album.isPublic ? 'translate-x-6' : 'translate-x-1'
														}`}
													/>
												</button>
											</div>
										</div>
									))}
								</div>
							)}
						</div>
					))}
				</div>
			)}

			{/* Create modal */}
			<Modal
				open={createOpen}
				onClose={() => setCreateOpen(false)}
				title='Nowy album publiczny'
			>
				<div className='p-8 space-y-6'>
					<Input
						id='public-album-name'
						label='Nazwa albumu'
						placeholder='np. portfolio-sluby'
						value={newName}
						onChange={(e) => setNewName(e.target.value)}
					/>

					{/* Quick tags */}
					<div>
						<p className='text-xs uppercase tracking-widest text-muted mb-2'>
							Szybki wybór
						</p>
						<div className='flex flex-wrap gap-2'>
							{SECTION_TAGS.map((tag) => (
								<button
									key={tag}
									onClick={() => setNewName(tag)}
									className={`px-3 py-1.5 text-xs border transition-colors ${
										newName === tag
											? 'border-accent text-accent bg-accent/5'
											: 'border-border text-muted hover:border-accent/50'
									}`}
								>
									{tag}
								</button>
							))}
						</div>
					</div>

					<p className='text-xs text-muted'>
						Album zostanie utworzony jako administracyjny i od razu ustawiony
						jako publiczny.
					</p>
				</div>
				<div className='px-8 py-6 border-t border-border flex justify-end gap-3'>
					<Button variant='ghost' onClick={() => setCreateOpen(false)}>
						Anuluj
					</Button>
					<Button
						onClick={handleCreate}
						disabled={createMutation.isPending || !newName.trim()}
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
