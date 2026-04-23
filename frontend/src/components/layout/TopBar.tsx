import { useUiStore } from '@/lib/stores/ui-store';

interface TopBarProps {
	breadcrumbs: { label: string; to?: string }[];
	actions?: React.ReactNode;
}

export function TopBar({ breadcrumbs, actions }: TopBarProps) {
	const toggleSidebar = useUiStore((s) => s.toggleSidebar);

	return (
		<header className='sticky top-0 z-30 bg-surface/90 backdrop-blur-sm'>
			<div className='flex items-center justify-between px-6 h-14'>
				<div className='flex items-center gap-4'>
					{/* Mobile hamburger */}
					<button
						onClick={toggleSidebar}
						className='lg:hidden text-on-surface-variant'
						aria-label='Menu'
					>
						<span className='material-symbols-outlined text-[22px]'>menu</span>
					</button>

					{/* Breadcrumbs */}
					<div className='flex items-center gap-2'>
						{breadcrumbs.map((crumb, i) => (
							<span key={crumb.label} className='flex items-center gap-2'>
								{i > 0 && (
									<span className='text-on-surface-variant/30 text-xs'>/</span>
								)}
								{crumb.to ? (
									<a
										href={crumb.to}
										className='label text-[10px] text-on-surface-variant hover:text-on-surface transition-colors'
									>
										{crumb.label}
									</a>
								) : (
									<span className='label text-[10px] text-on-surface'>
										{crumb.label}
									</span>
								)}
							</span>
						))}
					</div>
				</div>

				{/* Right actions */}
				{actions && <div className='flex items-center gap-2'>{actions}</div>}
			</div>
		</header>
	);
}
