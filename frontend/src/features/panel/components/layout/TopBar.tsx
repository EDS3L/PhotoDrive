import { Menu, LogOut, ChevronRight } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { usePanelLogout } from '../../hooks/usePanelAuth';
import { usePanelAuthStore } from '../../store/panelAuthStore';

interface TopBarProps {
	onMenuClick: () => void;
}

function buildBreadcrumbs(pathname: string): { label: string; to?: string }[] {
	const crumbs: { label: string; to?: string }[] = [{ label: 'Panel' }];

	const segments = pathname.split('/').filter(Boolean);

	const labelMap: Record<string, string> = {
		admin: 'Admin',
		photographer: 'Fotograf',
		users: 'Użytkownicy',
		albums: 'Albumy',
		'public-albums': 'Albumy publiczne',
		site: 'Strona wizytówka',
		settings: 'Ustawienia',
		clients: 'Klienci',
	};

	for (let i = 1; i < segments.length; i++) {
		const seg = segments[i];
		const label = labelMap[seg] ?? seg;
		const path = '/' + segments.slice(0, i + 1).join('/');
		crumbs.push({ label, to: path });
	}

	return crumbs;
}

export function TopBar({ onMenuClick }: TopBarProps) {
	const { mutate: logout } = usePanelLogout();
	const user = usePanelAuthStore((s) => s.user);
	const location = useLocation();
	const breadcrumbs = buildBreadcrumbs(location.pathname);

	return (
		<header className='sticky top-0 z-20 bg-surface/95 backdrop-blur-md border-b border-border'>
			<div className='flex items-center justify-between h-14 px-6'>
				<div className='flex items-center gap-4'>
					<button
						onClick={onMenuClick}
						className='lg:hidden text-muted hover:text-foreground'
					>
						<Menu className='w-5 h-5' />
					</button>

					{/* Breadcrumbs */}
					<nav className='flex items-center gap-1 text-sm'>
						{breadcrumbs.map((crumb, i) => (
							<span key={i} className='flex items-center gap-1'>
								{i > 0 && <ChevronRight className='w-3 h-3 text-muted' />}
								{crumb.to && i < breadcrumbs.length - 1 ? (
									<Link
										to={crumb.to}
										className='text-muted hover:text-foreground transition-colors'
									>
										{crumb.label}
									</Link>
								) : (
									<span
										className={
											i === breadcrumbs.length - 1
												? 'text-foreground'
												: 'text-muted'
										}
									>
										{crumb.label}
									</span>
								)}
							</span>
						))}
					</nav>
				</div>

				<div className='flex items-center gap-4'>
					{user && (
						<span className='hidden md:block text-xs text-muted'>
							{user.email}
						</span>
					)}
					<button
						onClick={() => logout()}
						className='flex items-center gap-2 text-xs uppercase tracking-widest text-muted hover:text-foreground transition-colors'
					>
						<LogOut className='w-4 h-4' />
						<span className='hidden sm:inline'>Wyloguj</span>
					</button>
				</div>
			</div>
		</header>
	);
}
