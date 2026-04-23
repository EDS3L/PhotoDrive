import { useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { useCreatePasswordToken, useRemindPassword } from '@/hooks/use-auth';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';

export function ResetPasswordPage() {
	const [step, setStep] = useState<'email' | 'reset' | 'done'>('email');
	const [email, setEmail] = useState('');
	const [token, setToken] = useState('');
	const [newPassword, setNewPassword] = useState('');
	const [error, setError] = useState('');

	const createToken = useCreatePasswordToken();
	const remindPassword = useRemindPassword();

	const handleRequestToken = async (e: FormEvent) => {
		e.preventDefault();
		setError('');
		try {
			await createToken.mutateAsync(email);
			setStep('reset');
		} catch {
			setError('Nie udało się wysłać tokena. Sprawdź adres email.');
		}
	};

	const handleResetPassword = async (e: FormEvent) => {
		e.preventDefault();
		setError('');
		try {
			await remindPassword.mutateAsync({ email, token, newPassword });
			setStep('done');
		} catch {
			setError('Nieprawidłowy token lub hasło nie spełnia wymagań.');
		}
	};

	return (
		<div className='min-h-screen w-full flex items-center justify-center px-6'>
			<div className='w-full max-w-md'>
				<div className='mb-12'>
					<p className='font-display italic text-3xl text-on-surface'>
						PhotoDrive
					</p>
					<p className='label text-[10px] text-on-surface-variant mt-2'>
						RESETOWANIE HASŁA
					</p>
				</div>

				{step === 'email' && (
					<form onSubmit={handleRequestToken} className='flex flex-col gap-6'>
						<p className='text-on-surface-variant text-sm'>
							Podaj adres email powiązany z Twoim kontem. Wyślemy Ci token do
							zresetowania hasła.
						</p>
						<Input
							label='Email'
							type='email'
							value={email}
							onChange={(e) => setEmail(e.target.value)}
							required
							autoComplete='email'
						/>
						{error && <p className='text-error text-sm'>{error}</p>}
						<Button
							type='submit'
							loading={createToken.isPending}
							className='w-full'
						>
							Wyślij token
						</Button>
					</form>
				)}

				{step === 'reset' && (
					<form onSubmit={handleResetPassword} className='flex flex-col gap-6'>
						<p className='text-on-surface-variant text-sm'>
							Token został wysłany na{' '}
							<span className='text-on-surface'>{email}</span>. Wpisz go poniżej
							wraz z nowym hasłem.
						</p>
						<Input
							label='Token'
							type='text'
							value={token}
							onChange={(e) => setToken(e.target.value)}
							required
						/>
						<Input
							label='Nowe hasło'
							type='password'
							value={newPassword}
							onChange={(e) => setNewPassword(e.target.value)}
							required
							autoComplete='new-password'
							helperText='Min. 8 znaków, wielka litera, cyfra, znak specjalny'
						/>
						{error && <p className='text-error text-sm'>{error}</p>}
						<Button
							type='submit'
							loading={remindPassword.isPending}
							className='w-full'
						>
							Zmień hasło
						</Button>
					</form>
				)}

				{step === 'done' && (
					<div className='text-center'>
						<span className='material-symbols-outlined text-success text-5xl mb-4'>
							check_circle
						</span>
						<h2 className='font-display text-3xl text-on-surface mb-4'>
							Hasło zmienione
						</h2>
						<p className='text-on-surface-variant text-sm mb-8'>
							Twoje hasło zostało pomyślnie zmienione. Możesz teraz się
							zalogować.
						</p>
						<Link to='/login'>
							<Button>Przejdź do logowania</Button>
						</Link>
					</div>
				)}
			</div>
		</div>
	);
}
