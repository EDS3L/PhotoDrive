import type { ReactNode } from 'react';
import { useEffect } from 'react';
import { useUiStore } from '@/lib/stores/ui-store';

interface ModalProps {
	name: string;
	title?: string;
	subtitle?: string;
	children: ReactNode;
	maxWidth?: string;
}

export function Modal({
	name,
	title,
	subtitle,
	children,
	maxWidth = 'max-w-lg',
}: ModalProps) {
	const { activeModal, closeModal } = useUiStore();
	const isOpen = activeModal === name;

	useEffect(() => {
		if (isOpen) {
			document.body.style.overflow = 'hidden';
		} else {
			document.body.style.overflow = '';
		}
		return () => {
			document.body.style.overflow = '';
		};
	}, [isOpen]);

	if (!isOpen) return null;

	return (
		<div className='fixed inset-0 z-50 flex items-center justify-center p-4'>
			{/* Backdrop */}
			<div
				className='absolute inset-0 bg-background/80 backdrop-blur-sm'
				onClick={closeModal}
				onKeyDown={(e) => e.key === 'Escape' && closeModal()}
				role='button'
				tabIndex={0}
				aria-label='Zamknij'
			/>
			{/* Content */}
			<div
				className={`relative ${maxWidth} w-full bg-surface-container shadow-2xl z-10 animate-in fade-in`}
			>
				{(title || subtitle) && (
					<div className='px-8 pt-8 pb-4'>
						{title && (
							<h2 className='font-display text-3xl text-on-surface'>{title}</h2>
						)}
						{subtitle && (
							<p className='text-on-surface-variant text-sm mt-1'>{subtitle}</p>
						)}
						<div className='h-px bg-outline-variant/15 mt-4' />
					</div>
				)}
				<div className='px-8 pb-8'>{children}</div>
			</div>
		</div>
	);
}
