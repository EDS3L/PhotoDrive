import { useState } from 'react';
import { useParams, Link } from 'react-router';
import { usePublicAlbums, usePublicAlbumPhotos } from '@/hooks/use-albums';

const CATEGORIES = [
	{ slug: 'sluby', label: 'ŚLUBY', icon: 'diamond' },
	{ slug: 'sesje-plenerowe', label: 'SESJE PLENEROWE', icon: 'landscape' },
	{ slug: 'portret', label: 'PORTRET', icon: 'person' },
	{ slug: 'reportaz', label: 'REPORTAŻ', icon: 'photo_camera' },
];

export function PortfolioPage() {
	const { category } = useParams();
	const activeCategory = category ?? CATEGORIES[0].slug;

	const { data: albums } = usePublicAlbums();

	const filteredAlbums = (albums ?? []).filter((a) =>
		a.name.toLowerCase().includes(activeCategory.replace('-', ' ')),
	);

	const [selectedAlbumId, setSelectedAlbumId] = useState<string | undefined>();
	const activeAlbumId = selectedAlbumId ?? filteredAlbums[0]?.albumId;

	const { data: photos } = usePublicAlbumPhotos(activeAlbumId, {
		width: 600,
		height: 600,
	});

	return (
		<div className='pt-16 min-h-screen'>
			<div className='flex'>
				{/* Side categories (desktop) */}
				<aside className='hidden lg:flex flex-col items-center gap-6 fixed left-0 top-16 bottom-0 w-64 py-12 bg-surface'>
					{CATEGORIES.map((cat) => (
						<Link
							key={cat.slug}
							to={`/portfolio/${cat.slug}`}
							className={`flex flex-col items-center gap-1 py-3 px-4 w-full transition-all duration-200
                ${
									activeCategory === cat.slug
										? 'text-primary bg-primary/5 border-l-2 border-primary'
										: 'text-on-surface-variant hover:text-on-surface border-l-2 border-transparent'
								}`}
						>
							<span className='material-symbols-outlined text-[24px]'>
								{cat.icon}
							</span>
							<span className='label text-[9px]'>{cat.label}</span>
						</Link>
					))}
				</aside>

				{/* Mobile tab bar */}
				<div className='lg:hidden fixed top-16 left-0 right-0 z-30 bg-surface/95 backdrop-blur-sm overflow-x-auto'>
					<div className='flex gap-0 min-w-max'>
						{CATEGORIES.map((cat) => (
							<Link
								key={cat.slug}
								to={`/portfolio/${cat.slug}`}
								className={`px-5 py-3 label text-[10px] whitespace-nowrap transition-colors
                  ${activeCategory === cat.slug ? 'text-primary border-b-2 border-primary' : 'text-on-surface-variant'}`}
							>
								{cat.label}
							</Link>
						))}
					</div>
				</div>

				{/* Main content */}
				<main className='flex-1 lg:pl-64 pt-4 lg:pt-0'>
					<div className='max-w-6xl mx-auto px-6 py-12 lg:py-16'>
						<p className='label text-[11px] text-primary mb-2'>PORTFOLIO</p>
						<h1 className='font-display text-5xl lg:text-6xl text-on-surface mb-12'>
							{CATEGORIES.find((c) => c.slug === activeCategory)?.label ??
								'Portfolio'}
						</h1>

						{filteredAlbums.length > 1 && (
							<div className='flex gap-2 mb-8 overflow-x-auto'>
								{filteredAlbums.map((album) => (
									<button
										key={album.albumId}
										onClick={() => setSelectedAlbumId(album.albumId)}
										className={`px-4 py-2 label text-[10px] whitespace-nowrap transition-colors ${
											activeAlbumId === album.albumId
												? 'bg-primary text-on-primary'
												: 'bg-surface-container-low text-on-surface-variant hover:text-on-surface'
										}`}
									>
										{album.name}
									</button>
								))}
							</div>
						)}

						{(photos ?? []).length > 0 ? (
							<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-1'>
								{(photos ?? []).map((url, i) => (
									<div
										key={i}
										className='group relative aspect-[4/5] overflow-hidden'
									>
										<img
											src={url}
											alt=''
											className='w-full h-full object-cover grayscale group-hover:grayscale-0
                        group-hover:scale-105 transition-all duration-500'
											loading='lazy'
										/>
										<div
											className='absolute inset-0 bg-background/40 opacity-0 group-hover:opacity-100
                      transition-opacity duration-300 flex items-end p-4'
										>
											<span className='material-symbols-outlined text-on-surface'>
												open_in_full
											</span>
										</div>
									</div>
								))}
							</div>
						) : (
							<div className='flex flex-col items-center justify-center py-24 text-center'>
								<span className='material-symbols-outlined text-5xl text-on-surface-variant/30 mb-4'>
									photo_library
								</span>
								<p className='text-on-surface-variant text-sm'>
									Portfolio jest w trakcie przygotowywania.
								</p>
								<p className='text-on-surface-variant/60 text-xs mt-1'>
									Zdjęcia zostaną załadowane z albumów publicznych.
								</p>
							</div>
						)}
					</div>
				</main>
			</div>
		</div>
	);
}
