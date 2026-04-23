import type { SelectHTMLAttributes } from 'react';

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
	label?: string;
	options: { value: string; label: string }[];
	error?: string;
}

export function Select({
	label,
	options,
	error,
	className = '',
	id,
	...props
}: SelectProps) {
	const selectId = id ?? label?.toLowerCase().replace(/\s+/g, '-');
	return (
		<div className={`flex flex-col gap-1.5 ${className}`}>
			{label && (
				<label htmlFor={selectId} className='label text-on-surface-variant'>
					{label}
				</label>
			)}
			<select
				id={selectId}
				className='bg-transparent border-b border-outline-variant/30 pb-2 text-on-surface text-sm font-light
          focus:border-primary focus:outline-none transition-colors duration-300 appearance-none cursor-pointer'
				{...props}
			>
				{options.map((o) => (
					<option
						key={o.value}
						value={o.value}
						className='bg-surface-container text-on-surface'
					>
						{o.label}
					</option>
				))}
			</select>
			{error && <span className='text-error text-xs'>{error}</span>}
		</div>
	);
}
