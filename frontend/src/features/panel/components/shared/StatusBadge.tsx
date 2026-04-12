import { cn } from '@/lib/utils';

type BadgeVariant = 'default' | 'success' | 'warning' | 'error' | 'accent';

interface StatusBadgeProps {
	variant?: BadgeVariant;
	children: React.ReactNode;
	className?: string;
}

const variantStyles: Record<BadgeVariant, string> = {
	default: 'bg-border text-muted',
	success: 'bg-green-900/30 text-green-400',
	warning: 'bg-yellow-900/30 text-yellow-400',
	error: 'bg-red-900/30 text-red-400',
	accent: 'bg-accent/10 text-accent',
};

export function StatusBadge({
	variant = 'default',
	children,
	className,
}: StatusBadgeProps) {
	return (
		<span
			className={cn(
				'inline-flex items-center px-2 py-0.5 text-[10px] uppercase tracking-wider font-medium',
				variantStyles[variant],
				className,
			)}
		>
			{children}
		</span>
	);
}
