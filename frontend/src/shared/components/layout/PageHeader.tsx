import { cn } from '@/lib/utils';

interface PageHeaderProps {
	eyebrow?: string;
	title: string;
	subtitle?: string;
	className?: string;
}

export function PageHeader({
	eyebrow,
	title,
	subtitle,
	className,
}: PageHeaderProps) {
	return (
		<div className={cn('text-center pt-32 pb-16 px-4', className)}>
			{eyebrow && (
				<p className='text-xs uppercase tracking-[0.3em] text-accent mb-4'>
					{eyebrow}
				</p>
			)}
			<h1 className='font-serif text-5xl md:text-7xl font-light text-foreground'>
				{title}
			</h1>
			{subtitle && (
				<p className='mt-4 text-muted max-w-xl mx-auto'>{subtitle}</p>
			)}
		</div>
	);
}
