import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router';
import { useLogin } from '@/hooks/use-auth';
import { useAuthStore } from '@/lib/stores/auth-store';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { api } from '@/lib/api';
import type { UserDto } from '@/types/user';
import { normalizeUser } from '@/types/user';

export function StaffLoginPage() {
	const [email, setEmail] = useState('');
	const [password, setPassword] = useState('');
	const [error, setError] = useState('');
	const login = useLogin();
	const navigate = useNavigate();
	const setUser = useAuthStore((s) => s.setUser);

	const handleSubmit = async (e: FormEvent) => {
		e.preventDefault();
		setError('');
		try {
			const res = await login.mutateAsync({ email, password });

			// Fetch current user info
			const usersRes = await api.get<UserDto[]>('/user/all');
			const users = usersRes.data.map(normalizeUser);
			const me = users.find((u) => u.email === email);
			if (me) {
				setUser(me);
				if (res.changePasswordOnNextLogin) {
					// Will handle with modal in dashboard
				}
				// Redirect based on role
				if (me.roles.includes('ADMIN')) navigate('/admin/dashboard');
				else if (me.roles.includes('PHOTOGRAPHER'))
					navigate('/fotograf/dashboard');
				else navigate('/klient/dashboard');
			}
		} catch {
			setError('Nieprawidłowy email lub hasło');
		}
	};

	return (
		<div className='min-h-screen w-full flex'>
			{/* Left: Hero image */}
			<div className='hidden lg:block lg:w-1/2 relative overflow-hidden'>
				<div className='absolute inset-0 bg-surface-dim' />
				<div className='absolute inset-0 bg-gradient-to-r from-transparent to-background' />
				<div className='absolute bottom-12 left-12 right-12'>
					<p className='font-display text-6xl text-on-surface/80 leading-[0.9]'>
						Witaj
						<br />
						<span className='italic text-primary'>ponownie.</span>
					</p>
				</div>
			</div>

			{/* Right: Login form */}
			<div className='w-full lg:w-1/2 flex items-center justify-center px-6 py-12'>
				<div className='w-full max-w-md'>
					{/* Brand */}
					<div className='mb-12'>
						<p className='font-display italic text-3xl text-on-surface'>
							PhotoDrive
						</p>
						<p className='label text-[10px] text-on-surface-variant mt-2'>
							PANEL ADMINISTRACYJNY
						</p>
					</div>

					<h2 className='font-display text-4xl text-on-surface mb-8'>
						Zaloguj się
					</h2>

					<form onSubmit={handleSubmit} className='flex flex-col gap-6'>
						<Input
							label='Email'
							type='email'
							value={email}
							onChange={(e) => setEmail(e.target.value)}
							placeholder='twoj@email.pl'
							required
							autoComplete='email'
						/>
						<Input
							label='Hasło'
							type='password'
							value={password}
							onChange={(e) => setPassword(e.target.value)}
							placeholder='••••••••'
							required
							autoComplete='current-password'
						/>

						{error && <p className='text-error text-sm'>{error}</p>}

						<Button
							type='submit'
							loading={login.isPending}
							className='w-full mt-2'
						>
							Zaloguj się
						</Button>

						<Link
							to='/reset-haslo'
							className='text-on-surface-variant text-xs text-center hover:text-primary transition-colors'
						>
							Zapomniałeś hasła?
						</Link>
					</form>
				</div>
			</div>
		</div>
	);
}
