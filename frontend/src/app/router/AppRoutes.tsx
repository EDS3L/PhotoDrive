import { lazy, Suspense } from 'react';
import { Routes, Route } from 'react-router-dom';
import { PanelLayout } from '@/features/panel/components/layout/PanelLayout';
import { NotFoundPage } from '@/shared/components/NotFoundPage';

const HomePage = lazy(() => import('@/features/home/index'));
const PortfolioPage = lazy(() => import('@/features/portfolio/index'));
const AboutPage = lazy(() => import('@/features/about/index'));
const ContactPage = lazy(() => import('@/features/contact/index'));
const ClientZonePage = lazy(() => import('@/features/client-zone/index'));

const PanelLoginPage = lazy(
	() => import('@/features/panel/pages/PanelLoginPage'),
);
const AdminDashboard = lazy(
	() => import('@/features/panel/pages/admin/AdminDashboard'),
);
const AdminUsers = lazy(
	() => import('@/features/panel/pages/admin/AdminUsers'),
);
const AdminAlbums = lazy(
	() => import('@/features/panel/pages/admin/AdminAlbums'),
);
const AdminAlbumDetail = lazy(
	() => import('@/features/panel/pages/admin/AdminAlbumDetail'),
);
const AdminPublicAlbums = lazy(
	() => import('@/features/panel/pages/admin/AdminPublicAlbums'),
);
const AdminWatermark = lazy(
	() => import('@/features/panel/pages/admin/AdminWatermark'),
);
const PhotographerDashboard = lazy(
	() => import('@/features/panel/pages/photographer/PhotographerDashboard'),
);
const PhotographerClients = lazy(
	() => import('@/features/panel/pages/photographer/PhotographerClients'),
);
const PhotographerAlbums = lazy(
	() => import('@/features/panel/pages/photographer/PhotographerAlbums'),
);
const PhotographerAlbumDetail = lazy(
	() => import('@/features/panel/pages/photographer/PhotographerAlbumDetail'),
);
const AccountPage = lazy(
	() => import('@/features/panel/pages/shared/AccountPage'),
);

const Loading = () => (
	<div className='min-h-screen flex items-center justify-center'>
		<div className='w-8 h-8 border-2 border-accent border-t-transparent rounded-full animate-spin' />
	</div>
);

export function AppRoutes() {
	return (
		<Suspense fallback={<Loading />}>
			<Routes>
				{/* Public pages */}
				<Route path='/' element={<HomePage />} />
				<Route path='/portfolio' element={<PortfolioPage />} />
				<Route path='/o-mnie' element={<AboutPage />} />
				<Route path='/kontakt' element={<ContactPage />} />
				<Route path='/strefa-klienta' element={<ClientZonePage />} />

				{/* Panel login */}
				<Route path='/panel-login' element={<PanelLoginPage />} />

				{/* Admin panel */}
				<Route element={<PanelLayout requiredRole='ADMIN' />}>
					<Route path='/admin' element={<AdminDashboard />} />
					<Route path='/admin/users' element={<AdminUsers />} />
					<Route path='/admin/albums' element={<AdminAlbums />} />
					<Route path='/admin/albums/:albumId' element={<AdminAlbumDetail />} />
					<Route path='/admin/public-albums' element={<AdminPublicAlbums />} />
					<Route path='/admin/watermark' element={<AdminWatermark />} />
					<Route path='/admin/settings' element={<AccountPage />} />
				</Route>

				{/* Photographer panel */}
				<Route element={<PanelLayout requiredRole='PHOTOGRAPHER' />}>
					<Route path='/photographer' element={<PhotographerDashboard />} />
					<Route
						path='/photographer/clients'
						element={<PhotographerClients />}
					/>
					<Route path='/photographer/albums' element={<PhotographerAlbums />} />
					<Route
						path='/photographer/albums/:albumId'
						element={<PhotographerAlbumDetail />}
					/>
					<Route path='/photographer/account' element={<AccountPage />} />
				</Route>

				{/*Error route*/}
				<Route path='*' element={<NotFoundPage />} />
			</Routes>
		</Suspense>
	);
}
