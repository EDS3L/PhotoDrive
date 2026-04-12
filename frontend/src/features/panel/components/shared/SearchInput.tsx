import { Search } from 'lucide-react';

interface SearchInputProps {
	value: string;
	onChange: (value: string) => void;
	placeholder?: string;
	className?: string;
}

export function SearchInput({
	value,
	onChange,
	placeholder = 'Szukaj...',
	className = '',
}: SearchInputProps) {
	return (
		<div className={`relative ${className}`}>
			<Search className='absolute left-0 top-1/2 -translate-y-1/2 w-4 h-4 text-muted' />
			<input
				type='text'
				value={value}
				onChange={(e) => onChange(e.target.value)}
				placeholder={placeholder}
				className='w-full bg-transparent border-b border-border py-2 pl-6 pr-2 text-sm text-foreground placeholder:text-muted/60 focus:border-accent focus:outline-none transition-colors'
			/>
		</div>
	);
}
