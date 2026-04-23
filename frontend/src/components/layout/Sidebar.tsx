import { Link, useLocation } from 'react-router';
import { useAuthStore } from '@/lib/stores/auth-store';
import { useUiStore } from '@/lib/stores/ui-store';
import type { ReactNode } from 'react';

interface NavItem {
	to: string;
	icon: string;
	label: string;
}

interface SidebarProps {
	brand: string;
	items: NavItem[];
	footer?: ReactNode;
	actionButton?: { label: string; onClick: () => void };
}

export function Sidebar({ brand, items, footer, actionButton }: SidebarProps) {
	const location = useLocation();
	const user = useAuthStore((s) => s.user);
	const { sidebarOpen, setSidebarOpen } = useUiStore();

	return (
		<>
			{/* Mobile overlay */}
			{sidebarOpen && (
				<div
					className='fixed inset-0 z-40 bg-background/60 backdrop-blur-sm lg:hidden'
					onClick={() => setSidebarOpen(false)}
					onKeyDown={(e) => e.key === 'Escape' && setSidebarOpen(false)}
					role='button'
					tabIndex={0}
					aria-label='Zamknij menu'
				/>
			)}

			{/* Sidebar */}
			<aside
				className={`fixed top-0 left-0 bottom-0 z-50 w-[264px] bg-surface flex flex-col
          transition-transform duration-300 lg:translate-x-0
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}`}
			>
				{/* Brand */}
				<div className='px-6 pt-8 pb-6'>
					<h1 className='font-display text-xl text-on-surface tracking-wider uppercase'>
						{brand}
					</h1>
				</div>

				{/* Nav items */}
				<nav className='flex-1 px-3'>
					{items.map((item) => {
						const active =
							location.pathname === item.to ||
							location.pathname.startsWith(item.to + '/');
						return (
							<Link
								key={item.to}
								to={item.to}
								onClick={() => setSidebarOpen(false)}
								className={`flex items-center gap-3 px-3 py-2.5 mb-0.5 text-sm transition-all duration-200
                  ${
										active
											? 'text-primary bg-primary/5 border-l-2 border-primary pl-[10px]'
											: 'text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low border-l-2 border-transparent pl-[10px]'
									}`}
							>
								<span className='material-symbols-outlined text-[20px]'>
									{item.icon}
								</span>
								<span className='label text-[11px]'>{item.label}</span>
							</Link>
						);
					})}

					{actionButton && (
						<button
							onClick={actionButton.onClick}
							className='w-full mt-4 px-4 py-2.5 bg-primary text-on-primary label text-[11px]
                hover:bg-primary-container transition-colors'
						>
							{actionButton.label}
						</button>
					)}
				</nav>

				{/* Footer */}
				<div className='px-4 py-4 border-t border-outline-variant/10'>
					{footer ??
						(user && (
							<div className='flex items-center gap-3'>
								<div className='w-8 h-8 bg-surface-container-high flex items-center justify-center text-xs font-medium text-on-surface'>
									{user.name
										.split(' ')
										.map((n) => n[0])
										.join('')
										.toUpperCase()
										.slice(0, 2)}
								</div>
								<div className='min-w-0'>
									<p className='text-sm text-on-surface truncate'>
										{user.name}
									</p>
									<p className='text-[10px] text-on-surface-variant truncate'>
										{user.email}
									</p>
								</div>
							</div>
						))}
				</div>
			</aside>
		</>
	);
}
