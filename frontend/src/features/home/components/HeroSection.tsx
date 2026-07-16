import { Link } from 'react-router-dom';
import { ChevronDown } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { useSiteSlots } from '@/shared/hooks/useSiteSlots';
import { placeholder } from '@/lib/placeholder';

export function HeroSection() {
	const { data: slots } = useSiteSlots();
	const heroUrl = slots?.HOME_HERO ?? placeholder(1920, 1080, 'Hero — ślub');

	return (
		<section className='relative h-dvh flex items-center justify-center overflow-hidden'>
			{/* Background */}
			<div className='absolute inset-0'>
				<img
					src={heroUrl}
					alt='Zdjęcie ślubne'
					className='w-full h-full object-cover'
				/>
				<div className='absolute inset-0 bg-black/50' />
			</div>

			{/* Content */}
			<div className='relative z-10 text-center px-4'>
				<h1 className='font-serif text-5xl md:text-7xl lg:text-8xl font-light leading-tight'>
					Twoja historia
					<br />
					<em className='italic text-accent'>w kadrze</em>
				</h1>

				<div className='mt-10 flex flex-col sm:flex-row items-center justify-center gap-4'>
					<Link to='/portfolio'>
						<Button variant='primary' size='lg'>
							Zobacz portfolio
						</Button>
					</Link>
					<Link to='/kontakt'>
						<Button variant='outline' size='lg'>
							Rezerwacja
						</Button>
					</Link>
				</div>
			</div>

			{/* Scroll indicator */}
			<div className='absolute bottom-8 left-1/2 -translate-x-1/2 animate-bounce'>
				<ChevronDown className='w-6 h-6 text-foreground/40' />
			</div>
		</section>
	);
}
