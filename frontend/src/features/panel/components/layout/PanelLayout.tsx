import { useState } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { usePanelAuthStore, resolveRole } from '../../store/panelAuthStore';
import { usePanelMe } from '../../hooks/usePanelAuth';
import { changePassword } from '../../api/panelAuthApi';
import { LoadingSpinner } from '../shared/LoadingSpinner';
import { ForcePasswordChange } from '@/shared/components/ForcePasswordChange';
import type { PanelRole } from '../../types/panel';

interface PanelLayoutProps {
	requiredRole?: PanelRole;
}

export function PanelLayout({ requiredRole }: PanelLayoutProps) {
	const [sidebarOpen, setSidebarOpen] = useState(false);
	const { loginPassword, setLoginPassword } = usePanelAuthStore();
	const { data: me, isLoading, isError, refetch } = usePanelMe();

	if (isLoading) {
		return (
			<div className='min-h-screen bg-background flex items-center justify-center'>
				<LoadingSpinner />
			</div>
		);
	}

	// Uwierzytelnienie i rolę bierzemy WPROST z `me` (query.data), nie z Zustanda.
	// Store jest zasilany dopiero w useEffect (usePanelMe) PO rozwiązaniu query, więc
	// poleganie na nim dawało render z `isLoading=false`, ale pustym store → błędny
	// redirect na /panel-login, a stamtąd PanelLoginPage odsyłał na dashboard
	// (gubienie bieżącej podstrony po F5 — B.23).
	if (isError || !me) {
		return <Navigate to='/panel-login' replace />;
	}

	// Wymuszona zmiana hasła startowego — zanim wpuścimy do panelu (dowolnej trasy).
	if (me.changePasswordOnNextLogin) {
		return (
			<ForcePasswordChange
				userId={me.id}
				changePassword={changePassword}
				presetCurrentPassword={loginPassword ?? undefined}
				onDone={async () => {
					await refetch();
					setLoginPassword(null);
				}}
			/>
		);
	}

	const role = resolveRole(me.roles);
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
