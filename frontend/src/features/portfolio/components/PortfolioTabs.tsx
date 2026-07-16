import { cn } from '@/lib/utils';

export interface PortfolioTabInfo {
	albumId: string;
	label: string;
}

interface PortfolioTabsProps {
	tabs: PortfolioTabInfo[];
	activeId: string;
	onChange: (albumId: string) => void;
}

/**
 * Zakładki budowane z publicznych albumów admina (kolejność i etykiety ustawia panel) —
 * żadnych zaszytych kategorii. Ikony celowo zniknęły: kategorii nie znamy z góry,
 * a dobieranie ikon do dowolnych nazw byłoby zgadywaniem.
 */
export function PortfolioTabs({ tabs, activeId, onChange }: PortfolioTabsProps) {
	return (
		<div className='flex justify-center gap-2 overflow-x-auto px-4 pb-2 no-scrollbar'>
			{tabs.map(({ albumId, label }) => (
				<button
					key={albumId}
					onClick={() => onChange(albumId)}
					className={cn(
						'px-5 py-3 text-xs uppercase tracking-[0.15em] whitespace-nowrap transition-all duration-300 border-b-2',
						activeId === albumId
							? 'border-accent text-accent'
							: 'border-transparent text-muted hover:text-foreground',
					)}
				>
					{label}
				</button>
			))}
		</div>
	);
}
