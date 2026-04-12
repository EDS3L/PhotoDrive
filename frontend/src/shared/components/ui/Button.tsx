import { type ButtonHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

type ButtonVariant = 'primary' | 'outline' | 'ghost';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
	variant?: ButtonVariant;
	size?: ButtonSize;
}

const variantStyles: Record<ButtonVariant, string> = {
	primary: 'bg-accent text-background hover:bg-accent-hover',
	outline:
		'border border-foreground/30 text-foreground hover:border-foreground hover:bg-foreground/5',
	ghost: 'text-foreground hover:text-accent',
};

const sizeStyles: Record<ButtonSize, string> = {
	sm: 'px-4 py-2 text-xs',
	md: 'px-6 py-3 text-sm',
	lg: 'px-8 py-4 text-sm',
};

export function Button({
	variant = 'primary',
	size = 'md',
	className,
	children,
	...props
}: ButtonProps) {
	return (
		<button
			className={cn(
				'inline-flex items-center justify-center font-medium tracking-widest uppercase transition-colors duration-300',
				variantStyles[variant],
				sizeStyles[size],
				'disabled:opacity-50 disabled:cursor-not-allowed',
				className,
			)}
			{...props}
		>
			{children}
		</button>
	);
}
