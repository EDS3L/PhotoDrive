import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { useCreateAdminAlbum } from '@/hooks/use-albums';
import { useUiStore } from '@/lib/stores/ui-store';

export function CreatePublicAlbumModal() {
	const closeModal = useUiStore((s) => s.closeModal);
	const createAlbum = useCreateAdminAlbum();

	const [name, setName] = useState('');

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		await createAlbum.mutateAsync({ name, isPublic: true });
		closeModal();
		setName('');
	};

	return (
		<Modal
			name='create-public-album'
			title='Album publiczny'
			subtitle='Utwórz nowy album publiczny do portfolio'
		>
			<form onSubmit={handleSubmit} className='space-y-5 mt-4'>
				<Input
					label='Nazwa albumu'
					value={name}
					onChange={(e) => setName(e.target.value)}
					required
				/>
				<div className='flex gap-3 justify-end pt-4'>
					<Button variant='ghost' type='button' onClick={closeModal}>
						Anuluj
					</Button>
					<Button type='submit' loading={createAlbum.isPending}>
						Utwórz
					</Button>
				</div>
			</form>
		</Modal>
	);
}
