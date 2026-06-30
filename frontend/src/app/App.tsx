import { useLocation } from 'react-router-dom';
import { Navbar } from '@/shared/components/layout/Navbar';
import { Footer } from '@/shared/components/layout/Footer';
import { ScrollToTop } from '@/shared/components/ScrollToTop';
import { ToastViewport } from '@/shared/components/ui/Toast';
import { AppRoutes } from '@/app/router/AppRoutes';

const PANEL_PREFIXES = ['/admin', '/photographer', '/panel-login'];

export function App() {
	const { pathname } = useLocation();
	const isPanel = PANEL_PREFIXES.some((p) => pathname.startsWith(p));

	return (
		<>
			<ScrollToTop />
			{!isPanel && <Navbar />}
			<main className='min-h-screen'>
				<AppRoutes />
			</main>
			{!isPanel && <Footer />}
			<ToastViewport />
		</>
	);
}
