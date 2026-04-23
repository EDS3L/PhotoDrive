import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Textarea } from '@/components/ui/Textarea';
import { Button } from '@/components/ui/Button';

const SESSION_TYPES = [
	{ value: '', label: 'Wybierz typ sesji...' },
	{ value: 'slubna', label: 'Sesja ślubna' },
	{ value: 'plenerowa', label: 'Sesja plenerowa' },
	{ value: 'portretowa', label: 'Sesja portretowa' },
	{ value: 'reportaz', label: 'Reportaż' },
	{ value: 'inna', label: 'Inna' },
];

export function ContactPage() {
	const navigate = useNavigate();
	const [isSubmitting, setIsSubmitting] = useState(false);
	const [form, setForm] = useState({
		name: '',
		email: '',
		phone: '',
		sessionType: '',
		message: '',
	});

	const handleSubmit = async (e: FormEvent) => {
		e.preventDefault();
		setIsSubmitting(true);
		try {
			// TODO: POST /api/public/contact once backend endpoint exists
			// await api.post('/public/contact', form);
			await new Promise((r) => setTimeout(r, 1000)); // Simulate
			navigate('/kontakt/sukces');
		} finally {
			setIsSubmitting(false);
		}
	};

	return (
		<div className='pt-32 pb-24'>
			<div className='max-w-7xl mx-auto px-6 lg:px-12'>
				<div className='mb-16'>
					<p className='label text-[11px] text-primary mb-4'>KONTAKT</p>
					<h1 className='font-display text-5xl lg:text-6xl text-on-surface leading-[1]'>
						Porozmawiajmy
						<br />
						<span className='italic'>o Twoim projekcie</span>
					</h1>
				</div>

				<div className='grid grid-cols-1 lg:grid-cols-[1fr,0.6fr] gap-16'>
					{/* Form */}
					<form onSubmit={handleSubmit} className='flex flex-col gap-8'>
						<div className='grid grid-cols-1 sm:grid-cols-2 gap-8'>
							<Input
								label='Imię i nazwisko'
								value={form.name}
								onChange={(e) => setForm({ ...form, name: e.target.value })}
								required
							/>
							<Input
								label='Email'
								type='email'
								value={form.email}
								onChange={(e) => setForm({ ...form, email: e.target.value })}
								required
							/>
							<Input
								label='Telefon'
								type='tel'
								value={form.phone}
								onChange={(e) => setForm({ ...form, phone: e.target.value })}
							/>
							<Select
								label='Typ sesji'
								options={SESSION_TYPES}
								value={form.sessionType}
								onChange={(e) =>
									setForm({ ...form, sessionType: e.target.value })
								}
							/>
						</div>
						<Textarea
							label='Wiadomość'
							value={form.message}
							onChange={(e) => setForm({ ...form, message: e.target.value })}
							placeholder='Opowiedz o swoim pomyśle...'
							required
						/>
						<Button
							type='submit'
							loading={isSubmitting}
							size='lg'
							className='self-start'
						>
							WYŚLIJ WIADOMOŚĆ
						</Button>
					</form>

					{/* Contact details */}
					<div className='flex flex-col gap-8'>
						<div className='flex items-start gap-4'>
							<span className='material-symbols-outlined text-primary text-[20px] mt-0.5'>
								mail
							</span>
							<div>
								<p className='label text-[10px] text-on-surface-variant mb-1'>
									EMAIL
								</p>
								<p className='text-on-surface text-sm'>kontakt@photodrive.pl</p>
							</div>
						</div>
						<div className='flex items-start gap-4'>
							<span className='material-symbols-outlined text-primary text-[20px] mt-0.5'>
								phone
							</span>
							<div>
								<p className='label text-[10px] text-on-surface-variant mb-1'>
									TELEFON
								</p>
								<p className='text-on-surface text-sm'>+48 123 456 789</p>
							</div>
						</div>
						<div className='flex items-start gap-4'>
							<span className='material-symbols-outlined text-primary text-[20px] mt-0.5'>
								location_on
							</span>
							<div>
								<p className='label text-[10px] text-on-surface-variant mb-1'>
									LOKALIZACJA
								</p>
								<p className='text-on-surface text-sm'>Warszawa i okolice</p>
							</div>
						</div>
						<div className='flex items-start gap-4'>
							<span className='material-symbols-outlined text-primary text-[20px] mt-0.5'>
								schedule
							</span>
							<div>
								<p className='label text-[10px] text-on-surface-variant mb-1'>
									DOSTĘPNOŚĆ
								</p>
								<p className='text-on-surface text-sm'>Pon-Pt: 9:00 – 18:00</p>
							</div>
						</div>

						{/* Info box */}
						<div className='border-l-2 border-primary bg-surface-container-low p-4 mt-4'>
							<div className='flex items-start gap-3'>
								<span className='material-symbols-outlined text-primary text-[18px]'>
									info
								</span>
								<p className='text-on-surface-variant text-xs leading-relaxed'>
									Odpowiadam na wiadomości w ciągu 24 godzin. W pilnych sprawach
									proszę o kontakt telefoniczny.
								</p>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	);
}
