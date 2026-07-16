import { useMemo, useState } from 'react';
import { usePublicAlbums } from '@/shared/hooks/usePublicAlbums';
import { usePublicAlbumPhotos } from '@/shared/hooks/usePublicPhotos';
import { PUBLIC_PHOTO_SIZE } from '@/lib/publicApi';
import { placeholder } from '@/lib/placeholder';
import { PageHeader } from '@/shared/components/layout/PageHeader';
import { PhotoGrid, PhotoGridItem } from '@/shared/components/PhotoGrid';
import { Button } from '@/shared/components/ui/Button';
import { PortfolioTabs } from './components/PortfolioTabs';
import { CTASection } from '@/features/home/components/CTASection';

function generatePlaceholders(count: number) {
	return Array.from({ length: count }, (_, i) => ({
		id: `placeholder-${i}`,
		src: placeholder(600, 600, `Portfolio ${i + 1}`),
		alt: `Portfolio zdjęcie ${i + 1}`,
	}));
}

export default function PortfolioPage() {
	const { data: albums } = usePublicAlbums();
	const [activeId, setActiveId] = useState<string | null>(null);
	const [visibleCount, setVisibleCount] = useState(6);

	// Zakładki = publiczne albumy Z ZAWARTOŚCIĄ (kolejność i etykiety ustawia panel).
	// Pusty album nie dostaje zakładki — gość nie może trafić na pustą galerię.
	const tabs = useMemo(
		() =>
			(albums ?? [])
				.filter((album) => album.photoCount > 0)
				.map((album) => ({
					albumId: album.albumId,
					label: album.displayName ?? album.name,
					name: album.name,
				})),
		[albums],
	);

	// Zakładka zniknęła (album ukryty/opróżniony w panelu)? Spadamy na pierwszą.
	const active = tabs.find((tab) => tab.albumId === activeId) ?? tabs[0];

	// Kafelki: siatka pokazuje kilkanaście zdjęć naraz, więc liczy się waga, nie ostatni piksel.
	const { data: apiPhotos } = usePublicAlbumPhotos(
		active?.name ?? '',
		PUBLIC_PHOTO_SIZE.tile,
	);

	const photos =
		apiPhotos?.map((p) => ({ id: p.fileId, src: p.url, alt: p.fileName })) ??
		generatePlaceholders(9);

	const visible = photos.slice(0, visibleCount);
	const hasMore = visibleCount < photos.length;

	const handleTabChange = (albumId: string) => {
		setActiveId(albumId);
		setVisibleCount(6);
	};

	return (
		<>
			<PageHeader eyebrow='Portfolio' title='Galeria' />

			<div className='max-w-7xl mx-auto px-6 pb-24'>
				{tabs.length > 0 && (
					<PortfolioTabs
						tabs={tabs}
						activeId={active?.albumId ?? ''}
						onChange={handleTabChange}
					/>
				)}

				<div className='mt-12'>
					<PhotoGrid columns={3}>
						{visible.map((photo) => (
							<PhotoGridItem key={photo.id} src={photo.src} alt={photo.alt} />
						))}
					</PhotoGrid>
				</div>

				{hasMore && (
					<div className='mt-12 text-center'>
						<Button
							variant='outline'
							onClick={() => setVisibleCount((c) => c + 6)}
						>
							Wczytaj więcej
						</Button>
					</div>
				)}
			</div>

			<CTASection />
		</>
	);
}
