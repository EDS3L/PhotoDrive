interface PhotoPlaceholderProps {
	label?: string;
	className?: string;
}

export function PhotoPlaceholder({
	label = 'Zdjęcie',
	className = '',
}: PhotoPlaceholderProps) {
	return (
		<div
			className={`relative flex items-end justify-start overflow-hidden bg-gradient-to-t from-surface-container to-surface-container-low ${className}`}
		>
			<div className='absolute inset-0 flex items-center justify-center'>
				<span className='material-symbols-outlined text-4xl text-on-surface-variant/10'>
					photo_camera
				</span>
			</div>
			<span className='relative z-10 px-3 py-2 text-[10px] tracking-[0.15em] uppercase text-on-surface-variant/50 font-body'>
				{label}
			</span>
		</div>
	);
}
