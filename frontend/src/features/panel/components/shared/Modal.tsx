import { X } from 'lucide-react';
import { useEffect, useRef, type ReactNode } from 'react';

interface ModalProps {
	open: boolean;
	onClose: () => void;
	title: string;
	children: ReactNode;
	maxWidth?: string;
}

export function Modal({
	open,
	onClose,
	title,
	children,
	maxWidth = 'max-w-lg',
}: ModalProps) {
	const overlayRef = useRef<HTMLDivElement>(null);

	useEffect(() => {
		if (open) {
			document.body.style.overflow = 'hidden';
		} else {
			document.body.style.overflow = '';
		}
		return () => {
			document.body.style.overflow = '';
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
				className={`bg-surface border border-border w-full ${maxWidth} mx-4 max-h-[90vh] flex flex-col`}
			>
				<div className='flex items-center justify-between px-8 py-6 border-b border-border'>
					<h3 className='font-serif text-2xl font-light'>{title}</h3>
					<button
						onClick={onClose}
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
