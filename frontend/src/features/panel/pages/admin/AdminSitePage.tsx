import { useRef, useState } from 'react';
import { ImageOff, Upload, Trash2, Loader2, Info } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { ConfirmDialog } from '../../components/shared/ConfirmDialog';
import { LoadingSpinner } from '../../components/shared/LoadingSpinner';
import {
	useAdminSiteSlots,
	useUploadSiteSlotImage,
	useDeleteSiteSlotImage,
} from '../../hooks/useSiteSlots';
import { getSiteSlotPreviewUrl } from '../../api/siteSlotsApi';
import { toast } from '@/shared/store/toastStore';
import type { SiteSlotDto } from '@/shared/types/api';

/**
 * Etykiety slotów — jedyne miejsce, w którym front tłumaczy klucz slotu na język
 * człowieka. Slot nieznany tej mapie (świeżo dodany w backendzie) i tak jest
 * wyświetlany, tylko z surowym kluczem — nowa sekcja nie wymaga zmiany panelu.
 */
const SLOT_LABELS: Record<string, { label: string; hint: string }> = {
	HOME_HERO: {
		label: 'Strona główna — tło hero',
		hint: 'Pełnoekranowe zdjęcie powitalne. Najlepiej poziome, min. 1920 px szerokości.',
	},
	HOME_INTRO: {
		label: 'Strona główna — przy cytacie',
		hint: 'Pionowe zdjęcie obok cytatu założyciela (proporcje ok. 3:4).',
	},
	HOME_CTA: {
		label: 'Strona główna — tło sekcji kontaktowej',
		hint: 'Poziome zdjęcie pod hasłem „Stwórzmy razem coś pięknego".',
	},
	ABOUT_BIO: {
		label: 'O mnie — portret',
		hint: 'Pionowy portret fotografa na stronie „O mnie" (proporcje ok. 3:4).',
	},
	ABOUT_EQUIPMENT: {
		label: 'O mnie — tło sekcji o sprzęcie',
		hint: 'Poziome zdjęcie (np. sprzęt, plener) jako tło sekcji.',
	},
	CLIENT_LOGIN: {
		label: 'Strefa klienta — ekran logowania',
		hint: 'Pionowe zdjęcie lewego panelu ekranu logowania klienta (proporcje ok. 8:9).',
	},
	PANEL_LOGIN: {
		label: 'Panel — ekran logowania',
		hint: 'Pionowe zdjęcie lewego panelu ekranu logowania do panelu zarządzania.',
	},
};

function slotLabel(slot: string) {
	return SLOT_LABELS[slot] ?? { label: slot, hint: '' };
}

