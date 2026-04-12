import { forwardRef, type SelectHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';
import { ChevronDown } from 'lucide-react';

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
	label?: string;
	error?: string;
	options?: { value: string; label: string }[];
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
	({ label, error, options, className, id, children, ...props }, ref) => {
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
				<div className='relative'>
					<select
						ref={ref}
						id={id}
						className={cn(
							'w-full appearance-none bg-transparent border-b border-border py-3 pr-8 text-foreground focus:border-accent focus:outline-none transition-colors',
							error && 'border-error',
							className,
						)}
						{...props}
					>
						{options
							? options.map((opt) => (
									<option
										key={opt.value}
										value={opt.value}
										className='bg-surface text-foreground'
									>
										{opt.label}
									</option>
								))
							: children}
					</select>
					<ChevronDown className='absolute right-0 top-1/2 -translate-y-1/2 w-4 h-4 text-muted pointer-events-none' />
				</div>
				{error && <span className='text-xs text-error'>{error}</span>}
			</div>
		);
	},
);

Select.displayName = 'Select';
