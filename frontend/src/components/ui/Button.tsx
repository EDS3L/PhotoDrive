import type { ButtonHTMLAttributes, ReactNode } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
	variant?: 'primary' | 'outline' | 'ghost' | 'danger';
	size?: 'sm' | 'md' | 'lg';
	children: ReactNode;
	loading?: boolean;
}

export function Button({
	variant = 'primary',
	size = 'md',
	children,
	loading,
	className = '',
	disabled,
	...props
}: ButtonProps) {
	const base =
		'inline-flex items-center justify-center font-body uppercase tracking-[0.25em] font-medium transition-all duration-300 ease-out cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed';

	const variants = {
		primary: 'bg-primary text-on-primary hover:bg-primary-container',
		outline:
			'border border-on-surface text-on-surface hover:bg-surface-container',
		ghost: 'text-on-surface-variant hover:text-on-surface',
		danger: 'bg-error text-white hover:bg-error/80',
	};

	const sizes = {
		sm: 'px-4 py-1.5 text-[10px]',
		md: 'px-6 py-2.5 text-[11px]',
		lg: 'px-8 py-3.5 text-xs',
	};

	return (
		<button
			className={`${base} ${variants[variant]} ${sizes[size]} ${className}`}
			disabled={disabled || loading}
			{...props}
		>
			{loading ? (
				<span className='material-symbols-outlined animate-spin text-[16px] mr-2'>
					progress_activity
				</span>
			) : null}
			{children}
		</button>
	);
}
