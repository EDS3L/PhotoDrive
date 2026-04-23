import { Outlet, Link, useLocation, useNavigate } from 'react-router';
import { useLogout } from '@/hooks/use-auth';
import { useAuthStore } from '@/lib/stores/auth-store';

const CLIENT_NAV = [{ to: '/klient/dashboard', label: 'MOJE ALBUMY' }];

export function ClientLayout() {
	const location = useLocation();
	const logout = useLogout();
	const navigate = useNavigate();
	const user = useAuthStore((s) => s.user);

	const handleLogout = async () => {
		await logout.mutateAsync();
		navigate('/strefa-klienta/login');
	};

	return (
		<div className='min-h-screen flex flex-col bg-background'>
			{/* Top nav */}
			<nav className='fixed top-0 left-0 right-0 z-50 glass'>
				<div className='max-w-7xl mx-auto px-6 lg:px-12 flex items-center justify-between h-16'>
					<Link
						to='/klient/dashboard'
						className='font-display italic text-2xl text-on-surface'
					>
						The Gallery
					</Link>

					<div className='hidden md:flex items-center gap-8'>
						{CLIENT_NAV.map((item) => (
							<Link
								key={item.to}
								to={item.to}
								className={`label text-[11px] transition-colors ${
									location.pathname.startsWith(item.to)
										? 'text-primary'
										: 'text-on-surface-variant hover:text-on-surface'
								}`}
							>
								{item.label}
							</Link>
						))}
					</div>

					<div className='flex items-center gap-4'>
						{user && (
							<span className='text-on-surface-variant text-xs hidden sm:inline'>
								{user.email}
							</span>
						)}
						<button
							onClick={handleLogout}
							className='label text-[11px] text-on-surface-variant hover:text-on-surface transition-colors'
						>
							WYLOGUJ
						</button>
					</div>
				</div>
			</nav>

			<main className='flex-1 pt-16'>
				<Outlet />
			</main>
		</div>
	);
}
