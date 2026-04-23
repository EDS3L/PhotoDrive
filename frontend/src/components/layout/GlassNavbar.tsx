import { Link, useLocation } from 'react-router';
import { useState, useEffect } from 'react';

const NAV_LINKS = [
	{ to: '/portfolio', label: 'GALERIE' },
	{ to: '/o-mnie', label: 'O MNIE' },
	{ to: '/kontakt', label: 'KONTAKT' },
];

export function GlassNavbar() {
	const [scrolled, setScrolled] = useState(false);
	const [mobileOpen, setMobileOpen] = useState(false);
	const location = useLocation();

	useEffect(() => {
		const onScroll = () => setScrolled(window.scrollY > 40);
		window.addEventListener('scroll', onScroll, { passive: true });
		return () => window.removeEventListener('scroll', onScroll);
	}, []);

	useEffect(() => setMobileOpen(false), [location.pathname]);

	return (
		<>
			<nav
				className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
					scrolled ? 'glass' : 'bg-transparent'
				}`}
			>
				<div className='max-w-7xl mx-auto px-6 lg:px-12 flex items-center justify-between h-16'>
					{/* Brand */}
					<Link
						to='/'
						className='font-display italic text-2xl text-on-surface tracking-wide'
					>
						PhotoDrive
					</Link>

					{/* Desktop nav */}
					<div className='hidden md:flex items-center gap-10'>
						{NAV_LINKS.map((link) => (
							<Link
								key={link.to}
								to={link.to}
								className={`label text-[11px] transition-colors duration-300 ${
									location.pathname.startsWith(link.to)
										? 'text-primary'
										: 'text-on-surface-variant hover:text-on-surface'
								}`}
							>
								{link.label}
							</Link>
						))}
						<Link
							to='/strefa-klienta/login'
							className='label text-[11px] px-4 py-2 border border-primary/40 text-primary
                hover:bg-primary hover:text-on-primary transition-all duration-300'
						>
							STREFA KLIENTA
						</Link>
					</div>

					{/* Mobile hamburger */}
					<button
						onClick={() => setMobileOpen(!mobileOpen)}
						className='md:hidden text-on-surface'
						aria-label='Menu'
					>
						<span className='material-symbols-outlined'>
							{mobileOpen ? 'close' : 'menu'}
						</span>
					</button>
				</div>
			</nav>

			{/* Mobile nav overlay */}
			{mobileOpen && (
				<div className='fixed inset-0 z-40 bg-background/95 backdrop-blur-sm flex flex-col items-center justify-center gap-8 md:hidden'>
					{NAV_LINKS.map((link) => (
						<Link
							key={link.to}
							to={link.to}
							className='font-display text-4xl text-on-surface hover:text-primary transition-colors'
						>
							{link.label.charAt(0) + link.label.slice(1).toLowerCase()}
						</Link>
					))}
					<Link
						to='/strefa-klienta/login'
						className='mt-4 px-6 py-3 bg-primary text-on-primary label text-[11px]'
					>
						STREFA KLIENTA
					</Link>
				</div>
			)}
		</>
	);
}
