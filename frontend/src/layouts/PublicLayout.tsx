import { Outlet } from 'react-router';
import { GlassNavbar } from '@/components/layout/GlassNavbar';
import { Footer } from '@/components/layout/Footer';

export function PublicLayout() {
	return (
		<div className='min-h-screen flex flex-col'>
			<GlassNavbar />
			<main className='flex-1'>
				<Outlet />
			</main>
			<Footer />
		</div>
	);
}
