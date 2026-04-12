import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2, Check } from 'lucide-react';
import { Input } from '@/shared/components/ui/Input';
import { Button } from '@/shared/components/ui/Button';
import { usePanelAuthStore } from '../../store/panelAuthStore';
import { changePassword, changeEmail, getMe } from '../../api/panelAuthApi';

const passwordSchema = z
	.object({
		currentPassword: z.string().min(1, 'Wymagane'),
		newPassword: z.string().min(8, 'Min. 8 znaków'),
		confirmPassword: z.string(),
	})
	.refine((d) => d.newPassword === d.confirmPassword, {
		message: 'Hasła muszą być takie same',
		path: ['confirmPassword'],
	});

const emailSchema = z.object({
	newEmail: z.string().email('Nieprawidłowy adres email'),
});

type PasswordData = z.infer<typeof passwordSchema>;
type EmailData = z.infer<typeof emailSchema>;

export default function AccountPage() {
	const { user, setUser } = usePanelAuthStore();

	const [pwdPending, setPwdPending] = useState(false);
	const [pwdSuccess, setPwdSuccess] = useState(false);
	const [pwdError, setPwdError] = useState('');
	const [emailPending, setEmailPending] = useState(false);
	const [emailSuccess, setEmailSuccess] = useState(false);
	const [emailError, setEmailError] = useState('');

	const pwdForm = useForm<PasswordData>({
		resolver: zodResolver(passwordSchema),
	});

	const emailForm = useForm<EmailData>({
		resolver: zodResolver(emailSchema),
	});

	const handleChangePassword = async (data: PasswordData) => {
		if (!user) return;
		setPwdPending(true);
		setPwdError('');
		setPwdSuccess(false);
		try {
			await changePassword(user.id, data.currentPassword, data.newPassword);
			setPwdSuccess(true);
			pwdForm.reset();
		} catch {
			setPwdError('Nie udało się zmienić hasła. Sprawdź aktualne hasło.');
		} finally {
			setPwdPending(false);
		}
	};

	const handleChangeEmail = async (data: EmailData) => {
		if (!user) return;
		setEmailPending(true);
		setEmailError('');
		setEmailSuccess(false);
		try {
			await changeEmail(user.id, data.newEmail);
			const updated = await getMe();
			setUser(updated);
			setEmailSuccess(true);
			emailForm.reset();
		} catch {
			setEmailError('Nie udało się zmienić adresu email.');
		} finally {
			setEmailPending(false);
		}
	};

	return (
		<div>
			<h2 className='font-serif text-4xl font-light mb-2'>Moje konto</h2>
			<p className='text-sm text-muted mb-8'>Zarządzaj swoim kontem i danymi</p>

			{/* Current info */}
			<div className='bg-surface border border-border p-6 mb-8'>
				<h3 className='font-medium mb-4'>Informacje</h3>
				<div className='space-y-2 text-sm'>
					<p>
						<span className='text-muted'>Imię:</span> {user?.name}
					</p>
					<p>
						<span className='text-muted'>Email:</span> {user?.email}
					</p>
					<p>
						<span className='text-muted'>Role:</span> {user?.roles.join(', ')}
					</p>
				</div>
			</div>

			<div className='grid gap-8 lg:grid-cols-2'>
				{/* Change password */}
				<div className='bg-surface border border-border p-6'>
					<h3 className='font-medium mb-4'>Zmień hasło</h3>

					{pwdSuccess && (
						<div className='mb-4 p-3 border border-accent/30 text-accent text-sm flex items-center gap-2'>
							<Check className='w-4 h-4' />
							Hasło zostało zmienione.
						</div>
					)}
					{pwdError && (
						<div className='mb-4 p-3 border border-error/30 text-error text-sm'>
							{pwdError}
						</div>
					)}

					<form
						onSubmit={pwdForm.handleSubmit(handleChangePassword)}
						className='space-y-4'
					>
						<Input
							id='currentPassword'
							label='Aktualne hasło'
							type='password'
							error={pwdForm.formState.errors.currentPassword?.message}
							{...pwdForm.register('currentPassword')}
						/>
						<Input
							id='newPassword'
							label='Nowe hasło'
							type='password'
							error={pwdForm.formState.errors.newPassword?.message}
							{...pwdForm.register('newPassword')}
						/>
						<Input
							id='confirmPassword'
							label='Potwierdź nowe hasło'
							type='password'
							error={pwdForm.formState.errors.confirmPassword?.message}
							{...pwdForm.register('confirmPassword')}
						/>
						<Button type='submit' disabled={pwdPending}>
							{pwdPending ? (
								<Loader2 className='w-4 h-4 mr-2 animate-spin' />
							) : null}
							Zmień hasło
						</Button>
					</form>
				</div>

				{/* Change email */}
				<div className='bg-surface border border-border p-6'>
					<h3 className='font-medium mb-4'>Zmień email</h3>

					{emailSuccess && (
						<div className='mb-4 p-3 border border-accent/30 text-accent text-sm flex items-center gap-2'>
							<Check className='w-4 h-4' />
							Adres email został zmieniony.
						</div>
					)}
					{emailError && (
						<div className='mb-4 p-3 border border-error/30 text-error text-sm'>
							{emailError}
						</div>
					)}

					<form
						onSubmit={emailForm.handleSubmit(handleChangeEmail)}
						className='space-y-4'
					>
						<Input
							id='newEmail'
							label='Nowy adres email'
							type='email'
							error={emailForm.formState.errors.newEmail?.message}
							{...emailForm.register('newEmail')}
						/>
						<Button type='submit' disabled={emailPending}>
							{emailPending ? (
								<Loader2 className='w-4 h-4 mr-2 animate-spin' />
							) : null}
							Zmień email
						</Button>
					</form>
				</div>
			</div>
		</div>
	);
}
