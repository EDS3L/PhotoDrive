import { Link } from 'react-router';
import { Button } from '@/components/ui/Button';
import { PhotoPlaceholder } from '@/components/ui/PhotoPlaceholder';

export function HomePage() {
	return (
		<>
			{/* Hero Section */}
			<section className='relative h-[100vh] flex items-center justify-center overflow-hidden'>
				<div className='absolute inset-0 bg-surface-dim' />
				<div className='absolute inset-0 bg-gradient-to-t from-background via-background/40 to-transparent' />
				<div className='relative z-10 text-center px-6 max-w-4xl mx-auto'>
					<p className='label text-[11px] text-primary mb-6'>
						FOTOGRAFIA PROFESJONALNA
					</p>
					<h1 className='font-display text-6xl md:text-8xl lg:text-[110px] text-on-surface leading-[0.9] mb-8'>
						Twoja historia
						<br />
						<span className='italic'>w kadrze</span>
					</h1>
					<div className='flex flex-col sm:flex-row items-center justify-center gap-4 mt-10'>
						<Link to='/portfolio'>
							<Button size='lg'>PORTFOLIO</Button>
						</Link>
						<Link to='/kontakt'>
							<Button variant='outline' size='lg'>
								UMÓW SESJĘ
							</Button>
						</Link>
					</div>
					<div className='mt-20 animate-bounce'>
						<span className='label text-[10px] text-on-surface-variant block mb-2'>
							PRZEWIŃ
						</span>
						<span className='material-symbols-outlined text-on-surface-variant'>
							expand_more
						</span>
					</div>
				</div>
			</section>

			{/* About Section */}
			<section className='py-24 lg:py-40'>
				<div className='max-w-7xl mx-auto px-6 lg:px-12 grid grid-cols-1 lg:grid-cols-2 gap-16 items-center'>
					<div>
						<p className='label text-[11px] text-primary mb-4'>
							KILKA SLOW O MNIE
						</p>
						<h2 className='font-display italic text-4xl lg:text-5xl text-on-surface mb-6 leading-[1.1]'>
							Fotografia to dla mnie sposób na opowiadanie historii
						</h2>
						<p className='text-on-surface-variant text-sm leading-relaxed mb-6'>
							Od ponad 8 lat uwieczniam najważniejsze momenty w życiu moich
							klientów. Każda sesja to dla mnie wyjątkowa historia, którą staram
							się opowiedzieć w sposób autentyczny i pełen emocji. Specjalizuję
							się w fotografii ślubnej, portretowej i reportażowej.
						</p>
						<Link
							to='/o-mnie'
							className='inline-flex items-center gap-2 text-primary text-sm group'
						>
							<span>Czytaj więcej</span>
							<span className='material-symbols-outlined text-[16px] group-hover:translate-x-1 transition-transform'>
								arrow_forward
							</span>
						</Link>
					</div>
					<div className='aspect-[3/4] overflow-hidden'>
						<PhotoPlaceholder
							label='Portret fotografki'
							className='w-full h-full'
						/>
					</div>
				</div>
			</section>

			{/* CTA Banner */}
			<section className='py-24 bg-surface-container-low'>
				<div className='max-w-3xl mx-auto px-6 text-center'>
					<h2 className='font-display text-4xl lg:text-5xl text-on-surface mb-4'>
						Zaplanujmy Twoją sesję
					</h2>
					<p className='text-on-surface-variant text-sm mb-8 max-w-lg mx-auto'>
						Niezależnie od tego, czy chodzi o ślub, sesję portretową czy
						reportaż — chętnie poznam Twoją wizję i stworzę coś wyjątkowego.
					</p>
					<div className='flex flex-col sm:flex-row items-center justify-center gap-4'>
						<Link to='/kontakt'>
							<Button size='lg'>SKONTAKTUJ SIĘ</Button>
						</Link>
						<Link to='/portfolio'>
							<Button variant='outline' size='lg'>
								ZOBACZ PORTFOLIO
							</Button>
						</Link>
					</div>
				</div>
			</section>
		</>
	);
}
