import type { TextareaHTMLAttributes } from 'react';

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
	label?: string;
	error?: string;
}

export function Textarea({
	label,
	error,
	className = '',
	id,
	...props
}: TextareaProps) {
	const inputId = id ?? label?.toLowerCase().replace(/\s+/g, '-');
	return (
		<div className={`flex flex-col gap-1.5 ${className}`}>
			{label && (
				<label htmlFor={inputId} className='label text-on-surface-variant'>
					{label}
				</label>
			)}
			<textarea
				id={inputId}
				className='bg-transparent border-b border-outline-variant/30 pb-2 text-on-surface text-sm font-light
          placeholder:text-on-surface-variant/40 focus:border-primary focus:outline-none transition-colors
          duration-300 resize-none min-h-[120px]'
				{...props}
			/>
			{error && <span className='text-error text-xs'>{error}</span>}
		</div>
	);
}
