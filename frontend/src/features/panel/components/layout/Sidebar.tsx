import { NavLink, useLocation } from 'react-router-dom';
import {
	LayoutDashboard,
	Users,
	FolderOpen,
	Globe,
	Droplets,
	Image,
	Settings,
	UserCheck,
	X,
	User,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { usePanelAuthStore } from '../../store/panelAuthStore';
import type { PanelRole } from '../../types/panel';

interface SidebarProps {
	open: boolean;
	onClose: () => void;
}

interface NavItem {
	to: string;
	label: string;
	icon: React.ReactNode;
}

const adminNav: NavItem[] = [
	{
		to: '/admin',
		label: 'Dashboard',
		icon: <LayoutDashboard className='w-5 h-5' />,
	},
	{
		to: '/admin/users',
		label: 'Użytkownicy',
		icon: <Users className='w-5 h-5' />,
	},
	{
		to: '/admin/albums',
		label: 'Albumy',
		icon: <FolderOpen className='w-5 h-5' />,
	},
	{
		to: '/admin/public-albums',
		label: 'Albumy publiczne',
		icon: <Globe className='w-5 h-5' />,
	},
	{
		to: '/admin/site',
		label: 'Strona wizytówka',
		icon: <Image className='w-5 h-5' />,
	},
	{
		to: '/admin/watermark',
		label: 'Znak wodny',
		icon: <Droplets className='w-5 h-5' />,
	},
	{
		to: '/admin/settings',
		label: 'Ustawienia',
		icon: <Settings className='w-5 h-5' />,
	},
];

const photographerNav: NavItem[] = [
	{
		to: '/photographer',
		label: 'Dashboard',
		icon: <LayoutDashboard className='w-5 h-5' />,
	},
	{
		to: '/photographer/clients',
		label: 'Klienci',
		icon: <UserCheck className='w-5 h-5' />,
	},
	{
		to: '/photographer/albums',
		label: 'Albumy',
		icon: <FolderOpen className='w-5 h-5' />,
	},
	{
		to: '/photographer/account',
		label: 'Moje konto',
		icon: <User className='w-5 h-5' />,
	},
];

function getNavItems(role: PanelRole | null): NavItem[] {
	if (role === 'ADMIN') return adminNav;
	if (role === 'PHOTOGRAPHER') return photographerNav;
	return [];
}

export function Sidebar({ open, onClose }: SidebarProps) {
	const { role, user } = usePanelAuthStore();
	const location = useLocation();
	const navItems = getNavItems(role);

	const isActive = (to: string) => {
		if (to === '/admin' || to === '/photographer') {
			return location.pathname === to;
		}
		return location.pathname.startsWith(to);
	};

	const sidebarContent = (
		<>
			{/* Brand */}
			<div className='px-6 py-6 border-b border-border'>
				<NavLink
					to={role === 'ADMIN' ? '/admin' : '/photographer'}
					className='font-serif text-xl tracking-wider text-foreground'
				>
					PHOTODRIVE
				</NavLink>
				<span className='ml-2 text-[10px] uppercase tracking-widest px-1.5 py-0.5 border border-accent text-accent'>
					Panel
				</span>
			</div>

			{/* Navigation */}
			<nav className='flex-1 px-3 py-4 space-y-1'>
				{navItems.map((item) => (
					<NavLink
						key={item.to}
						to={item.to}
						end={item.to === '/admin' || item.to === '/photographer'}
						onClick={onClose}
						className={cn(
							'flex items-center gap-3 px-3 py-2.5 text-sm transition-colors',
							isActive(item.to)
								? 'text-foreground border-l-2 border-accent bg-accent/5'
								: 'text-muted hover:text-foreground hover:bg-surface-light',
						)}
					>
						{item.icon}
						{item.label}
					</NavLink>
				))}
			</nav>

			{/* User info */}
			{user && (
				<div className='px-6 py-4 border-t border-border'>
					<p className='text-sm text-foreground truncate'>{user.name}</p>
					<p className='text-xs text-muted truncate'>{user.email}</p>
					<p className='text-[10px] uppercase tracking-widest text-accent mt-1'>
						{role}
					</p>
				</div>
			)}
		</>
	);

	return (
		<>
			{/* Desktop sidebar */}
			<aside className='hidden lg:flex lg:flex-col lg:w-[260px] lg:fixed lg:inset-y-0 lg:left-0 bg-surface border-r border-border z-30'>
				{sidebarContent}
			</aside>

			{/* Mobile overlay */}
			{open && (
				<div className='fixed inset-0 z-40 lg:hidden'>
					<div className='absolute inset-0 bg-black/60' onClick={onClose} />
					<aside className='relative w-[260px] h-full bg-surface border-r border-border flex flex-col'>
						<button
							onClick={onClose}
							className='absolute top-4 right-4 text-muted hover:text-foreground'
						>
							<X className='w-5 h-5' />
						</button>
						{sidebarContent}
					</aside>
				</div>
			)}
		</>
	);
}
