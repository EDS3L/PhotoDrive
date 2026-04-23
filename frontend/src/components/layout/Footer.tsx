import { Link } from 'react-router';

export function Footer() {
	return (
		<footer className='bg-surface-dim'>
			<div className='max-w-7xl mx-auto px-6 lg:px-12 py-16'>
				<div className='grid grid-cols-1 md:grid-cols-3 gap-12'>
					{/* Brand */}
					<div>
						<p className='font-display italic text-2xl text-on-surface mb-3'>
							PhotoDrive
						</p>
						<p className='text-on-surface-variant text-sm leading-relaxed max-w-xs'>
							Profesjonalna fotografia ślubna i portretowa. Każda historia
							zasługuje na piękne kadry.
						</p>
					</div>

					{/* Navigation */}
					<div>
						<p className='label text-[11px] text-on-surface mb-4'>NAWIGACJA</p>
						<div className='flex flex-col gap-2'>
							<Link
								to='/portfolio'
								className='text-on-surface-variant text-sm hover:text-on-surface transition-colors'
							>
								Portfolio
							</Link>
							<Link
								to='/o-mnie'
								className='text-on-surface-variant text-sm hover:text-on-surface transition-colors'
							>
								O mnie
							</Link>
							<Link
								to='/kontakt'
								className='text-on-surface-variant text-sm hover:text-on-surface transition-colors'
							>
								Kontakt
							</Link>
						</div>
					</div>

					{/* Social */}
					<div>
						<p className='label text-[11px] text-on-surface mb-4'>SOCIAL</p>
						<div className='flex gap-4'>
							<span className='material-symbols-outlined text-on-surface-variant hover:text-primary transition-colors cursor-pointer'>
								photo_camera
							</span>
							<span className='material-symbols-outlined text-on-surface-variant hover:text-primary transition-colors cursor-pointer'>
								alternate_email
							</span>
							<span className='material-symbols-outlined text-on-surface-variant hover:text-primary transition-colors cursor-pointer'>
								play_circle
							</span>
						</div>
					</div>
				</div>

				<div className='h-px bg-outline-variant/10 my-8' />

				<div className='flex flex-col md:flex-row items-center justify-between gap-4'>
					<p className='text-on-surface-variant text-xs'>
						© {new Date().getFullYear()} PhotoDrive. Wszelkie prawa zastrzeżone.
					</p>
					<div className='flex gap-6'>
						<span className='text-on-surface-variant text-xs hover:text-on-surface transition-colors cursor-pointer'>
							Polityka prywatności
						</span>
						<span className='text-on-surface-variant text-xs hover:text-on-surface transition-colors cursor-pointer'>
							Regulamin
						</span>
						<Link
							to='/login'
							className='text-on-surface-variant/40 text-xs hover:text-on-surface transition-colors'
						>
							Panel
						</Link>
					</div>
				</div>
			</div>
		</footer>
	);
}
