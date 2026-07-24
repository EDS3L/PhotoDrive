import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2 } from 'lucide-react';
import { Input } from '@/shared/components/ui/Input';
import { Select } from '@/shared/components/ui/Select';
import { Button } from '@/shared/components/ui/Button';
import { useSendContactMessage } from '../hooks/useSendContactMessage';
import { SESSION_OPTIONS, buildContactPayload } from '../lib/contactForm';

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

export function ContactForm({ onSuccess }: ContactFormProps) {
	const { mutate, isPending } = useSendContactMessage();

	const {
		register,
		handleSubmit,
		formState: { errors },
	} = useForm<ContactFormData>({
		resolver: zodResolver(contactSchema),
	});

	const onSubmit = (data: ContactFormData) => {
		// Błąd wysyłki pokazuje globalny toast (MutationCache.onError); przy sukcesie
		// przechodzimy do ekranu podziękowania. Formularz zostaje, gdy się nie uda.
		mutate(buildContactPayload(data), { onSuccess });
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
					options={SESSION_OPTIONS}
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
				disabled={isPending}
			>
				{isPending ? (
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
