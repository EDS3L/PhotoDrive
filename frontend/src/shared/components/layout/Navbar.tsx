import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Menu, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useScrollNavbar } from '@/shared/hooks/useScrollNavbar';
import { Button } from '@/shared/components/ui/Button';

const navLinks = [
	{ to: '/portfolio', label: 'Galeria' },
	{ to: '/o-mnie', label: 'O mnie' },
	{ to: '/kontakt', label: 'Kontakt' },
];

export function Navbar() {
	const isScrolled = useScrollNavbar();
	const location = useLocation();
	const [mobileOpen, setMobileOpen] = useState(false);

	return (
		<>
			<header
				className={cn(
					'fixed top-0 left-0 right-0 z-50 transition-all duration-300',
					isScrolled
						? 'bg-surface/95 backdrop-blur-md border-b border-border'
						: 'bg-transparent',
				)}
			>
				<nav className='max-w-7xl mx-auto px-6 flex items-center justify-between h-20'>
					<Link
						to='/'
						className='font-serif text-2xl tracking-wider text-foreground'
					>
						PHOTODRIVE
					</Link>

					{/* Desktop nav */}
					<div className='hidden md:flex items-center gap-8'>
						{navLinks.map((link) => (
							<Link
								key={link.to}
								to={link.to}
								className={cn(
									'text-xs uppercase tracking-[0.2em] transition-colors duration-300',
									location.pathname === link.to
										? 'text-accent'
										: 'text-foreground/70 hover:text-foreground',
								)}
							>
								{link.label}
							</Link>
						))}
						<Link to='/strefa-klienta'>
							<Button variant='outline' size='sm'>
								Strefa klienta
							</Button>
						</Link>
					</div>

					{/* Mobile hamburger */}
					<button
						className='md:hidden text-foreground'
						onClick={() => setMobileOpen(true)}
						aria-label='Otwórz menu'
					>
						<Menu className='w-6 h-6' />
					</button>
				</nav>
			</header>

			{/* Mobile overlay */}
			{mobileOpen && (
				<div className='fixed inset-0 z-[60] bg-background flex flex-col items-center justify-center'>
					<button
						className='absolute top-6 right-6 text-foreground'
						onClick={() => setMobileOpen(false)}
						aria-label='Zamknij menu'
					>
						<X className='w-6 h-6' />
					</button>
					<nav className='flex flex-col items-center gap-8'>
						{navLinks.map((link) => (
							<Link
								key={link.to}
								to={link.to}
								onClick={() => setMobileOpen(false)}
								className={cn(
									'font-serif text-3xl transition-colors',
									location.pathname === link.to
										? 'text-accent'
										: 'text-foreground/70 hover:text-foreground',
								)}
							>
								{link.label}
							</Link>
						))}
						<Link to='/strefa-klienta' onClick={() => setMobileOpen(false)}>
							<Button variant='outline' size='md'>
								Strefa klienta
							</Button>
						</Link>
					</nav>
				</div>
			)}
		</>
	);
}
