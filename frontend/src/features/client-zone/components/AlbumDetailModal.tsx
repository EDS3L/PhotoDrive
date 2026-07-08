import { X, Download, Loader2 } from 'lucide-react';
import { useEffect, useId, useRef, useState } from 'react';
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
	const visibleFiles = album.files.filter((file) => file.visible);
	const dialogRef = useRef<HTMLDivElement>(null);
	const titleId = useId();

	// onClose bywa nową funkcją przy każdym renderze rodzica — trzymamy w ref,
	// żeby efekt a11y uruchamiał się tylko przy mount/unmount (modal jest montowany
	// warunkowo, więc mount = otwarcie, unmount = zamknięcie).
	const onCloseRef = useRef(onClose);
	useEffect(() => {
		onCloseRef.current = onClose;
	});

	// A11y: blokada scrolla tła, Escape, pułapka Tab, przeniesienie i przywrócenie focusu.
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
				onCloseRef.current();
				return;
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
					{visibleFiles.map((file) => (
						<PhotoGridItem
							key={file.fileID}
							src={getPhotoUrl(album.albumId, file.fileName, 600)}
							alt={file.fileName}
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
		</div>
	);
}
