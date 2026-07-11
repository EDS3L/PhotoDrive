import { useRef, useState } from 'react';
import { Droplets, Upload, Trash2, Loader2, Info } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { ConfirmDialog } from '../../components/shared/ConfirmDialog';
import { LoadingSpinner } from '../../components/shared/LoadingSpinner';
import {
	useWatermarkStatus,
	useUploadWatermark,
	useDeleteWatermark,
} from '../../hooks/useWatermark';
import { getWatermarkImageUrl } from '../../api/watermarkApi';
import { toast } from '@/shared/store/toastStore';

export default function AdminWatermark() {
	const { data: status, isLoading } = useWatermarkStatus();
	const uploadMutation = useUploadWatermark();
	const deleteMutation = useDeleteWatermark();
	const fileInputRef = useRef<HTMLInputElement>(null);
	const [confirmDelete, setConfirmDelete] = useState(false);

	if (isLoading) return <LoadingSpinner />;

	const configured = status?.configured ?? false;

	const handleFileChange = (files: FileList | null) => {
		const file = files?.[0];
		if (!file) return;
		uploadMutation.mutate(file, {
			onSuccess: () =>
				toast.success(
					configured ? 'Znak wodny podmieniony' : 'Znak wodny zapisany',
				),
		});
		// pozwala wgrać ten sam plik ponownie
		if (fileInputRef.current) fileInputRef.current.value = '';
	};

	return (
		<div>
			<div className='mb-6'>
				<h2 className='font-serif text-4xl font-light'>Znak wodny</h2>
				<p className='text-sm text-muted mt-2 max-w-2xl'>
					Jeden globalny znak wodny platformy. Zdjęcia oznaczone watermarkiem są
					serwowane klientom z nałożonym logo (kafelki na całej powierzchni) —
					oryginały na dysku pozostają nietknięte. Bez wgranego loga opcja
					watermarku jest ukryta u fotografów.
				</p>
			</div>

			{/* Podgląd */}
			<div className='bg-surface border border-border p-6 mb-6'>
				<p className='text-[10px] uppercase tracking-widest text-muted mb-4'>
					Aktualne logo
				</p>
				{configured ? (
					<div className='inline-block p-6 bg-black/40 border border-border'>
						<img
							src={getWatermarkImageUrl(status?.updatedAt ?? null)}
							alt='Znak wodny platformy'
							className='max-h-40 max-w-full'
						/>
					</div>
				) : (
					<div className='border-2 border-dashed border-border py-12 px-8 text-center max-w-md'>
						<Droplets className='w-10 h-10 text-muted mx-auto mb-3' />
						<p className='text-muted text-sm'>
							Brak znaku wodnego — wgraj logo w formacie PNG, aby udostępnić
							watermark fotografom.
						</p>
					</div>
				)}
			</div>

			{/* Akcje */}
			<div className='flex items-center gap-3'>
				<Button
					onClick={() => fileInputRef.current?.click()}
					disabled={uploadMutation.isPending}
				>
					{uploadMutation.isPending ? (
						<Loader2 className='w-4 h-4 mr-2 animate-spin' />
					) : (
						<Upload className='w-4 h-4 mr-2' />
					)}
					{configured ? 'Podmień logo' : 'Wgraj logo'}
				</Button>
				<input
					ref={fileInputRef}
					type='file'
					accept='image/png'
					className='hidden'
					onChange={(e) => handleFileChange(e.target.files)}
				/>
				{configured && (
					<Button
						variant='ghost'
						className='text-red-400 hover:text-red-300'
						onClick={() => setConfirmDelete(true)}
						disabled={deleteMutation.isPending}
					>
						<Trash2 className='w-4 h-4 mr-2' />
						Usuń
					</Button>
				)}
			</div>

			{/* Wskazówki */}
			<div className='mt-8 p-4 bg-surface border border-border max-w-2xl'>
				<p className='flex items-start gap-2 text-xs text-muted'>
					<Info className='w-4 h-4 shrink-0 text-accent' />
					<span>
						Najlepiej sprawdza się <strong>PNG z przezroczystym tłem</strong>{' '}
						(maks. 2 MB). Podmiana loga automatycznie odświeży wszystkie
						watermarkowane podglądy. Usunięcie jest możliwe tylko wtedy, gdy
						żaden plik nie ma włączonego watermarku.
					</span>
				</p>
			</div>

			<ConfirmDialog
				open={confirmDelete}
				title='Usunąć znak wodny?'
				message='Fotografowie stracą możliwość oznaczania zdjęć watermarkiem, a opcja zniknie z panelu. Usunięcie nie powiedzie się, jeśli jakiekolwiek zdjęcie ma włączony watermark.'
				confirmLabel='Usuń'
				onClose={() => setConfirmDelete(false)}
				onConfirm={() => {
					setConfirmDelete(false);
					deleteMutation.mutate(undefined, {
						onSuccess: () => toast.success('Znak wodny usunięty'),
					});
				}}
			/>
		</div>
	);
}
