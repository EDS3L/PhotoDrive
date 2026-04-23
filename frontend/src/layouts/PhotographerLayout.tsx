import { Outlet, useNavigate } from 'react-router';
import { Sidebar } from '@/components/layout/Sidebar';
import { useLogout } from '@/hooks/use-auth';

const PHOTOGRAPHER_NAV = [
	{ to: '/fotograf/dashboard', icon: 'dashboard', label: 'Dashboard' },
	{ to: '/fotograf/klienci', icon: 'group', label: 'Klienci' },
	{ to: '/fotograf/albumy', icon: 'gallery_thumbnail', label: 'Albumy' },
];

export function PhotographerLayout() {
	const logout = useLogout();
	const navigate = useNavigate();

	const handleLogout = async () => {
		await logout.mutateAsync();
		navigate('/login');
	};

	return (
		<div className='min-h-screen'>
			<Sidebar
				brand='PhotoDrive'
				items={PHOTOGRAPHER_NAV}
				footer={
					<button
						onClick={handleLogout}
						className='flex items-center gap-2 text-on-surface-variant text-sm hover:text-on-surface transition-colors w-full'
					>
						<span className='material-symbols-outlined text-[18px]'>
							logout
						</span>
						Wyloguj
					</button>
				}
			/>
			<div className='lg:ml-[264px] min-h-screen bg-background'>
				<Outlet />
			</div>
		</div>
	);
}
