import { cn } from '@/lib/utils';

interface StatsCardProps {
	label: string;
	value: string | number;
	icon: React.ReactNode;
	className?: string;
}

export function StatsCard({ label, value, icon, className }: StatsCardProps) {
	return (
		<div
			className={cn(
				'bg-surface border border-border p-6 flex items-center gap-4',
				className,
			)}
		>
			<div className='text-accent'>{icon}</div>
			<div>
				<p className='text-2xl font-light text-foreground'>{value}</p>
				<p className='text-xs uppercase tracking-widest text-muted'>{label}</p>
			</div>
		</div>
	);
}
