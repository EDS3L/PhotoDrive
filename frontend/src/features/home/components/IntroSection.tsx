import { usePublicAlbumPhotos } from '@/shared/hooks/usePublicPhotos';
import { placeholder } from '@/lib/placeholder';

export function IntroSection() {
	const { data: photos } = usePublicAlbumPhotos('home-intro');
	const introUrl = photos?.[0]?.url ?? placeholder(600, 800, 'Fotograf');

	return (
		<section className='max-w-7xl mx-auto px-6 py-24'>
			<div className='grid grid-cols-1 md:grid-cols-2 gap-12 items-center'>
				{/* Quote */}
				<div>
					<blockquote className='font-serif text-2xl md:text-3xl italic leading-relaxed text-foreground/90'>
						&ldquo;Uchwycam emocje, których słowa nie potrafią opisać. Moim
						celem jest stworzenie ponadczasowych wspomnień.&rdquo;
					</blockquote>
					<p className='mt-6 text-xs uppercase tracking-[0.2em] text-muted'>
						Marek V. — założyciel
					</p>
				</div>

				{/* Photo */}
				<div className='aspect-[3/4] overflow-hidden'>
					<img
						src={introUrl}
						alt='Fotograf w pracy'
						className='w-full h-full object-cover'
					/>
				</div>
			</div>

			{/* Pasja i precyzja */}
			<div className='mt-24 text-center max-w-2xl mx-auto'>
				<p className='text-xs uppercase tracking-[0.3em] text-accent mb-4'>
					Pasja i precyzja
				</p>
				<p className='text-muted leading-relaxed'>
					Wierzę w naturalność. Nie reżyseruję momentów — je dla was zatrzymuję.
					Każda sesja to dla mnie nowa opowieść, do której podchodzę z
					empatyczną obserwacją i dbałością o detale i światło.
				</p>
			</div>
		</section>
	);
}
