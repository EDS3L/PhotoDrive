import { Link } from 'react-router-dom';
import { Globe, ExternalLink } from 'lucide-react';

export function Footer() {
	return (
		<footer className='border-t border-border bg-surface'>
			<div className='max-w-7xl mx-auto px-6 py-16'>
				<div className='grid grid-cols-1 md:grid-cols-3 gap-12'>
					{/* Brand */}
					<div>
						<Link
							to='/'
							className='font-serif text-2xl tracking-wider text-foreground'
						>
							PHOTODRIVE
						</Link>
						<p className='mt-4 text-sm text-muted leading-relaxed'>
							Fotografia ślubna i portretowa.
							<br />
							Twoja historia w kadrze.
						</p>
					</div>

					{/* Navigation */}
					<div>
						<h4 className='text-xs uppercase tracking-[0.2em] text-muted mb-4'>
							Nawigacja
						</h4>
						<ul className='space-y-3'>
							{[
								{ to: '/portfolio', label: 'Galeria' },
								{ to: '/o-mnie', label: 'O mnie' },
								{ to: '/kontakt', label: 'Kontakt' },
								{ to: '/strefa-klienta', label: 'Strefa klienta' },
								{ to: '/panel-login', label: 'Panel zarządzania' },
							].map((link) => (
								<li key={link.to}>
									<Link
										to={link.to}
										className='text-sm text-foreground/60 hover:text-foreground transition-colors'
									>
										{link.label}
									</Link>
								</li>
							))}
						</ul>
					</div>

					{/* Social */}
					<div>
						<h4 className='text-xs uppercase tracking-[0.2em] text-muted mb-4'>
							Social
						</h4>
						<div className='flex gap-4'>
							<a
								href='https://instagram.com'
								target='_blank'
								rel='noopener noreferrer'
								className='text-foreground/60 hover:text-accent transition-colors'
								aria-label='Instagram'
							>
								<Globe className='w-5 h-5' />
							</a>
							<a
								href='https://facebook.com'
								target='_blank'
								rel='noopener noreferrer'
								className='text-foreground/60 hover:text-accent transition-colors'
								aria-label='Facebook'
							>
								<ExternalLink className='w-5 h-5' />
							</a>
						</div>
					</div>
				</div>

				<div className='mt-12 pt-8 border-t border-border text-center text-xs text-muted'>
					&copy; {new Date().getFullYear()} PhotoDrive. All rights reserved.
				</div>
			</div>
		</footer>
	);
}
