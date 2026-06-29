import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2 } from 'lucide-react';
import { useState } from 'react';
import { Input } from '@/shared/components/ui/Input';
import { Select } from '@/shared/components/ui/Select';
import { Button } from '@/shared/components/ui/Button';

const contactSchema = z.object({
	name: z.string().min(2, 'Imię jest wymagane'),
	email: z.string().email('Nieprawidłowy adres email'),
	phone: z.string().optional(),
	sessionType: z.string().min(1, 'Wybierz rodzaj sesji'),
	message: z.string().min(10, 'Wiadomość musi mieć minimum 10 znaków'),
});

type ContactFormData = z.infer<typeof contactSchema>;

interface ContactFormProps {
	onSuccess: () => void;
}

const sessionOptions = [
	{ value: '', label: 'Wybierz rodzaj sesji' },
	{ value: 'slub', label: 'Fotografia ślubna' },
	{ value: 'plener', label: 'Sesja plenerowa' },
	{ value: 'portret', label: 'Sesja portretowa' },
	{ value: 'reportaz', label: 'Reportaż' },
	{ value: 'inne', label: 'Inne' },
];

export function ContactForm({ onSuccess }: ContactFormProps) {
	const [isSubmitting, setIsSubmitting] = useState(false);

	const {
		register,
		handleSubmit,
		formState: { errors },
	} = useForm<ContactFormData>({
		resolver: zodResolver(contactSchema),
	});

	const onSubmit = (data: ContactFormData) => {
		setIsSubmitting(true);
		// Placeholder — replace with real API call or mailto
		const subject = encodeURIComponent(`Zapytanie: ${data.sessionType}`);
		const body = encodeURIComponent(
			`Imię: ${data.name}\nEmail: ${data.email}\nTelefon: ${data.phone ?? 'brak'}\nRodzaj: ${data.sessionType}\n\n${data.message}`,
		);
		window.location.assign(
			`mailto:kontakt@photodrive.dev?subject=${subject}&body=${body}`,
		);
		setTimeout(() => {
			setIsSubmitting(false);
			onSuccess();
		}, 500);
	};

	return (
		<form onSubmit={handleSubmit(onSubmit)} className='space-y-6'>
			<div className='grid grid-cols-1 sm:grid-cols-2 gap-6'>
				<Input
					id='name'
					label='Imię i nazwisko'
					placeholder='Jan Kowalski'
					error={errors.name?.message}
					{...register('name')}
				/>
				<Input
					id='email'
					label='Email'
					type='email'
					placeholder='jan@example.com'
					error={errors.email?.message}
					{...register('email')}
				/>
			</div>
			<div className='grid grid-cols-1 sm:grid-cols-2 gap-6'>
				<Input
					id='phone'
					label='Telefon'
					type='tel'
					placeholder='+48 123 456 789'
					{...register('phone')}
				/>
				<Select
					id='sessionType'
					label='Rodzaj sesji'
					options={sessionOptions}
					error={errors.sessionType?.message}
					{...register('sessionType')}
				/>
			</div>
			<div className='flex flex-col gap-1'>
				<label
					htmlFor='message'
					className='text-xs uppercase tracking-widest text-muted'
				>
					Wiadomość
				</label>
				<textarea
					id='message'
					rows={5}
					placeholder='Opowiedz mi o swoich planach...'
					className='w-full bg-transparent border-b border-border py-3 text-foreground placeholder:text-muted/60 focus:border-accent focus:outline-none transition-colors resize-none'
					{...register('message')}
				/>
				{errors.message && (
					<span className='text-xs text-error'>{errors.message.message}</span>
				)}
			</div>
			<Button
				type='submit'
				size='lg'
				className='w-full sm:w-auto'
				disabled={isSubmitting}
			>
				{isSubmitting ? (
					<>
						<Loader2 className='w-4 h-4 mr-2 animate-spin' />
						Wysyłanie...
					</>
				) : (
					'Wyślij wiadomość'
				)}
			</Button>
		</form>
	);
}
