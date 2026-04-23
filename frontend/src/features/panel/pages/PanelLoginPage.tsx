import { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2 } from 'lucide-react';
import { Input } from '@/shared/components/ui/Input';
import { Button } from '@/shared/components/ui/Button';
import { usePanelLogin } from '../hooks/usePanelAuth';
import { usePanelAuthStore } from '../store/panelAuthStore';
import { placeholder } from '@/lib/placeholder';

const loginSchema = z.object({
	email: z.string().email('Nieprawidłowy adres email'),
	password: z.string().min(1, 'Hasło jest wymagane'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function PanelLoginPage() {
	const navigate = useNavigate();
	const { isAuthenticated, role } = usePanelAuthStore();
	const { mutate, isPending, isError, error } = usePanelLogin();

	const {
		register,
		handleSubmit,
		formState: { errors },
	} = useForm<LoginFormData>({
		resolver: zodResolver(loginSchema),
	});

	useEffect(() => {
		if (isAuthenticated && role) {
			const target = role === 'ADMIN' ? '/admin' : '/photographer';
			navigate(target, { replace: true });
		}
	}, [isAuthenticated, role, navigate]);

	const isAccessDenied = isError && error?.message === 'ACCESS_DENIED';

	const onSubmit = (data: LoginFormData) => {
		mutate(data);
	};

	return (
		<div className='min-h-screen flex'>
			{/* Left — photo panel */}
			<div className='hidden lg:flex lg:w-1/2 relative items-end p-12'>
				<img
					src={placeholder(960, 1080, 'Panel — studio')}
					alt=''
					className='absolute inset-0 w-full h-full object-cover'
					aria-hidden='true'
				/>
				<div className='absolute inset-0 bg-black/50' />
				<div className='relative z-10'>
					<span className='text-[10px] uppercase tracking-[0.3em] px-2 py-1 border border-accent/40 text-accent'>
						Panel
					</span>
					<p className='font-serif text-3xl italic leading-snug mt-4'>
						Za kulisami
						<br />
						każdego kadru
					</p>
				</div>
			</div>

			{/* Right — form */}
			<div className='flex-1 flex items-center justify-center px-6 py-12 lg:px-16 bg-background'>
				<div className='w-full max-w-md'>
					<span className='text-[10px] uppercase tracking-[0.3em] px-2 py-1 border border-accent/40 text-accent'>
						Panel zarządzania
					</span>
					<p className='text-muted mb-10'>
						Panel dostępny dla administratorów i fotografów.
					</p>

					{isError && !isAccessDenied && (
						<div className='mb-6 p-4 border border-error/30 text-error text-sm'>
							Nieprawidłowy email lub hasło. Spróbuj ponownie.
						</div>
					)}

					{isAccessDenied && (
						<div className='mb-6 p-4 border border-error/30 text-error text-sm'>
							Brak uprawnień do panelu. Dostęp tylko dla administratorów i
							fotografów.
						</div>
					)}

					<form onSubmit={handleSubmit(onSubmit)} className='space-y-6'>
						<Input
							id='panel-email'
							label='Email'
							type='email'
							placeholder='admin@photodrive.dev'
							autoComplete='email'
							error={errors.email?.message}
							{...register('email')}
						/>
						<Input
							id='panel-password'
							label='Hasło'
							type='password'
							placeholder='••••••••'
							autoComplete='current-password'
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
						<Link to='/' className='block mt-3'>
							<Button
								variant='ghost'
								size='lg'
								className='w-full hover:cursor-pointer'
							>
								Powrót do strony głównej
							</Button>
						</Link>
					</form>
				</div>
			</div>
		</div>
	);
}
