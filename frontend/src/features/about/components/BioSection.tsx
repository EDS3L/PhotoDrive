import { usePublicAlbumPhotos } from '@/shared/hooks/usePublicPhotos';
import { placeholder } from '@/lib/placeholder';

export function BioSection() {
	const { data: photos } = usePublicAlbumPhotos('about-bio');
	const bioUrl = photos?.[0]?.url ?? placeholder(600, 800, 'Fotograf portret');

	return (
		<section className='max-w-7xl mx-auto px-6 py-24'>
			<div className='grid grid-cols-1 md:grid-cols-2 gap-12 items-center'>
				{/* Photo */}
				<div className='aspect-[3/4] overflow-hidden'>
					<img
						src={bioUrl}
						alt='Fotograf'
						className='w-full h-full object-cover'
					/>
				</div>

				{/* Bio text */}
				<div>
					<p className='text-xs uppercase tracking-[0.3em] text-accent mb-4'>
						O mnie
					</p>
					<h2 className='font-serif text-4xl md:text-5xl font-light mb-6'>
						Marek Wiśniewski
					</h2>
					<p className='text-muted leading-relaxed mb-6'>
						Fotograf z pasją do opowiadania historii przez obiektyw. Od ponad 8
						lat uwieczniam najważniejsze chwile — od kameralnych ślubów po
						dynamiczne reportaże. Wierzę, że każde zdjęcie powinno oddawać
						emocje danej chwili.
					</p>
					<p className='text-muted leading-relaxed mb-8'>
						Moim podejściem jest dyskretna, reportażowa fotografia, uzupełniona
						o starannie zaplanowane portrety. Stawiam na naturalność,
						autentyczność i piękne światło.
					</p>

					{/* Stats */}
					<div className='flex gap-12'>
						<div>
							<p className='font-serif text-4xl text-foreground'>8+</p>
							<p className='text-xs uppercase tracking-widest text-muted mt-1'>
								Lat doświadczenia
							</p>
						</div>
						<div>
							<p className='font-serif text-4xl text-foreground'>500+</p>
							<p className='text-xs uppercase tracking-widest text-muted mt-1'>
								Sesji zdjęciowych
							</p>
						</div>
						<div>
							<p className='font-serif text-4xl text-foreground'>1500+</p>
							<p className='text-xs uppercase tracking-widest text-muted mt-1'>
								Zadowolonych klientów
							</p>
						</div>
					</div>
				</div>
			</div>
		</section>
	);
}
