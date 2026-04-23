import { Navigate } from 'react-router';
import { useAuthStore, type Role } from '@/lib/stores/auth-store';
import type { ReactNode } from 'react';

interface ProtectedRouteProps {
	children: ReactNode;
	requiredRole?: Role;
}

export function ProtectedRoute({
	children,
	requiredRole,
}: ProtectedRouteProps) {
	const { isAuthenticated, isLoading, hasRole, primaryRole } = useAuthStore();

	if (isLoading) {
		return (
			<div className='min-h-screen flex items-center justify-center bg-background'>
				<span className='material-symbols-outlined text-primary animate-spin text-4xl'>
					progress_activity
				</span>
			</div>
		);
	}

	if (!isAuthenticated) {
		return <Navigate to='/login' replace />;
	}

	if (requiredRole && !hasRole(requiredRole)) {
		// Redirect to appropriate dashboard
		const role = primaryRole();
		if (role === 'ADMIN') return <Navigate to='/admin/dashboard' replace />;
		if (role === 'PHOTOGRAPHER')
			return <Navigate to='/fotograf/dashboard' replace />;
		if (role === 'CLIENT') return <Navigate to='/klient/dashboard' replace />;
		return <Navigate to='/login' replace />;
	}

	return <>{children}</>;
}
