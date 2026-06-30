import { X } from 'lucide-react';
import { useEffect, useId, useRef, type ReactNode } from 'react';

interface ModalProps {
	open: boolean;
	onClose: () => void;
	title: string;
	children: ReactNode;
	maxWidth?: string;
}

const FOCUSABLE_SELECTOR =
	'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';

export function Modal({
	open,
	onClose,
	title,
	children,
	maxWidth = 'max-w-lg',
}: ModalProps) {
	const overlayRef = useRef<HTMLDivElement>(null);
	const dialogRef = useRef<HTMLDivElement>(null);
	const titleId = useId();

	// onClose bywa nową funkcją przy każdym renderze rodzica — trzymamy w ref,
	// żeby efekt focusu/Escape uruchamiał się tylko przy zmianie `open`.
	const onCloseRef = useRef(onClose);
	useEffect(() => {
		onCloseRef.current = onClose;
	});

	// Blokada scrolla tła
	useEffect(() => {
		if (open) {
			document.body.style.overflow = 'hidden';
		}
		return () => {
			document.body.style.overflow = '';
		};
	}, [open]);

	// Zarządzanie focusem: przeniesienie do dialogu, Escape, pułapka Tab,
	// przywrócenie focusu na element wyzwalający po zamknięciu.
	useEffect(() => {
		if (!open) return;

		const previouslyFocused = document.activeElement as HTMLElement | null;
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
			previouslyFocused?.focus?.();
		};
	}, [open]);

	if (!open) return null;

	return (
		<div
			ref={overlayRef}
			className='fixed inset-0 z-50 flex items-center justify-center bg-black/60'
			onClick={(e) => {
				if (e.target === overlayRef.current) onClose();
			}}
		>
			<div
				ref={dialogRef}
				role='dialog'
				aria-modal='true'
				aria-labelledby={titleId}
				tabIndex={-1}
				className={`bg-surface border border-border w-full ${maxWidth} mx-4 max-h-[90vh] flex flex-col focus:outline-none`}
			>
				<div className='flex items-center justify-between px-8 py-6 border-b border-border'>
					<h3 id={titleId} className='font-serif text-2xl font-light'>
						{title}
					</h3>
					<button
						type='button'
						onClick={onClose}
						aria-label='Zamknij'
						className='text-muted hover:text-foreground transition-colors'
					>
						<X className='w-5 h-5' />
					</button>
				</div>
				<div className='overflow-y-auto'>{children}</div>
			</div>
		</div>
	);
}
