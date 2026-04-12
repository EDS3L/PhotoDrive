import { forwardRef, type InputHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
	label?: string;
	error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
	({ label, error, className, id, ...props }, ref) => {
		return (
			<div className='flex flex-col gap-1'>
				{label && (
					<label
						htmlFor={id}
						className='text-xs uppercase tracking-widest text-muted'
					>
						{label}
					</label>
				)}
				<input
					ref={ref}
					id={id}
					className={cn(
						'w-full bg-transparent border-b border-border py-3 text-foreground placeholder:text-muted/60 focus:border-accent focus:outline-none transition-colors',
						error && 'border-error',
						className,
					)}
					{...props}
				/>
				{error && <span className='text-xs text-error'>{error}</span>}
			</div>
		);
	},
);

Input.displayName = 'Input';
