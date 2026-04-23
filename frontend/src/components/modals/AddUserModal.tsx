import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';
import { useCreateUser } from '@/hooks/use-users';
import { useUiStore } from '@/lib/stores/ui-store';
import type { Role } from '@/types/user';

export function AddUserModal() {
	const closeModal = useUiStore((s) => s.closeModal);
	const createUser = useCreateUser();

	const [name, setName] = useState('');
	const [email, setEmail] = useState('');
	const [password, setPassword] = useState('');
	const [role, setRole] = useState<Role>('PHOTOGRAPHER');

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		await createUser.mutateAsync({
			name,
			email,
			password: password || undefined,
			role,
		});
		closeModal();
		setName('');
		setEmail('');
		setPassword('');
		setRole('PHOTOGRAPHER');
	};

	return (
		<Modal
			name='add-user'
			title='Nowy użytkownik'
			subtitle='Dodaj nowego użytkownika do systemu'
		>
			<form onSubmit={handleSubmit} className='space-y-5 mt-4'>
				<Input
					label='Imię i nazwisko'
					value={name}
					onChange={(e) => setName(e.target.value)}
					required
				/>
				<Input
					label='Email'
					type='email'
					value={email}
					onChange={(e) => setEmail(e.target.value)}
					required
				/>
				<Input
					label='Hasło (opcjonalnie)'
					type='password'
					value={password}
					onChange={(e) => setPassword(e.target.value)}
					helperText='Jeśli puste, użytkownik otrzyma link do ustawienia hasła'
				/>
				<Select
					label='Rola'
					value={role}
					onChange={(e) => setRole(e.target.value as Role)}
					options={[
						{ value: 'ADMIN', label: 'Administrator' },
						{ value: 'PHOTOGRAPHER', label: 'Fotograf' },
						{ value: 'CLIENT', label: 'Klient' },
					]}
				/>
				<div className='flex gap-3 justify-end pt-4'>
					<Button variant='ghost' type='button' onClick={closeModal}>
						Anuluj
					</Button>
					<Button type='submit' loading={createUser.isPending}>
						Dodaj użytkownika
					</Button>
				</div>
			</form>
		</Modal>
	);
}
