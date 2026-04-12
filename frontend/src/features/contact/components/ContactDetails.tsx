import { Mail, Phone, MapPin, Clock, Globe, ExternalLink } from 'lucide-react';

export function ContactDetails() {
	return (
		<div className='space-y-8'>
			{/* Contact info */}
			<div className='space-y-6'>
				<div className='flex items-start gap-4'>
					<Mail className='w-5 h-5 text-accent mt-0.5 shrink-0' />
					<div>
						<p className='text-xs uppercase tracking-widest text-muted mb-1'>
							Email
						</p>
						<a
							href='mailto:kontakt@photodrive.dev'
							className='text-foreground hover:text-accent transition-colors'
						>
							kontakt@photodrive.dev
						</a>
					</div>
				</div>
				<div className='flex items-start gap-4'>
					<Phone className='w-5 h-5 text-accent mt-0.5 shrink-0' />
					<div>
						<p className='text-xs uppercase tracking-widest text-muted mb-1'>
							Telefon
						</p>
						<a
							href='tel:+48123456789'
							className='text-foreground hover:text-accent transition-colors'
						>
							+48 123 456 789
						</a>
					</div>
				</div>
				<div className='flex items-start gap-4'>
					<MapPin className='w-5 h-5 text-accent mt-0.5 shrink-0' />
					<div>
						<p className='text-xs uppercase tracking-widest text-muted mb-1'>
							Studio
						</p>
						<p className='text-foreground'>Kraków, Polska</p>
					</div>
				</div>
				<div className='flex items-start gap-4'>
					<Clock className='w-5 h-5 text-accent mt-0.5 shrink-0' />
					<div>
						<p className='text-xs uppercase tracking-widest text-muted mb-1'>
							Godziny
						</p>
						<p className='text-foreground'>Pon – Pt: 9:00 – 18:00</p>
					</div>
				</div>
			</div>

			{/* Social */}
			<div className='flex gap-4'>
				<a
					href='https://instagram.com'
					target='_blank'
					rel='noopener noreferrer'
					className='text-muted hover:text-accent transition-colors'
					aria-label='Instagram'
				>
					<Globe className='w-5 h-5' />
				</a>
				<a
					href='https://facebook.com'
					target='_blank'
					rel='noopener noreferrer'
					className='text-muted hover:text-accent transition-colors'
					aria-label='Facebook'
				>
					<ExternalLink className='w-5 h-5' />
				</a>
			</div>

			{/* Availability card */}
			<div className='border border-accent/30 p-6'>
				<p className='text-xs uppercase tracking-widest text-accent mb-2'>
					Dostępność
				</p>
				<p className='text-sm text-muted'>
					Aktualnie przyjmuję rezerwacje na sezon 2026/2027. Skontaktuj się, aby
					sprawdzić wolne terminy.
				</p>
			</div>
		</div>
	);
}
