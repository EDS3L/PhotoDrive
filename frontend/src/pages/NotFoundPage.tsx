import { Link } from 'react-router';
import { Button } from '@/components/ui/Button';

export function NotFoundPage() {
	return (
		<div className='min-h-screen flex items-center justify-center px-6'>
			<div className='text-center max-w-md'>
				<p className='font-display text-[120px] text-primary/20 leading-none'>
					404
				</p>
				<h1 className='font-display text-3xl text-on-surface mt-2 mb-4'>
					Strona nie istnieje
				</h1>
				<p className='text-on-surface-variant text-sm mb-8'>
					Strona, której szukasz, nie została odnaleziona lub została
					przeniesiona.
				</p>
				<Link to='/'>
					<Button variant='outline'>Wróć na stronę główną</Button>
				</Link>
			</div>
		</div>
	);
}
