import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2 } from 'lucide-react';
import { Input } from '@/shared/components/ui/Input';
import { Button } from '@/shared/components/ui/Button';
import { useLogin } from '../hooks/useLogin';
import { placeholder } from '@/lib/placeholder';
import { useState } from 'react';
import { requestPasswordToken, resetPassword } from '../api/clientZoneApi';
import { getApiErrorMessage } from '@/lib/queryClient';

const loginSchema = z.object({
	email: z.string().email('Nieprawidłowy adres email'),
	password: z.string().min(1, 'Hasło jest wymagane'),
});

const resetSchema = z
	.object({
		token: z.string().uuid('Nieprawidłowy kod autoryzacji'),
		newPassword: z.string().min(8, 'Hasło musi mieć min. 8 znaków'),
		confirmPassword: z.string(),
	})
	.refine((data) => data.newPassword === data.confirmPassword, {
		message: 'Hasła muszą być takie same',
		path: ['confirmPassword'],
	});

type LoginFormData = z.infer<typeof loginSchema>;
type ResetFormData = z.infer<typeof resetSchema>;

export function LoginForm() {
	const { mutate, isPending, isError } = useLogin();
	const [showReset, setShowReset] = useState(false);
	const [resetEmailSent, setResetEmailSent] = useState(false);
	const [resetEmail, setResetEmail] = useState('');
	const [resetComplete, setResetComplete] = useState(false);
	const [resetError, setResetError] = useState<string | null>(null);
	const [resetPending, setResetPending] = useState(false);

	const {
		register,
		handleSubmit,
		getValues,
		formState: { errors },
	} = useForm<LoginFormData>({
		resolver: zodResolver(loginSchema),
	});

	const resetForm = useForm<ResetFormData>({
		resolver: zodResolver(resetSchema),
	});

	const onSubmit = (data: LoginFormData) => {
		mutate(data);
	};

	const openReset = () => {
		// Zawsze przenosi na ekran resetu; email podpowiadamy z formularza logowania,
		// jeśli był wpisany (ale NIE jest wymagany, żeby tu wejść).
		setResetEmail(getValues('email') || '');
		setResetError(null);
		setResetEmailSent(false);
		setShowReset(true);
	};

	const handleSendToken = async () => {
		if (!resetEmail) return;
		setResetPending(true);
		setResetError(null);
		try {
			await requestPasswordToken(resetEmail);
		} catch {
			// cicho — nie zdradzamy, czy konto istnieje (anty-enumeracja)
		} finally {
			setResetPending(false);
			setResetEmailSent(true);
		}
	};

	const handleResetPassword = async (data: ResetFormData) => {
		setResetPending(true);
		setResetError(null);
		try {
			await resetPassword(resetEmail, data.token, data.newPassword);
			setResetComplete(true);
		} catch (err) {
			setResetError(getApiErrorMessage(err));
		} finally {
			setResetPending(false);
		}
	};

	const handleBackToLogin = () => {
		setShowReset(false);
		setResetEmailSent(false);
		setResetComplete(false);
		setResetError(null);
		setResetEmail('');
	};

	return (
		<div className='min-h-screen flex'>
			{/* Left — photo panel (hidden on mobile) */}
			<div className='hidden lg:flex lg:w-1/2 relative items-end p-12'>
				<img
					src={placeholder(960, 1080, 'Login — portret')}
					alt=''
					className='absolute inset-0 w-full h-full object-cover'
					aria-hidden='true'
				/>
				<div className='absolute inset-0 bg-black/40' />
				<p className='relative z-10 font-serif text-3xl italic leading-snug'>
					Twoje wspomnienia
					<br />
					czekają
				</p>
			</div>

			{/* Right — form */}
			<div className='flex-1 flex items-center justify-center px-6 py-12 lg:px-16'>
				<div className='w-full max-w-md'>
					<p className='text-xs uppercase tracking-[0.3em] text-accent mb-2'>
						Strefa klienta
					</p>

					{resetComplete ? (
						<>
							<h1 className='font-serif text-4xl md:text-5xl font-light mb-2'>
								Hasło zmienione
							</h1>
							<p className='text-muted mb-10'>
								Możesz się teraz zalogować nowym hasłem.
							</p>
							<Button size='lg' className='w-full' onClick={handleBackToLogin}>
								Powrót do logowania
							</Button>
						</>
					) : showReset ? (
						<>
							<h1 className='font-serif text-4xl md:text-5xl font-light mb-2'>
								Zresetuj hasło
							</h1>

							{!resetEmailSent ? (
								<>
									<p className='text-muted mb-10'>
										Podaj swój adres email — wyślemy kod autoryzacji do ustawienia nowego
										hasła.
									</p>
									<div className='space-y-6'>
										<Input
											id='reset-email'
											label='Email'
											type='email'
											placeholder='anna@kowalska.pl'
											value={resetEmail}
											onChange={(e) => setResetEmail(e.target.value)}
										/>
										<Button
											size='lg'
											className='w-full'
											onClick={handleSendToken}
											disabled={resetPending || !resetEmail}
										>
											{resetPending ? (
												<>
													<Loader2 className='w-4 h-4 mr-2 animate-spin' />
													Wysyłanie...
												</>
											) : (
												'Wyślij kod'
											)}
										</Button>
									</div>
								</>
							) : (
								<>
									<p className='text-muted mb-10'>
										Jeśli konto istnieje, kod autoryzacji został wysłany na{' '}
										<strong>{resetEmail}</strong>. Wpisz kod z wiadomości i
										nowe hasło.
									</p>

							{resetError && (
								<div className='mb-6 p-4 border border-error/30 text-error text-sm'>
									{resetError}
								</div>
							)}

							<form
								onSubmit={resetForm.handleSubmit(handleResetPassword)}
								className='space-y-6'
							>
								<Input
									id='token'
									label='Kod autoryzacji'
									type='text'
									placeholder='xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'
									error={resetForm.formState.errors.token?.message}
									{...resetForm.register('token')}
								/>
								<Input
									id='newPassword'
									label='Nowe hasło'
									type='password'
									placeholder='••••••••'
									error={resetForm.formState.errors.newPassword?.message}
									{...resetForm.register('newPassword')}
								/>
								<Input
									id='confirmPassword'
									label='Potwierdź hasło'
									type='password'
									placeholder='••••••••'
									error={resetForm.formState.errors.confirmPassword?.message}
									{...resetForm.register('confirmPassword')}
								/>
								<Button
									type='submit'
									size='lg'
									className='w-full'
									disabled={resetPending}
								>
									{resetPending ? (
										<>
											<Loader2 className='w-4 h-4 mr-2 animate-spin' />
											Zmieniam hasło...
										</>
									) : (
										'Zmień hasło'
									)}
								</Button>
							</form>
								</>
							)}

							<button
								type='button'
								onClick={handleBackToLogin}
								className='mt-6 text-xs uppercase tracking-widest text-muted hover:text-accent transition-colors'
							>
								Powrót do logowania
							</button>
						</>
					) : (
						<>
							<h1 className='font-serif text-4xl md:text-5xl font-light mb-2'>
								Witaj z powrotem
							</h1>
							<p className='text-muted mb-10'>
								Zaloguj się, aby zobaczyć swoje albumy ze zdjęciami.
							</p>

							{isError && (
								<div className='mb-6 p-4 border border-error/30 text-error text-sm'>
									Nieprawidłowy email lub hasło. Spróbuj ponownie.
								</div>
							)}

							<form onSubmit={handleSubmit(onSubmit)} className='space-y-6'>
								<Input
									id='email'
									label='Email'
									type='email'
									placeholder='anna@kowalska.pl'
									error={errors.email?.message}
									{...register('email')}
								/>
								<Input
									id='password'
									label='Hasło'
									type='password'
									placeholder='••••••••'
									error={errors.password?.message}
									{...register('password')}
								/>
								<Button
									type='submit'
									size='lg'
									className='w-full'
									disabled={isPending}
								>
									{isPending ? (
										<>
											<Loader2 className='w-4 h-4 mr-2 animate-spin' />
											Logowanie...
										</>
									) : (
										'Zaloguj się'
									)}
								</Button>
							</form>

							<button
								type='button'
								onClick={openReset}
								className='mt-6 text-xs uppercase tracking-widest text-muted hover:text-accent transition-colors'
							>
								Nie pamiętasz hasła?
							</button>
						</>
					)}
				</div>
			</div>
		</div>
	);
}