export default function AdminSitePage() {
	const { data: slots, isLoading } = useAdminSiteSlots();
	const uploadMutation = useUploadSiteSlotImage();
	const deleteMutation = useDeleteSiteSlotImage();
	const fileInputRef = useRef<HTMLInputElement>(null);
	const [pendingSlot, setPendingSlot] = useState<string | null>(null);
	const [confirmDelete, setConfirmDelete] = useState<string | null>(null);

	if (isLoading) return <LoadingSpinner />;

	const pickFileFor = (slot: string) => {
		setPendingSlot(slot);
		fileInputRef.current?.click();
	};

	const handleFileChange = (files: FileList | null) => {
		const file = files?.[0];
		// Zamknięcie okna wyboru pliku nie może zostawić „uzbrojonego" slotu.
		const slot = pendingSlot;
		setPendingSlot(null);
		if (!file || !slot) return;
		uploadMutation.mutate(
			{ slot, file },
			{
				onSuccess: () =>
					toast.success(
						`Zdjęcie sekcji „${slotLabel(slot).label}" zapisane`,
					),
			},
		);
		// pozwala wgrać ten sam plik ponownie
		if (fileInputRef.current) fileInputRef.current.value = '';
	};

	return (
		<div>
			<div className='mb-6'>
				<h2 className='font-serif text-4xl font-light'>Strona wizytówka</h2>
				<p className='text-sm text-muted mt-2 max-w-2xl'>
					Zdjęcia stałych sekcji strony publicznej. Każda sekcja ma dokładnie
					jedno zdjęcie — wgranie nowego podmienia stare, a sekcja bez zdjęcia
					pokazuje neutralny placeholder. Serwer sam skaluje wgrany plik do
					rozmiaru publicznego (2560 px), więc można wgrywać oryginały.
				</p>
			</div>

			<div className='space-y-4'>
				{slots?.map((slot: SiteSlotDto) => {
					const { label, hint } = slotLabel(slot.slot);
					const isUploading =
						uploadMutation.isPending &&
						uploadMutation.variables?.slot === slot.slot;
					return (
						<div
							key={slot.slot}
							className='bg-surface border border-border p-4 flex flex-col sm:flex-row sm:items-center gap-4'
						>
							{/* Podgląd */}
							<div className='w-40 h-24 shrink-0 bg-black/40 border border-border flex items-center justify-center overflow-hidden'>
								{slot.configured && slot.updatedAt ? (
									<img
										src={getSiteSlotPreviewUrl(slot.slot, slot.updatedAt)}
										alt={label}
										className='w-full h-full object-cover'
									/>
								) : (
									<ImageOff className='w-6 h-6 text-muted' />
								)}
							</div>

							{/* Opis */}
							<div className='flex-1 min-w-0'>
								<p className='font-medium'>{label}</p>
								{hint && <p className='text-xs text-muted mt-1'>{hint}</p>}
								{!slot.configured && (
									<p className='text-xs text-accent mt-1'>
										Brak zdjęcia — sekcja pokazuje placeholder.
									</p>
								)}
							</div>

							{/* Akcje */}
							<div className='flex items-center gap-2 shrink-0'>
								<Button
									size='sm'
									aria-label={`${slot.configured ? 'Podmień' : 'Wgraj'} zdjęcie — ${label}`}
									onClick={() => pickFileFor(slot.slot)}
									disabled={uploadMutation.isPending}
								>
									{isUploading ? (
										<Loader2 className='w-4 h-4 mr-2 animate-spin' />
									) : (
										<Upload className='w-4 h-4 mr-2' />
									)}
									{slot.configured ? 'Podmień' : 'Wgraj zdjęcie'}
								</Button>
								{slot.configured && (
									<Button
										size='sm'
										variant='ghost'
										className='text-red-400 hover:text-red-300'
										aria-label={`Usuń zdjęcie — ${label}`}
										onClick={() => setConfirmDelete(slot.slot)}
										disabled={deleteMutation.isPending}
									>
										<Trash2 className='w-4 h-4' />
									</Button>
								)}
							</div>
						</div>
					);
				})}
			</div>

			<input
				ref={fileInputRef}
				type='file'
				accept='image/jpeg,image/png'
				className='hidden'
				aria-label='Plik zdjęcia sekcji'
				onChange={(e) => handleFileChange(e.target.files)}
			/>

			<div className='mt-8 p-4 bg-surface border border-border max-w-2xl'>
				<p className='flex items-start gap-2 text-xs text-muted'>
					<Info className='w-4 h-4 shrink-0 text-accent' />
					<span>
						Formaty: <strong>JPG lub PNG</strong> (maks. 30 MB). Zmiany są
						widoczne na stronie publicznej w ciągu ok. pół minuty. Zdjęcia
						sekcji nie są albumami — galerie portfolio zarządzane są osobno w
						„Albumach publicznych".
					</span>
				</p>
			</div>

			<ConfirmDialog
				open={confirmDelete !== null}
				title='Usunąć zdjęcie sekcji?'
				message={`Sekcja „${confirmDelete ? slotLabel(confirmDelete).label : ''}" wróci do neutralnego placeholdera na stronie publicznej.`}
				confirmLabel='Usuń'
				onClose={() => setConfirmDelete(null)}
				onConfirm={() => {
					const slot = confirmDelete;
					setConfirmDelete(null);
					if (!slot) return;
					deleteMutation.mutate(slot, {
						onSuccess: () => toast.success('Zdjęcie sekcji usunięte'),
					});
				}}
			/>
		</div>
	);
}
