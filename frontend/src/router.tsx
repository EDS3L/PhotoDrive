import { createBrowserRouter } from 'react-router';

// Layouts
import { PublicLayout } from '@/layouts/PublicLayout';
import { AuthLayout } from '@/layouts/AuthLayout';
import { AdminLayout } from '@/layouts/AdminLayout';
import { PhotographerLayout } from '@/layouts/PhotographerLayout';
import { ClientLayout } from '@/layouts/ClientLayout';

// Guard
import { ProtectedRoute } from '@/components/ProtectedRoute';

// Public pages
import { HomePage } from '@/pages/public/HomePage';
import { AboutPage } from '@/pages/public/AboutPage';
import { PortfolioPage } from '@/pages/public/PortfolioPage';
import { ContactPage } from '@/pages/public/ContactPage';
import { ContactSuccessPage } from '@/pages/public/ContactSuccessPage';

// Auth pages
import { StaffLoginPage } from '@/pages/auth/StaffLoginPage';
import { ClientLoginPage } from '@/pages/auth/ClientLoginPage';
import { ResetPasswordPage } from '@/pages/auth/ResetPasswordPage';

// Admin pages
import { AdminDashboardPage } from '@/pages/admin/DashboardPage';
import { UserManagementPage } from '@/pages/admin/UserManagementPage';
import { AlbumsManagementPage as AdminAlbumsPage } from '@/pages/admin/AlbumsManagementPage';
import { AdminAlbumDetailPage } from '@/pages/admin/AlbumDetailPage';
import { PublicAlbumsPage } from '@/pages/admin/PublicAlbumsPage';

// Photographer pages
import { PhotographerDashboardPage } from '@/pages/photographer/DashboardPage';
import { ClientListPage } from '@/pages/photographer/ClientListPage';
import { AlbumsListPage as PhotographerAlbumsPage } from '@/pages/photographer/AlbumsListPage';
import { PhotographerAlbumDetailPage } from '@/pages/photographer/AlbumDetailPage';

// Client pages
import { ClientDashboardPage } from '@/pages/client/DashboardPage';
import { ClientAlbumDetailPage } from '@/pages/client/AlbumDetailPage';

// Not found
import { NotFoundPage } from '@/pages/NotFoundPage';

export const router = createBrowserRouter([
	// ── Public pages ──
	{
		element: <PublicLayout />,
		children: [
			{ path: '/', element: <HomePage /> },
			{ path: '/o-mnie', element: <AboutPage /> },
			{ path: '/portfolio', element: <PortfolioPage /> },
			{ path: '/portfolio/:category', element: <PortfolioPage /> },
			{ path: '/kontakt', element: <ContactPage /> },
			{ path: '/kontakt/sukces', element: <ContactSuccessPage /> },
		],
	},

	// ── Auth pages ──
	{
		element: <AuthLayout />,
		children: [
			{ path: '/login', element: <StaffLoginPage /> },
			{ path: '/strefa-klienta/login', element: <ClientLoginPage /> },
			{ path: '/reset-haslo', element: <ResetPasswordPage /> },
		],
	},

	// ── Admin ──
	{
		element: (
			<ProtectedRoute requiredRole='ADMIN'>
				<AdminLayout />
			</ProtectedRoute>
		),
		children: [
			{ path: '/admin/dashboard', element: <AdminDashboardPage /> },
			{ path: '/admin/uzytkownicy', element: <UserManagementPage /> },
			{ path: '/admin/albumy', element: <AdminAlbumsPage /> },
			{ path: '/admin/albumy/:id', element: <AdminAlbumDetailPage /> },
			{ path: '/admin/albumy-publiczne', element: <PublicAlbumsPage /> },
		],
	},

	// ── Photographer ──
	{
		element: (
			<ProtectedRoute requiredRole='PHOTOGRAPHER'>
				<PhotographerLayout />
			</ProtectedRoute>
		),
		children: [
			{ path: '/fotograf/dashboard', element: <PhotographerDashboardPage /> },
			{ path: '/fotograf/klienci', element: <ClientListPage /> },
			{ path: '/fotograf/albumy', element: <PhotographerAlbumsPage /> },
			{
				path: '/fotograf/albumy/:id',
				element: <PhotographerAlbumDetailPage />,
			},
		],
	},

	// ── Client ──
	{
		element: (
			<ProtectedRoute requiredRole='CLIENT'>
				<ClientLayout />
			</ProtectedRoute>
		),
		children: [
			{ path: '/klient/dashboard', element: <ClientDashboardPage /> },
			{ path: '/klient/album/:id', element: <ClientAlbumDetailPage /> },
		],
	},

	// ── 404 ──
	{ path: '*', element: <NotFoundPage /> },
]);
