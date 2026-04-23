import type { InputHTMLAttributes } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
	label?: string;
	error?: string;
	helperText?: string;
}

export function Input({
	label,
	error,
	helperText,
	className = '',
	id,
	...props
}: InputProps) {
	const inputId = id ?? label?.toLowerCase().replace(/\s+/g, '-');
	return (
		<div className={`flex flex-col gap-1.5 ${className}`}>
			{label && (
				<label htmlFor={inputId} className='label text-on-surface-variant'>
					{label}
				</label>
			)}
			<input
				id={inputId}
				className='bg-transparent border-b border-outline-variant/30 pb-2 text-on-surface text-sm font-light
          placeholder:text-on-surface-variant/40 focus:border-primary focus:outline-none transition-colors duration-300'
				{...props}
			/>
			{error && <span className='text-error text-xs'>{error}</span>}
			{helperText && !error && (
				<span className='text-on-surface-variant/60 text-xs'>{helperText}</span>
			)}
		</div>
	);
}
