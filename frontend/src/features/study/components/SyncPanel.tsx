import { useState, type FormEvent } from 'react';
import type { SyncNotice, SyncStatus } from '../lib/useLearned';
import { syncUrl } from '../lib/syncCode';

interface SyncPanelProps {
	syncCode: string | null;
	syncStatus: SyncStatus;
	syncNotice: SyncNotice;
	enableSync: () => void;
	joinSync: (input: string) => boolean;
	disableSync: () => void;
}

const STATUS_LABEL: Record<SyncStatus, string> = {
	off: 'wyłączona',
	syncing: 'zapisywanie…',
	synced: 'zsynchronizowano',
	error: 'brak połączenia — zmiany są na tym urządzeniu i zostaną wysłane później',
};

const NOTICE_MESSAGE: Record<Exclude<SyncNotice, null>, string> = {
	joined: 'Połączono z synchronizacją — postęp pobrany z serwera.',
	'invalid-link': 'Link synchronizacji jest nieprawidłowy — został postęp z tego urządzenia.',
};

const actionClass = 'text-xs uppercase tracking-widest text-muted hover:text-accent transition-colors';

export function SyncPanel({
	syncCode,
	syncStatus,
	syncNotice,
	enableSync,
	joinSync,
	disableSync,
}: SyncPanelProps) {
	const [message, setMessage] = useState<string | null>(
		syncNotice ? NOTICE_MESSAGE[syncNotice] : null,
	);
	const [manualUrl, setManualUrl] = useState<string | null>(null);
	const [joinInput, setJoinInput] = useState('');

	const copyLink = async (code: string) => {
		const url = syncUrl(code, window.location.href);
		try {
			await navigator.clipboard.writeText(url);
			setManualUrl(null);
			setMessage('Skopiowano link synchronizacji — otwórz go na drugim urządzeniu.');
		} catch {
			setManualUrl(url);
			setMessage('Nie udało się skopiować automatycznie — skopiuj link poniżej.');
		}
	};

	const onJoin = (e: FormEvent) => {
		e.preventDefault();
		if (joinSync(joinInput)) {
			setJoinInput('');
			setManualUrl(null);
			setMessage('Połączono z synchronizacją — postęp pobrany z serwera.');
		} else {
			setMessage('To nie jest poprawny link ani kod synchronizacji.');
		}
	};

	return (
		<section
			aria-label='Synchronizacja postępu'
			className='mt-6 border border-border bg-surface p-4 sm:p-5 text-sm'
		>
			{syncCode ? (
				<>
					<p className='text-foreground'>
						Synchronizacja: <span className='text-accent'>{STATUS_LABEL[syncStatus]}</span>
					</p>
					<div className='mt-3 flex flex-wrap gap-x-6 gap-y-2'>
						<button type='button' onClick={() => copyLink(syncCode)} className={actionClass}>
							Skopiuj link synchronizacji
						</button>
						<button
							type='button'
							onClick={() => {
								disableSync();
								setManualUrl(null);
								setMessage('Synchronizacja wyłączona — postęp został na tym urządzeniu.');
							}}
							className={actionClass}
						>
							Wyłącz synchronizację
						</button>
					</div>
				</>
			) : (
				<>
					<p className='text-muted'>
						Postęp jest zapisany tylko na tym urządzeniu. Włącz synchronizację, żeby był taki sam
						na telefonie i laptopie.
					</p>
					<div className='mt-3 flex flex-wrap items-center gap-x-6 gap-y-3'>
						<button
							type='button'
							onClick={() => {
								enableSync();
								setMessage(
									'Synchronizacja włączona — skopiuj link i otwórz go na drugim urządzeniu.',
								);
							}}
							className={actionClass}
						>
							Włącz synchronizację
						</button>
						<form onSubmit={onJoin} className='flex flex-1 min-w-[16rem] gap-3'>
							<input
								value={joinInput}
								onChange={(e) => setJoinInput(e.target.value)}
								placeholder='albo wklej link / kod z drugiego urządzenia'
								aria-label='Link lub kod synchronizacji'
								className='flex-1 min-w-0 bg-transparent border-b border-border py-1 text-foreground placeholder:text-muted/60 focus:border-accent focus:outline-none'
							/>
							<button type='submit' className={actionClass}>
								Połącz
							</button>
						</form>
					</div>
				</>
			)}
			<div role='status' className='empty:hidden mt-3 text-accent'>
				{message}
			</div>
			{manualUrl && (
				<input
					readOnly
					value={manualUrl}
					aria-label='Link synchronizacji'
					onFocus={(e) => e.target.select()}
					className='mt-2 w-full bg-transparent border border-border px-3 py-2 text-foreground'
				/>
			)}
		</section>
	);
}
