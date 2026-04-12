import { useState } from 'react';
import { PageHeader } from '@/shared/components/layout/PageHeader';
import { ContactForm } from './components/ContactForm';
import { ContactDetails } from './components/ContactDetails';
import { SuccessState } from './components/SuccessState';

export default function ContactPage() {
	const [submitted, setSubmitted] = useState(false);

	if (submitted) {
		return (
			<>
				<PageHeader eyebrow='Kontakt' title='Porozmawiajmy' />
				<div className='max-w-7xl mx-auto px-6 pb-24'>
					<SuccessState />
				</div>
			</>
		);
	}

	return (
		<>
			<PageHeader
				eyebrow='Kontakt'
				title='Porozmawiajmy'
				subtitle='Masz pytania dotyczące sesji? Chętnie odpowiem i pomogę zaplanować Twój wyjątkowy dzień.'
			/>

			<div className='max-w-7xl mx-auto px-6 pb-24'>
				<div className='grid grid-cols-1 lg:grid-cols-5 gap-16'>
					<div className='lg:col-span-3'>
						<ContactForm onSuccess={() => setSubmitted(true)} />
					</div>
					<div className='lg:col-span-2'>
						<ContactDetails />
					</div>
				</div>
			</div>
		</>
	);
}
