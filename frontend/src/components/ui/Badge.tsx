import type { ReactNode } from 'react';

interface BadgeProps {
	children: ReactNode;
	variant?: 'default' | 'primary' | 'success' | 'warning' | 'error' | 'muted';
	className?: string;
}

const variants = {
	default: 'bg-surface-container-high text-on-surface',
	primary: 'bg-primary/15 text-primary',
	success: 'bg-success/15 text-success',
	warning: 'bg-warning/15 text-warning',
	error: 'bg-error/15 text-error',
	muted: 'bg-surface-container text-on-surface-variant',
};

export function Badge({
	children,
	variant = 'default',
	className = '',
}: BadgeProps) {
	return (
		<span
			className={`inline-flex items-center gap-1 px-2.5 py-0.5 text-[10px] uppercase tracking-[0.2em] font-medium ${variants[variant]} ${className}`}
		>
			{children}
		</span>
	);
}
