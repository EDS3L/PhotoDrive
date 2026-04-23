import { Link } from 'react-router';
import { Button } from '@/components/ui/Button';

export function ContactSuccessPage() {
	return (
		<div className='pt-32 pb-24 flex items-center justify-center min-h-[80vh]'>
			<div className='max-w-md text-center px-6'>
				<span className='material-symbols-outlined text-success text-6xl mb-6'>
					check_circle
				</span>
				<h1 className='font-display text-4xl text-on-surface mb-4'>
					Wiadomość wysłana
				</h1>
				<p className='text-on-surface-variant text-sm mb-8 leading-relaxed'>
					Dziękuję za Twoją wiadomość! Odpowiem najszybciej jak to możliwe,
					zwykle w ciągu 24 godzin.
				</p>
				<Link to='/'>
					<Button variant='outline'>Wróć na stronę główną</Button>
				</Link>
			</div>
		</div>
	);
}
