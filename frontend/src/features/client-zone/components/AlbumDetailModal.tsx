import { X, Download, Loader2, ChevronLeft, ChevronRight } from 'lucide-react';
import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { Button } from '@/shared/components/ui/Button';
import { PhotoGrid, PhotoGridItem } from '@/shared/components/PhotoGrid';
import { getPhotoUrl, downloadAlbumZip } from '../api/clientZoneApi';
import type { AlbumDto } from '@/shared/types/api';

interface AlbumDetailModalProps {
	album: AlbumDto;
	onClose: () => void;
}

const FOCUSABLE_SELECTOR =
	'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';

export function AlbumDetailModal({ album, onClose }: AlbumDetailModalProps) {
	const [downloading, setDownloading] = useState(false);
	// Indeks otwartego zdjęcia w lightboxie (null = lightbox zamknięty).
	const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);
	const visibleFiles = useMemo(
		() => album.files.filter((file) => file.visible),
		[album.files],
	);
	const dialogRef = useRef<HTMLDivElement>(null);
	const lightboxRef = useRef<HTMLDivElement>(null);
	const titleId = useId();

	// onClose bywa nową funkcją przy każdym renderze rodzica — trzymamy w ref,
	// żeby efekt a11y uruchamiał się tylko przy mount/unmount (modal jest montowany
	// warunkowo, więc mount = otwarcie, unmount = zamknięcie).
	const onCloseRef = useRef(onClose);
	useEffect(() => {
		onCloseRef.current = onClose;
	});

	// Mirror bieżących wartości do refów — jeden globalny listener klawiatury (efekt
	// z [] deps) musi widzieć aktualny stan lightboxa i liczbę zdjęć bez stale-closure.
	const lightboxIndexRef = useRef<number | null>(lightboxIndex);
	lightboxIndexRef.current = lightboxIndex;
	const countRef = useRef(visibleFiles.length);
	countRef.current = visibleFiles.length;

	const showPrev = () =>
		setLightboxIndex((i) =>
			i === null ? null : (i - 1 + visibleFiles.length) % visibleFiles.length,
		);
	const showNext = () =>
		setLightboxIndex((i) =>
			i === null ? null : (i + 1) % visibleFiles.length,
		);

	// A11y: blokada scrolla tła, Escape, strzałki lightboxa, pułapka Tab, focus.
	// Jeden listener na document obsługuje i modal, i lightbox — dzięki temu Escape
	// przy otwartym lightboxie zamyka TYLKO lightbox (nie cały modal).
	useEffect(() => {
		const previouslyFocused = document.activeElement as HTMLElement | null;
		const html = document.documentElement;
		const prevBodyOverflow = document.body.style.overflow;
		const prevHtmlOverflow = html.style.overflow;
		// Blokujemy oba (body ORAZ html) — w zależności od układu scrollowany bywa
		// element root, więc sama blokada body czasem nie wystarcza.
		document.body.style.overflow = 'hidden';
		html.style.overflow = 'hidden';
		const dialog = dialogRef.current;
		dialog?.focus();

		const focusables = () =>
			dialog
				? Array.from(
						dialog.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR),
					).filter((el) => el.offsetParent !== null)
				: [];

		const handleKeyDown = (e: KeyboardEvent) => {
			if (e.key === 'Escape') {
				e.stopPropagation();
				if (lightboxIndexRef.current !== null) {
					setLightboxIndex(null);
				} else {
					onCloseRef.current();
				}
				return;
			}

			// Nawigacja strzałkami tylko przy otwartym lightboxie.
			if (lightboxIndexRef.current !== null) {
				const count = countRef.current;
				if (e.key === 'ArrowRight') {
					e.preventDefault();
					setLightboxIndex((i) => (i === null ? null : (i + 1) % count));
					return;
				}
				if (e.key === 'ArrowLeft') {
					e.preventDefault();
					setLightboxIndex((i) =>
						i === null ? null : (i - 1 + count) % count,
					);
					return;
				}
			}

			if (e.key !== 'Tab') return;

			const items = focusables();
			if (items.length === 0) {
				e.preventDefault();
				dialog?.focus();
				return;
			}
			const first = items[0];
			const last = items[items.length - 1];
			if (e.shiftKey && document.activeElement === first) {
				e.preventDefault();
				last.focus();
			} else if (!e.shiftKey && document.activeElement === last) {
				e.preventDefault();
				first.focus();
			}
		};

		document.addEventListener('keydown', handleKeyDown);
		return () => {
			document.removeEventListener('keydown', handleKeyDown);
			document.body.style.overflow = prevBodyOverflow;
			html.style.overflow = prevHtmlOverflow;
			previouslyFocused?.focus?.();
		};
	}, []);

	// Po otwarciu lightboxa przenosimy do niego focus (dla klawiatury/czytników).
	useEffect(() => {
		if (lightboxIndex !== null) lightboxRef.current?.focus();
	}, [lightboxIndex]);

	// Prefetch oryginałów sąsiadów (następny + poprzedni) — po nawigacji ostra
	// wersja jest już w cache przeglądarki, więc przewijanie jest płynne. Ograniczone
	// do 2 zdjęć, żeby nie obciążać słabego VPS-a.
	useEffect(() => {
		if (lightboxIndex === null) return;
		const len = visibleFiles.length;
		if (len < 2) return;
		[(lightboxIndex + 1) % len, (lightboxIndex - 1 + len) % len].forEach(
			(idx) => {
				const file = visibleFiles[idx];
				if (file) {
					const img = new Image();
					img.src = getPhotoUrl(album.albumId, file.fileName);
				}
			},
		);
	}, [lightboxIndex, visibleFiles, album.albumId]);

	const handleDownload = async () => {
		if (visibleFiles.length === 0) return;
		setDownloading(true);
		try {
			await downloadAlbumZip(
				album.albumId,
				visibleFiles.map((f) => f.fileName),
			);
		} finally {
			setDownloading(false);
		}
	};

	const activeFile =
		lightboxIndex !== null ? visibleFiles[lightboxIndex] : undefined;

	return (
		<div
			ref={dialogRef}
			role='dialog'
			aria-modal='true'
			aria-labelledby={titleId}
			tabIndex={-1}
			className='fixed inset-0 z-[70] bg-background/95 overflow-y-auto focus:outline-none'
		>
			{/* Header */}
			<div className='sticky top-0 bg-surface/95 backdrop-blur-md border-b border-border z-10'>
				<div className='max-w-7xl mx-auto px-6 py-4 flex items-center justify-between'>
					<div>
						<p className='text-xs uppercase tracking-[0.2em] text-accent'>
							Kolekcja
						</p>
						<h2 id={titleId} className='font-serif text-2xl'>
							{album.name}
						</h2>
						<p className='text-xs text-muted'>
							{visibleFiles.length} zdjęć
							{album.ttd && (
								<>
									{' '}
									&middot; Wygasa{' '}
									{new Date(album.ttd).toLocaleDateString('pl-PL', {
										month: 'long',
										year: 'numeric',
									})}
								</>
							)}
						</p>
					</div>
					<button
						type='button'
						onClick={onClose}
						className='text-foreground/60 hover:text-foreground transition-colors'
						aria-label='Zamknij'
					>
						<X className='w-6 h-6' />
					</button>
				</div>
			</div>

			{/* Photo grid */}
			<div className='max-w-7xl mx-auto px-6 py-8'>
				<PhotoGrid columns={4}>
					{visibleFiles.map((file, idx) => (
						<PhotoGridItem
							key={file.fileID}
							src={getPhotoUrl(album.albumId, file.fileName, 600)}
							alt={file.fileName}
							onClick={() => setLightboxIndex(idx)}
						/>
					))}
				</PhotoGrid>

				{/* Download all */}
				<div className='flex justify-end mt-8'>
					<Button
						variant='outline'
						size='lg'
						onClick={handleDownload}
						disabled={downloading || visibleFiles.length === 0}
					>
						{downloading ? (
							<Loader2 className='w-4 h-4 mr-2 animate-spin' />
						) : (
							<Download className='w-4 h-4 mr-2' />
						)}
						Pobierz wszystkie
					</Button>
				</div>
			</div>

			{/* Lightbox — powiększenie pojedynczego zdjęcia */}
			{activeFile && (
				<div
					ref={lightboxRef}
					tabIndex={-1}
					role='dialog'
					aria-modal='true'
					aria-label={`Podgląd zdjęcia ${(lightboxIndex ?? 0) + 1} z ${visibleFiles.length}`}
					className='fixed inset-0 z-[80] bg-background/98 flex items-center justify-center focus:outline-none'
					onClick={(e) => {
						if (e.target === e.currentTarget) setLightboxIndex(null);
					}}
				>
					<button
						type='button'
						onClick={() => setLightboxIndex(null)}
						aria-label='Zamknij podgląd'
						className='absolute top-4 right-4 text-foreground/70 hover:text-foreground transition-colors'
					>
						<X className='w-7 h-7' />
					</button>

					{visibleFiles.length > 1 && (
						<button
							type='button'
							onClick={showPrev}
							aria-label='Poprzednie zdjęcie'
							className='absolute left-3 sm:left-6 text-foreground/60 hover:text-accent transition-colors'
						>
							<ChevronLeft className='w-9 h-9' />
						</button>
					)}

					{/* Progressive: miniatura (już w cache z gridu) pokazuje się natychmiast,
					    oryginał (bez ?width — pełna rozdzielczość, bo backend przy width oddaje
					    tylko 600px) doczytuje się w tle i podmienia, gdy gotowy. */}
					<LightboxImage
						key={activeFile.fileName}
						thumbSrc={getPhotoUrl(album.albumId, activeFile.fileName, 600)}
						fullSrc={getPhotoUrl(album.albumId, activeFile.fileName)}
						alt={activeFile.fileName}
					/>

					{visibleFiles.length > 1 && (
						<button
							type='button'
							onClick={showNext}
							aria-label='Następne zdjęcie'
							className='absolute right-3 sm:right-6 text-foreground/60 hover:text-accent transition-colors'
						>
							<ChevronRight className='w-9 h-9' />
						</button>
					)}

					<p className='absolute bottom-5 left-1/2 -translate-x-1/2 text-xs tracking-widest text-muted'>
						{(lightboxIndex ?? 0) + 1} / {visibleFiles.length}
					</p>
				</div>
			)}
		</div>
	);
}

/**
 * Obraz lightboxa z progresywnym ładowaniem: natychmiast pokazuje miniaturę
 * (zwykle już w cache przeglądarki z gridu), a w tle doczytuje oryginał i podmienia
 * `src`, gdy się załaduje. Dzięki temu otwieranie/nawigacja są natychmiastowe, a
 * ostrość „doskakuje" — bez blokowania na wolnym pobieraniu pełnego pliku.
 *
 * Reset do miniatury przy zmianie zdjęcia realizujemy przez `key` na komponencie
 * (remount inicjalizuje stan `thumbSrc`), zamiast synchronicznego setState w efekcie.
 */
function LightboxImage({
	thumbSrc,
	fullSrc,
	alt,
}: {
	thumbSrc: string;
	fullSrc: string;
	alt: string;
}) {
	const [src, setSrc] = useState(thumbSrc);

	useEffect(() => {
		let cancelled = false;
		const img = new Image();
		img.src = fullSrc;
		img.onload = () => {
			if (!cancelled) setSrc(fullSrc);
		};
		return () => {
			cancelled = true;
		};
	}, [fullSrc]);

	return (
		<img
			src={src}
			alt={alt}
			className='max-w-[92vw] max-h-[86vh] object-contain select-none'
		/>
	);
}
