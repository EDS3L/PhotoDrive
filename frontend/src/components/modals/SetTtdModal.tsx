import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { useSetTtd } from '@/hooks/use-albums';
import { useUiStore } from '@/lib/stores/ui-store';

export function SetTtdModal() {
	const closeModal = useUiStore((s) => s.closeModal);
	const modalData = useUiStore((s) => s.modalData) as {
		albumId: string;
	} | null;
	const setTtd = useSetTtd();

	const [date, setDate] = useState('');

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		if (!modalData?.albumId || !date) return;
		await setTtd.mutateAsync({ albumId: modalData.albumId, ttd: date });
		closeModal();
		setDate('');
	};

	return (
		<Modal
			name='set-ttd'
			title='Data wygaśnięcia'
			subtitle='Ustaw termin usunięcia albumu'
		>
			<form onSubmit={handleSubmit} className='space-y-5 mt-4'>
				<Input
					label='Data wygaśnięcia (TTD)'
					type='date'
					value={date}
					onChange={(e) => setDate(e.target.value)}
					required
				/>
				<div className='flex gap-3 justify-end pt-4'>
					<Button variant='ghost' type='button' onClick={closeModal}>
						Anuluj
					</Button>
					<Button type='submit' loading={setTtd.isPending}>
						Ustaw datę
					</Button>
				</div>
			</form>
		</Modal>
	);
}
