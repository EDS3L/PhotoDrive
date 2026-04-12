import { CheckCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Button } from '@/shared/components/ui/Button';

export function SuccessState() {
	return (
		<div className='text-center py-16'>
			<CheckCircle className='w-16 h-16 text-accent mx-auto mb-6' />
			<h2 className='font-serif text-3xl mb-4'>Wiadomość wysłana!</h2>
			<p className='text-muted mb-8 max-w-md mx-auto'>
				Dziękuję za kontakt. Odpowiem najszybciej jak to możliwe, zazwyczaj w
				ciągu 24 godzin.
			</p>
			<Link to='/'>
				<Button variant='outline'>Wróć na stronę główną</Button>
			</Link>
		</div>
	);
}
