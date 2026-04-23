interface PaginationProps {
	currentPage: number;
	totalPages: number;
	onPageChange: (page: number) => void;
	totalItems?: number;
	pageSize?: number;
}

export function Pagination({
	currentPage,
	totalPages,
	onPageChange,
	totalItems,
	pageSize,
}: PaginationProps) {
	if (totalPages <= 1) return null;

	const pages: (number | '...')[] = [];
	for (let i = 1; i <= totalPages; i++) {
		if (
			i === 1 ||
			i === totalPages ||
			(i >= currentPage - 1 && i <= currentPage + 1)
		) {
			pages.push(i);
		} else if (pages[pages.length - 1] !== '...') {
			pages.push('...');
		}
	}

	return (
		<div className='flex items-center justify-between pt-6'>
			{totalItems !== undefined && pageSize !== undefined && (
				<span className='text-on-surface-variant text-xs'>
					Pokazuje {(currentPage - 1) * pageSize + 1}–
					{Math.min(currentPage * pageSize, totalItems)} z {totalItems}
				</span>
			)}
			<div className='flex items-center gap-1 ml-auto'>
				<button
					onClick={() => onPageChange(currentPage - 1)}
					disabled={currentPage === 1}
					className='w-8 h-8 flex items-center justify-center text-on-surface-variant
            hover:bg-surface-container-high transition-colors disabled:opacity-30 disabled:cursor-not-allowed'
				>
					<span className='material-symbols-outlined text-[18px]'>
						chevron_left
					</span>
				</button>
				{pages.map((p, i) =>
					p === '...' ? (
						<span
							key={`ellipsis-${i}`}
							className='w-8 h-8 flex items-center justify-center text-on-surface-variant text-sm'
						>
							…
						</span>
					) : (
						<button
							key={p}
							onClick={() => onPageChange(p)}
							className={`w-8 h-8 flex items-center justify-center text-xs transition-colors ${
								currentPage === p
									? 'bg-primary text-on-primary'
									: 'text-on-surface-variant hover:bg-surface-container-high'
							}`}
						>
							{p}
						</button>
					),
				)}
				<button
					onClick={() => onPageChange(currentPage + 1)}
					disabled={currentPage === totalPages}
					className='w-8 h-8 flex items-center justify-center text-on-surface-variant
            hover:bg-surface-container-high transition-colors disabled:opacity-30 disabled:cursor-not-allowed'
				>
					<span className='material-symbols-outlined text-[18px]'>
						chevron_right
					</span>
				</button>
			</div>
		</div>
	);
}
