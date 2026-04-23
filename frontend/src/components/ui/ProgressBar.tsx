interface ProgressBarProps {
	value: number;
	max?: number;
	size?: 'sm' | 'md';
	className?: string;
	showLabel?: boolean;
}

export function ProgressBar({
	value,
	max = 100,
	size = 'sm',
	className = '',
	showLabel,
}: ProgressBarProps) {
	const pct = Math.min(Math.round((value / max) * 100), 100);
	return (
		<div className={`flex items-center gap-2 ${className}`}>
			<div
				className={`flex-1 bg-surface-container-high ${size === 'sm' ? 'h-1' : 'h-2'}`}
			>
				<div
					className={`h-full transition-all duration-500 ease-out ${
						pct === 100 ? 'bg-success' : 'bg-primary'
					}`}
					style={{ width: `${pct}%` }}
				/>
			</div>
			{showLabel && (
				<span className='text-on-surface-variant text-[10px] tabular-nums w-8 text-right'>
					{pct}%
				</span>
			)}
		</div>
	);
}
