import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { useCreateUser } from '@/hooks/use-users';
import { useUiStore } from '@/lib/stores/ui-store';

export function AddClientModal() {
	const closeModal = useUiStore((s) => s.closeModal);
	const createUser = useCreateUser();

	const [name, setName] = useState('');
	const [email, setEmail] = useState('');

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		await createUser.mutateAsync({
			name,
			email,
			role: 'CLIENT',
		});
		closeModal();
		setName('');
		setEmail('');
	};

	return (
		<Modal
			name='add-client'
			title='Nowy klient'
			subtitle='Dodaj nowego klienta'
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
				<div className='flex gap-3 justify-end pt-4'>
					<Button variant='ghost' type='button' onClick={closeModal}>
						Anuluj
					</Button>
					<Button type='submit' loading={createUser.isPending}>
						Dodaj klienta
					</Button>
				</div>
			</form>
		</Modal>
	);
}
