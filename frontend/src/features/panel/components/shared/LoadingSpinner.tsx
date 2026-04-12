import { Loader2 } from 'lucide-react';

export function LoadingSpinner() {
	return (
		<div className='flex items-center justify-center py-24'>
			<Loader2 className='w-8 h-8 text-accent animate-spin' />
		</div>
	);
}
