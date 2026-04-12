import { useState } from 'react';
import { LogOut } from 'lucide-react';
import { useAuthStore } from '@/app/store/authStore';
import { logout } from './api/clientZoneApi';
import { LoginForm } from './components/LoginForm';
import { AlbumList } from './components/AlbumList';
import { AlbumDetailModal } from './components/AlbumDetailModal';
import { Button } from '@/shared/components/ui/Button';
import type { AlbumDto } from '@/shared/types/api';
import { queryClient } from '@/lib/queryClient';

export default function ClientZonePage() {
	const { isAuthenticated, email, clear } = useAuthStore();
	const [selectedAlbum, setSelectedAlbum] = useState<AlbumDto | null>(null);

	const handleLogout = async () => {
		try {
			await logout();
		} finally {
			clear();
			queryClient.removeQueries({ queryKey: ['albums'] });
		}
	};

	if (!isAuthenticated) {
		return <LoginForm />;
	}

	return (
		<div className='pt-28 pb-24'>
			<div className='max-w-7xl mx-auto px-6'>
				{/* Header */}
				<div className='flex items-center justify-between mb-12'>
					<div>
						<h1 className='font-serif text-4xl md:text-5xl font-light'>
							Twoje albumy
						</h1>
						{email && (
							<p className='text-xs uppercase tracking-widest text-muted mt-2'>
								Zalogowany jako {email}
							</p>
						)}
					</div>
					<Button variant='ghost' size='sm' onClick={handleLogout}>
						<LogOut className='w-4 h-4 mr-2' />
						Wyloguj
					</Button>
				</div>

				<AlbumList onOpenAlbum={setSelectedAlbum} />
			</div>

			{/* Album detail modal */}
			{selectedAlbum && (
				<AlbumDetailModal
					album={selectedAlbum}
					onClose={() => setSelectedAlbum(null)}
				/>
			)}
		</div>
	);
}
