import { useCallback, useEffect, useRef, useState } from 'react';
import { getStudyProgress, saveStudyProgress } from '@/lib/publicApi';
import { generateSyncCode, parseSyncInput, readSyncCodeFromHash } from './syncCode';

export const LEARNED_STORAGE_KEY = 'photodrive.study.learned';
export const SYNC_CODE_STORAGE_KEY = 'photodrive.study.syncCode';
export const PUSH_DELAY_MS = 400;

export type SyncStatus = 'off' | 'syncing' | 'synced' | 'error';
export type SyncNotice = 'joined' | 'invalid-link' | null;

function readStorage(key: string): string | null {
	try {
		return localStorage.getItem(key);
	} catch {
		return null;
	}
}

function writeStorage(key: string, value: string | null) {
	try {
		if (value === null) localStorage.removeItem(key);
		else localStorage.setItem(key, value);
	} catch {
		return;
	}
}

function readLearned(): Set<number> {
	try {
		const parsed: unknown = JSON.parse(readStorage(LEARNED_STORAGE_KEY) ?? '[]');
		return new Set(Array.isArray(parsed) ? parsed.filter((n) => Number.isInteger(n)) : []);
	} catch {
		return new Set();
	}
}

function initialState() {
	const fromLink = readSyncCodeFromHash(window.location.hash);
	const stored = readStorage(SYNC_CODE_STORAGE_KEY);
	const code = fromLink ?? (stored && parseSyncInput(stored));
	if (fromLink) writeStorage(SYNC_CODE_STORAGE_KEY, fromLink);
	const notice: SyncNotice = fromLink ? 'joined' : fromLink === null ? 'invalid-link' : null;
	return { learned: readLearned(), code: code || null, notice, hadLink: fromLink !== undefined };
}

export function useLearned() {
	const [initial] = useState(initialState);
	const [learned, setLearned] = useState<Set<number>>(initial.learned);
	const [syncCode, setSyncCode] = useState<string | null>(initial.code);
	const [syncStatus, setSyncStatus] = useState<SyncStatus>(initial.code ? 'syncing' : 'off');

	const learnedRef = useRef(learned);
	const codeRef = useRef(syncCode);
	const versionRef = useRef(0);
	const dirtyRef = useRef(false);
	const pullingRef = useRef(false);
	const timerRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

	const applyLearned = (next: Set<number>) => {
		learnedRef.current = next;
		setLearned(next);
	};

	const push = useCallback(async () => {
		const code = codeRef.current;
		if (!code) return;
		const version = versionRef.current;
		setSyncStatus('syncing');
		try {
			await saveStudyProgress(code, [...learnedRef.current].sort((a, b) => a - b));
			if (codeRef.current !== code) return;
			if (versionRef.current === version) {
				dirtyRef.current = false;
				setSyncStatus('synced');
			}
		} catch {
			if (codeRef.current === code) setSyncStatus('error');
		}
	}, []);

	const pull = useCallback(async () => {
		const code = codeRef.current;
		if (!code || pullingRef.current) return;
		if (dirtyRef.current) {
			await push();
			return;
		}
		pullingRef.current = true;
		const version = versionRef.current;
		setSyncStatus('syncing');
		try {
			const remote = await getStudyProgress(code);
			if (codeRef.current !== code || versionRef.current !== version) return;
			if (remote.updatedAt === null) {
				dirtyRef.current = true;
				await push();
			} else {
				learnedRef.current = new Set(remote.learned);
				setLearned(learnedRef.current);
				setSyncStatus('synced');
			}
		} catch {
			if (codeRef.current === code) setSyncStatus('error');
		} finally {
			pullingRef.current = false;
		}
	}, [push]);

	useEffect(() => writeStorage(LEARNED_STORAGE_KEY, JSON.stringify([...learned])), [learned]);

	useEffect(() => {
		if (initial.hadLink) {
			const { pathname, search } = window.location;
			window.history.replaceState(window.history.state, '', pathname + search);
		}
	}, [initial.hadLink]);

	useEffect(() => {
		codeRef.current = syncCode;
		if (syncCode) void pull();
	}, [syncCode, pull]);

	useEffect(() => {
		const onReturn = () => {
			if (document.visibilityState === 'visible') void pull();
		};
		document.addEventListener('visibilitychange', onReturn);
		window.addEventListener('focus', onReturn);
		return () => {
			document.removeEventListener('visibilitychange', onReturn);
			window.removeEventListener('focus', onReturn);
			clearTimeout(timerRef.current);
		};
	}, [pull]);

	const toggleLearned = (n: number) => {
		const next = new Set(learnedRef.current);
		if (next.has(n)) next.delete(n);
		else next.add(n);
		applyLearned(next);
		versionRef.current++;
		if (!codeRef.current) return;
		dirtyRef.current = true;
		clearTimeout(timerRef.current);
		timerRef.current = setTimeout(() => void push(), PUSH_DELAY_MS);
	};

	const startSync = (code: string, uploadLocal: boolean) => {
		clearTimeout(timerRef.current);
		writeStorage(SYNC_CODE_STORAGE_KEY, code);
		dirtyRef.current = uploadLocal;
		codeRef.current = code;
		setSyncCode(code);
	};

	const enableSync = () => startSync(generateSyncCode(), true);

	const joinSync = (input: string): boolean => {
		const code = parseSyncInput(input);
		if (!code) return false;
		startSync(code, false);
		return true;
	};

	const disableSync = () => {
		clearTimeout(timerRef.current);
		writeStorage(SYNC_CODE_STORAGE_KEY, null);
		dirtyRef.current = false;
		codeRef.current = null;
		setSyncCode(null);
		setSyncStatus('off');
	};

	return {
		learned,
		toggleLearned,
		syncCode,
		syncStatus,
		syncNotice: initial.notice,
		enableSync,
		joinSync,
		disableSync,
	};
}
