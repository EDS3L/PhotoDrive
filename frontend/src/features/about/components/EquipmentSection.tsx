import { useSiteSlots } from '@/shared/hooks/useSiteSlots';
import { placeholder } from '@/lib/placeholder';

export function EquipmentSection() {
	const { data: slots } = useSiteSlots();
	const equipUrl = slots?.ABOUT_EQUIPMENT ?? placeholder(1920, 800, 'Sprzęt — obiektyw');

	return (
		<section className='relative py-24 overflow-hidden'>
			{/* Background */}
			<div className='absolute inset-0'>
				<img
					src={equipUrl}
					alt=''
					className='w-full h-full object-cover'
					aria-hidden='true'
				/>
				<div className='absolute inset-0 bg-black/70' />
			</div>

			<div className='relative z-10 max-w-7xl mx-auto px-6'>
				<p className='text-xs uppercase tracking-[0.3em] text-accent mb-4'>
					Narzędzia
				</p>
				<h2 className='font-serif text-4xl md:text-5xl font-light mb-12'>
					Sprzęt
				</h2>

				<div className='grid grid-cols-1 sm:grid-cols-2 gap-6 max-w-2xl'>
					{[
						'Sony A7 IV',
						'Sony 24-70mm f/2.8 GM II',
						'Sony 85mm f/1.4 GM',
						'Sony 35mm f/1.4 GM',
						'Godox AD200 Pro',
						'DJI Mavic 3 Pro',
					].map((item) => (
						<div key={item} className='flex items-center gap-3'>
							<div className='w-1.5 h-1.5 bg-accent rounded-full shrink-0' />
							<span className='text-foreground/80'>{item}</span>
						</div>
					))}
				</div>
			</div>
		</section>
	);
}
