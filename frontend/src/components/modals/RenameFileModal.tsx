import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { useRenameFile } from '@/hooks/use-albums';
import { useUiStore } from '@/lib/stores/ui-store';

export function RenameFileModal() {
	const closeModal = useUiStore((s) => s.closeModal);
	const modalData = useUiStore((s) => s.modalData) as {
		albumId: string;
		fileId: string;
		currentName?: string;
	} | null;
	const renameFile = useRenameFile();

	const [newFileName, setNewFileName] = useState('');

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		if (!modalData?.albumId || !modalData?.fileId || !newFileName) return;
		await renameFile.mutateAsync({
			albumId: modalData.albumId,
			fileId: modalData.fileId,
			newFileName,
		});
		closeModal();
		setNewFileName('');
	};

	return (
		<Modal
			name='rename-file'
			title='Zmień nazwę'
			subtitle={modalData?.currentName ?? 'Zmień nazwę pliku'}
		>
			<form onSubmit={handleSubmit} className='space-y-5 mt-4'>
				<Input
					label='Nowa nazwa pliku'
					value={newFileName}
					onChange={(e) => setNewFileName(e.target.value)}
					required
				/>
				<div className='flex gap-3 justify-end pt-4'>
					<Button variant='ghost' type='button' onClick={closeModal}>
						Anuluj
					</Button>
					<Button type='submit' loading={renameFile.isPending}>
						Zmień nazwę
					</Button>
				</div>
			</form>
		</Modal>
	);
}
