import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';
import { useCreateClientAlbum } from '@/hooks/use-albums';
import { useAssignedUsers } from '@/hooks/use-users';
import { useUiStore } from '@/lib/stores/ui-store';

export function NewAlbumModal() {
	const closeModal = useUiStore((s) => s.closeModal);
	const createAlbum = useCreateClientAlbum();
	const { data: clients } = useAssignedUsers();

	const [name, setName] = useState('');
	const [clientId, setClientId] = useState('');

	const clientOptions = (clients ?? []).map((c) => ({
		value: c.id,
		label: c.name,
	}));

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		if (!clientId) return;
		await createAlbum.mutateAsync({ clientId, name });
		closeModal();
		setName('');
		setClientId('');
	};

	return (
		<Modal
			name='new-album'
			title='Nowy album'
			subtitle='Utwórz album dla klienta'
		>
			<form onSubmit={handleSubmit} className='space-y-5 mt-4'>
				<Input
					label='Nazwa albumu'
					value={name}
					onChange={(e) => setName(e.target.value)}
					required
				/>
				<Select
					label='Klient'
					value={clientId}
					onChange={(e) => setClientId(e.target.value)}
					options={[
						{ value: '', label: 'Wybierz klienta...' },
						...clientOptions,
					]}
				/>
				<div className='flex gap-3 justify-end pt-4'>
					<Button variant='ghost' type='button' onClick={closeModal}>
						Anuluj
					</Button>
					<Button
						type='submit'
						loading={createAlbum.isPending}
						disabled={!clientId}
					>
						Utwórz album
					</Button>
				</div>
			</form>
		</Modal>
	);
}
