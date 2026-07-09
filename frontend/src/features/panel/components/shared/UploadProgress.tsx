import { useEffect } from 'react';
import { Loader2, AlertTriangle, Check } from 'lucide-react';
import type { UploadProgressState } from '../../hooks/useChunkedUpload';

interface UploadProgressProps {
	state: UploadProgressState | null;
}

/**
 * Pasek postępu uploadu + ostrzeżenie „nie zamykaj karty".
 * Pasek jest DWUFAZOWY: część wysyłki bajtów + część potwierdzonego zapisu na VPS
 * (patrz {@link useChunkedUpload}) — 100% dopiero, gdy wszystkie pliki zapisane.
 * Dopóki upload trwa (faza inna niż `done`) blokujemy zamknięcie/odświeżenie karty
 * natywnym oknem przeglądarki (beforeunload) — przerwany request nic nie zapisze.
 */
export function UploadProgress({ state }: UploadProgressProps) {
	const active = state !== null && state.phase !== 'done';

	useEffect(() => {
		if (!active) return;
		const handler = (e: BeforeUnloadEvent) => {
			e.preventDefault();
			e.returnValue = '';
		};
		window.addEventListener('beforeunload', handler);
		return () => window.removeEventListener('beforeunload', handler);
	}, [active]);

	if (state === null) return null;

	const pct = Math.min(100, Math.max(0, Math.round(state.percent)));
	const done = state.phase === 'done';
	const saving = state.phase === 'saving';
	const label = done
		? 'Gotowe — wszystkie zdjęcia zapisane'
		: saving
			? 'Zapisywanie na serwerze…'
			: 'Przesyłanie zdjęć…';

	return (
		<div
			className='mb-4 p-4 bg-surface border border-border'
			role='status'
			aria-live='polite'
		>
			<div className='flex items-center gap-3'>
				{done ? (
					<Check className='w-5 h-5 text-accent shrink-0' />
				) : (
					<Loader2 className='w-5 h-5 text-accent animate-spin shrink-0' />
				)}
				<span className='text-sm'>{label}</span>
				<span className='ml-auto text-sm tabular-nums text-muted'>{pct}%</span>
			</div>
			<div className='mt-3 h-1.5 w-full bg-border overflow-hidden'>
				<div
					className={`h-full bg-accent transition-[width] duration-200 ease-out ${
						saving ? 'animate-pulse' : ''
					}`}
					style={{ width: `${pct}%` }}
				/>
			</div>
			<p className='mt-2 text-xs text-muted tabular-nums'>
				Zapisano {state.savedCount} z {state.totalCount} zdjęć
			</p>
			{!done && (
				<p className='mt-2 flex items-center gap-1.5 text-xs text-yellow-400'>
					<AlertTriangle className='w-3.5 h-3.5 shrink-0' />
					Nie zamykaj tej karty — przerwanie anuluje przesyłanie, a niezapisane
					zdjęcia nie zostaną dodane.
				</p>
			)}
		</div>
	);
}
