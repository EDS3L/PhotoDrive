import { useState } from 'react';
import { LogOut, Loader2 } from 'lucide-react';
import { useAuthStore } from '@/app/store/authStore';
import { logout, changePassword } from './api/clientZoneApi';
import { useClientSession } from './hooks/useClientSession';
import { LoginForm } from './components/LoginForm';
import { AlbumList } from './components/AlbumList';
import { AlbumDetailModal } from './components/AlbumDetailModal';
import { Button } from '@/shared/components/ui/Button';
import { ForcePasswordChange } from '@/shared/components/ForcePasswordChange';
import type { AlbumDto } from '@/shared/types/api';
import { queryClient } from '@/lib/queryClient';

export default function ClientZonePage() {
	const {
		isAuthenticated,
		email,
		userId,
		mustChangePassword,
		loginPassword,
		clear,
		completePasswordChange,
	} = useAuthStore();
	const { isChecking } = useClientSession();
	const [selectedAlbum, setSelectedAlbum] = useState<AlbumDto | null>(null);

	const handleLogout = async () => {
		try {
			await logout();
		} finally {
			clear();
			queryClient.removeQueries({ queryKey: ['albums'] });
			queryClient.removeQueries({ queryKey: ['client', 'me'] });
		}
	};

	if (!isAuthenticated) {
		if (isChecking) {
			return (
				<div className='min-h-screen flex items-center justify-center'>
					<Loader2 className='w-8 h-8 text-accent animate-spin' />
				</div>
			);
		}
		return <LoginForm />;
	}

	// Wymuszona zmiana hasła startowego — dopóki flaga ustawiona, nic innego nie pokazujemy.
	if (mustChangePassword && userId) {
		return (
			<ForcePasswordChange
				userId={userId}
				changePassword={changePassword}
				presetCurrentPassword={loginPassword ?? undefined}
				onDone={completePasswordChange}
			/>
		);
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
