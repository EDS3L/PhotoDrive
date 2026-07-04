import { useState, type FormEvent } from 'react';
import { Loader2, ShieldAlert } from 'lucide-react';
import { Input } from './ui/Input';
import { Button } from './ui/Button';
import { getApiErrorMessage } from '@/lib/queryClient';

interface ForcePasswordChangeProps {
	userId: string;
	/** Wstrzykiwane przez strefę (panel/klient), by komponent nie był zależny od konkretnego modułu API. */
	changePassword: (
		userId: string,
		currentPassword: string,
		newPassword: string,
	) => Promise<void>;
	/** Wywoływane po udanej zmianie — strefa odświeża `/user/me` / czyści flagę i wpuszcza dalej. */
	onDone: () => void | Promise<void>;
	/**
	 * Hasło użyte przy logowaniu (świeże logowanie) — jeśli podane, nie prosimy
	 * użytkownika o nie ponownie. Przy re-hydracji sesji po F5 nie mamy go w pamięci,
	 * więc wtedy pole „hasło startowe" jest pokazywane.
	 */
	presetCurrentPassword?: string;
}

/**
 * Obowiązkowy, pełnoekranowy ekran zmiany hasła startowego. Renderowany dopóki
 * `/user/me` zwraca `changePasswordOnNextLogin=true` — użytkownik nie zrobi nic
 * innego, dopóki nie ustawi własnego hasła (bramka przeżywa odświeżenie strony,
 * bo flaga siedzi w bazie).
 */
export function ForcePasswordChange({
	userId,
	changePassword,
	onDone,
	presetCurrentPassword,
}: ForcePasswordChangeProps) {
	const [manualCurrent, setManualCurrent] = useState('');
	const [next, setNext] = useState('');
	const [confirm, setConfirm] = useState('');
	const [error, setError] = useState<string | null>(null);
	const [submitting, setSubmitting] = useState(false);

	// Jeśli mamy hasło z logowania — używamy go i nie prosimy o nie ponownie.
	const currentPassword = presetCurrentPassword ?? manualCurrent;

	const handleSubmit = async (e: FormEvent) => {
		e.preventDefault();
		setError(null);
		if (next.length < 8) {
			setError('Nowe hasło musi mieć min. 8 znaków.');
			return;
		}
		if (next !== confirm) {
			setError('Hasła nie są takie same.');
			return;
		}
		setSubmitting(true);
		try {
			await changePassword(userId, currentPassword, next);
			await onDone();
		} catch (err) {
			setError(getApiErrorMessage(err));
		} finally {
			setSubmitting(false);
		}
	};

	return (
		<div className='min-h-screen bg-background flex items-center justify-center px-6 py-12'>
			<div className='w-full max-w-md'>
				<div className='flex items-center gap-2 mb-2 text-accent'>
					<ShieldAlert className='w-5 h-5' />
					<p className='text-xs uppercase tracking-[0.3em]'>Wymagane działanie</p>
				</div>
				<h1 className='font-serif text-3xl md:text-4xl font-light mb-2'>
					Ustaw nowe hasło
				</h1>
				<p className='text-muted mb-8'>
					Przy pierwszym logowaniu musisz zmienić hasło startowe na własne.
					Dopóki tego nie zrobisz, dostęp do aplikacji pozostaje zablokowany.
				</p>

				{error && (
					<div className='mb-6 p-4 border border-error/30 text-error text-sm'>
						{error}
					</div>
				)}

				<form onSubmit={handleSubmit} className='space-y-6'>
					{!presetCurrentPassword && (
						<Input
							id='current-password'
							label='Hasło startowe (z maila)'
							type='password'
							placeholder='••••••••'
							value={manualCurrent}
							onChange={(e) => setManualCurrent(e.target.value)}
						/>
					)}
					<Input
						id='new-password'
						label='Nowe hasło'
						type='password'
						placeholder='Min. 8 znaków'
						value={next}
						onChange={(e) => setNext(e.target.value)}
					/>
					<Input
						id='confirm-password'
						label='Potwierdź nowe hasło'
						type='password'
						placeholder='Min. 8 znaków'
						value={confirm}
						onChange={(e) => setConfirm(e.target.value)}
					/>
					<Button
						type='submit'
						size='lg'
						className='w-full'
						disabled={submitting || !currentPassword || !next || !confirm}
					>
						{submitting ? (
							<>
								<Loader2 className='w-4 h-4 mr-2 animate-spin' />
								Zapisywanie...
							</>
						) : (
							'Zmień hasło i kontynuuj'
						)}
					</Button>
				</form>
			</div>
		</div>
	);
}
