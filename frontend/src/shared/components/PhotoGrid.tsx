import { cn } from '@/lib/utils';

interface PhotoGridProps {
	columns?: 2 | 3 | 4;
	children: React.ReactNode;
	className?: string;
}

export function PhotoGrid({
	columns = 3,
	children,
	className,
}: PhotoGridProps) {
	const colClasses = {
		2: 'grid-cols-1 sm:grid-cols-2',
		3: 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3',
		4: 'grid-cols-2 sm:grid-cols-3 lg:grid-cols-4',
	};

	return (
		<div className={cn('grid gap-4', colClasses[columns], className)}>
			{children}
		</div>
	);
}

interface PhotoGridItemProps {
	src: string;
	alt: string;
	aspectRatio?: 'square' | 'portrait' | 'landscape';
	className?: string;
	onClick?: () => void;
}

export function PhotoGridItem({
	src,
	alt,
	aspectRatio = 'square',
	className,
	onClick,
}: PhotoGridItemProps) {
	const aspectClasses = {
		square: 'aspect-square',
		portrait: 'aspect-[3/4]',
		landscape: 'aspect-[4/3]',
	};

	return (
		<div
			className={cn(
				'group relative overflow-hidden cursor-pointer',
				aspectClasses[aspectRatio],
				className,
			)}
			onClick={onClick}
		>
			<img
				src={src}
				alt={alt}
				className='w-full h-full object-cover transition-transform duration-500 group-hover:scale-[1.04]'
				loading='lazy'
			/>
			<div className='absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors duration-300' />
		</div>
	);
}
