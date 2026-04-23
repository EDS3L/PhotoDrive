import { QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router';
import { queryClient } from '@/lib/query-client';
import { router } from '@/router';
import { useInitAuth } from '@/hooks/use-auth';
import { ToastContainer } from '@/components/ui/Toast';
import { AddUserModal } from '@/components/modals/AddUserModal';
import { UserDetailsModal } from '@/components/modals/UserDetailsModal';
import { AddClientModal } from '@/components/modals/AddClientModal';
import { NewAlbumModal } from '@/components/modals/NewAlbumModal';
import { CreatePublicAlbumModal } from '@/components/modals/CreatePublicAlbumModal';
import { SetTtdModal } from '@/components/modals/SetTtdModal';
import { ChangePasswordModal } from '@/components/modals/ChangePasswordModal';
import { RenameFileModal } from '@/components/modals/RenameFileModal';

function AppInner() {
	useInitAuth();

	return (
		<>
			<RouterProvider router={router} />
			<AddUserModal />
			<UserDetailsModal />
			<AddClientModal />
			<NewAlbumModal />
			<CreatePublicAlbumModal />
			<SetTtdModal />
			<ChangePasswordModal />
			<RenameFileModal />
			<ToastContainer />
		</>
	);
}

export function App() {
	return (
		<QueryClientProvider client={queryClient}>
			<AppInner />
		</QueryClientProvider>
	);
}
