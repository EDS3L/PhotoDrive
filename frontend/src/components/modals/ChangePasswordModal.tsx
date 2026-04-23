import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { useChangePassword } from '@/hooks/use-users';
import { useUiStore } from '@/lib/stores/ui-store';
import { useAuthStore } from '@/lib/stores/auth-store';

export function ChangePasswordModal() {
	const closeModal = useUiStore((s) => s.closeModal);
	const user = useAuthStore((s) => s.user);
	const changePassword = useChangePassword();

	const [currentPassword, setCurrentPassword] = useState('');
	const [newPassword, setNewPassword] = useState('');
	const [confirmPassword, setConfirmPassword] = useState('');

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		if (!user || newPassword !== confirmPassword) return;
		await changePassword.mutateAsync({
			userId: user.id,
			currentPassword: currentPassword || undefined,
			newPassword,
		});
		closeModal();
		setCurrentPassword('');
		setNewPassword('');
		setConfirmPassword('');
	};

	return (
		<Modal
			name='change-password'
			title='Zmiana hasła'
			subtitle='Ustaw nowe hasło do konta'
		>
			<form onSubmit={handleSubmit} className='space-y-5 mt-4'>
				<Input
					label='Obecne hasło'
					type='password'
					value={currentPassword}
					onChange={(e) => setCurrentPassword(e.target.value)}
				/>
				<Input
					label='Nowe hasło'
					type='password'
					value={newPassword}
					onChange={(e) => setNewPassword(e.target.value)}
					required
				/>
				<Input
					label='Potwierdź hasło'
					type='password'
					value={confirmPassword}
					onChange={(e) => setConfirmPassword(e.target.value)}
					required
					error={
						confirmPassword && newPassword !== confirmPassword
							? 'Hasła się nie zgadzają'
							: undefined
					}
				/>
				<div className='flex gap-3 justify-end pt-4'>
					<Button variant='ghost' type='button' onClick={closeModal}>
						Anuluj
					</Button>
					<Button
						type='submit'
						loading={changePassword.isPending}
						disabled={!newPassword || newPassword !== confirmPassword}
					>
						Zmień hasło
					</Button>
				</div>
			</form>
		</Modal>
	);
}
