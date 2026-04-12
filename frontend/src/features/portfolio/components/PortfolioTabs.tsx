import { Heart, Trees, User, Camera } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { PortfolioCategory } from '../types/portfolio.types';

const tabs: { id: PortfolioCategory; label: string; Icon: typeof Heart }[] = [
	{ id: 'sluby', label: 'Śluby', Icon: Heart },
	{ id: 'plener', label: 'Sesje plenerowe', Icon: Trees },
	{ id: 'portret', label: 'Portret', Icon: User },
	{ id: 'reportaz', label: 'Reportaż', Icon: Camera },
];

interface PortfolioTabsProps {
	active: PortfolioCategory;
	onChange: (category: PortfolioCategory) => void;
}

export function PortfolioTabs({ active, onChange }: PortfolioTabsProps) {
	return (
		<div className='flex justify-center gap-2 overflow-x-auto px-4 pb-2 no-scrollbar'>
			{tabs.map(({ id, label, Icon }) => (
				<button
					key={id}
					onClick={() => onChange(id)}
					className={cn(
						'flex items-center gap-2 px-5 py-3 text-xs uppercase tracking-[0.15em] whitespace-nowrap transition-all duration-300 border-b-2',
						active === id
							? 'border-accent text-accent'
							: 'border-transparent text-muted hover:text-foreground',
					)}
				>
					<Icon className='w-4 h-4' />
					{label}
				</button>
			))}
		</div>
	);
}
