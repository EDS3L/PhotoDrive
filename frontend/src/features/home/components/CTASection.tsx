import { Link } from 'react-router-dom';
import { Button } from '@/shared/components/ui/Button';
import { useSiteSlots } from '@/shared/hooks/useSiteSlots';
import { placeholder } from '@/lib/placeholder';

export function CTASection() {
	const { data: slots } = useSiteSlots();
	const ctaUrl = slots?.HOME_CTA ?? placeholder(1920, 600, 'CTA background');

	return (
		<section className='relative py-24 overflow-hidden'>
			{/* Background */}
			<div className='absolute inset-0'>
				<img
					src={ctaUrl}
					alt=''
					className='w-full h-full object-cover'
					aria-hidden='true'
				/>
				<div className='absolute inset-0 bg-black/60' />
			</div>

			<div className='relative z-10 text-center px-4'>
				<p className='text-xs uppercase tracking-[0.3em] text-accent mb-4'>
					Rozpocznij przygodę
				</p>
				<h2 className='font-serif text-4xl md:text-5xl font-light mb-4'>
					Stwórzmy razem coś pięknego
				</h2>
				<p className='text-muted mb-8 max-w-md mx-auto'>
					Gotowy na piękne wspomnienia? Skontaktuj się ze mną.
				</p>
				<div className='flex flex-col sm:flex-row items-center justify-center gap-4'>
					<Link to='/kontakt'>
						<Button variant='primary' size='lg'>
							Napisz do mnie
						</Button>
					</Link>
					<Link to='/kontakt'>
						<Button variant='outline' size='lg'>
							Sprawdź terminy
						</Button>
					</Link>
				</div>
			</div>
		</section>
	);
}
