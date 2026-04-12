import { useState } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { usePanelAuthStore } from '../../store/panelAuthStore';
import { usePanelMe } from '../../hooks/usePanelAuth';
import { LoadingSpinner } from '../shared/LoadingSpinner';
import type { PanelRole } from '../../types/panel';

interface PanelLayoutProps {
	requiredRole?: PanelRole;
}

export function PanelLayout({ requiredRole }: PanelLayoutProps) {
	const [sidebarOpen, setSidebarOpen] = useState(false);
	const { isAuthenticated, role } = usePanelAuthStore();
	const { isLoading, isError } = usePanelMe();

	if (isLoading) {
		return (
			<div className='min-h-screen bg-background flex items-center justify-center'>
				<LoadingSpinner />
			</div>
		);
	}

	if (isError || !isAuthenticated) {
		return <Navigate to='/panel-login' replace />;
	}

	if (requiredRole && role !== requiredRole) {
		const redirect = role === 'ADMIN' ? '/admin' : '/photographer';
		return <Navigate to={redirect} replace />;
	}

	return (
		<div className='min-h-screen bg-background'>
			<Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
			<div className='lg:ml-[260px] flex flex-col min-h-screen'>
				<TopBar onMenuClick={() => setSidebarOpen(true)} />
				<main className='flex-1 p-6 lg:p-8'>
					<Outlet />
				</main>
			</div>
		</div>
	);
}
