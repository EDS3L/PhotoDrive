interface EmptyStateProps {
	icon: React.ReactNode;
	title: string;
	description?: string;
	action?: React.ReactNode;
}

export function EmptyState({
	icon,
	title,
	description,
	action,
}: EmptyStateProps) {
	return (
		<div className='flex flex-col items-center justify-center py-16 text-center'>
			<div className='text-muted mb-4'>{icon}</div>
			<h3 className='font-serif text-xl mb-1'>{title}</h3>
			{description && (
				<p className='text-sm text-muted mb-6 max-w-md'>{description}</p>
			)}
			{action}
		</div>
	);
}
