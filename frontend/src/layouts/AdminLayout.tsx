import { Outlet } from 'react-router';
import { Sidebar } from '@/components/layout/Sidebar';
import { useLogout } from '@/hooks/use-auth';
import { useNavigate } from 'react-router';

const ADMIN_NAV = [
	{ to: '/admin/dashboard', icon: 'dashboard', label: 'Dashboard' },
	{ to: '/admin/uzytkownicy', icon: 'group', label: 'Użytkownicy' },
	{ to: '/admin/albumy', icon: 'gallery_thumbnail', label: 'Albumy' },
	{ to: '/admin/albumy-publiczne', icon: 'public', label: 'Albumy publiczne' },
];

export function AdminLayout() {
	const logout = useLogout();
	const navigate = useNavigate();

	const handleLogout = async () => {
		await logout.mutateAsync();
		navigate('/login');
	};

	return (
		<div className='min-h-screen'>
			<Sidebar
				brand='PhotoDrive Admin'
				items={ADMIN_NAV}
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
