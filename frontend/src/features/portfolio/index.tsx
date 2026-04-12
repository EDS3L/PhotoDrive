import { useState } from 'react';
import { usePublicAlbumPhotos } from '@/shared/hooks/usePublicPhotos';
import { placeholder } from '@/lib/placeholder';
import { PageHeader } from '@/shared/components/layout/PageHeader';
import { PhotoGrid, PhotoGridItem } from '@/shared/components/PhotoGrid';
import { Button } from '@/shared/components/ui/Button';
import { PortfolioTabs } from './components/PortfolioTabs';
import type { PortfolioCategory } from './types/portfolio.types';
import { CTASection } from '@/features/home/components/CTASection';

const categoryAlbumMap: Record<PortfolioCategory, string> = {
	sluby: 'portfolio-sluby',
	plener: 'portfolio-plener',
	portret: 'portfolio-portret',
	reportaz: 'portfolio-reportaz',
};

function generatePlaceholders(category: string, count: number) {
	return Array.from({ length: count }, (_, i) => ({
		id: `${category}-${i}`,
		src: placeholder(600, 600, `${category} ${i + 1}`),
		alt: `${category} zdjęcie ${i + 1}`,
	}));
}

export default function PortfolioPage() {
	const [active, setActive] = useState<PortfolioCategory>('sluby');
	const [visibleCount, setVisibleCount] = useState(6);

	const albumName = categoryAlbumMap[active];
	const { data: apiPhotos } = usePublicAlbumPhotos(albumName);

	const photos =
		apiPhotos?.map((p) => ({ id: p.fileId, src: p.url, alt: p.fileName })) ??
		generatePlaceholders(active, 9);

	const visible = photos.slice(0, visibleCount);
	const hasMore = visibleCount < photos.length;

	const handleCategoryChange = (category: PortfolioCategory) => {
		setActive(category);
		setVisibleCount(6);
	};

	return (
		<>
			<PageHeader eyebrow='Portfolio' title='Galeria' />

			<div className='max-w-7xl mx-auto px-6 pb-24'>
				<PortfolioTabs active={active} onChange={handleCategoryChange} />

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
