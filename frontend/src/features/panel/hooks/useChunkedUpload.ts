import { useCallback, useRef, useState } from 'react';
import { toast } from '@/shared/store/toastStore';
import { getApiErrorMessage } from '@/lib/queryClient';

// Upload w paczkach: każda paczka to osobny request, a jego odpowiedź = pliki
// realnie zapisane na dysku VPS (osobna transakcja + commit po stronie backendu).
// Dzięki temu pasek postępu odzwierciedla NIE TYLKO wysyłkę bajtów, ale też zapis.
// Mniejsze requesty odciążają też słaby VPS (przetwarzanie jest synchroniczne,
// w pamięci — patrz B.26) i sprawiają, że błąd jednej paczki nie gubi wszystkiego
// (wcześniejsze paczki są już zapisane).
const MAX_FILES_PER_CHUNK = 5;
const MAX_BYTES_PER_CHUNK = 40 * 1024 * 1024;

// Podział paska: połowa na transfer bajtów, połowa na potwierdzony zapis na serwerze.
// 100% osiągane dopiero, gdy WSZYSTKIE bajty wysłane I wszystkie pliki zapisane.
const UPLOAD_WEIGHT = 0.5;
const SAVE_WEIGHT = 0.5;

export type UploadPhase = 'uploading' | 'saving' | 'done';

export interface UploadProgressState {
	/** 0–100, ważona kombinacja wysyłki bajtów i zapisu na serwerze. */
	percent: number;
	/** Pliki potwierdzone jako zapisane na dysku VPS. */
	savedCount: number;
	totalCount: number;
	phase: UploadPhase;
}

interface UseChunkedUploadParams {
	/** Wysyłka jednej paczki; `onProgress` = postęp bajtów tej paczki (0–100). */
	upload: (files: File[], onProgress: (percent: number) => void) => Promise<void>;
	/** Po całości (i po błędzie) — np. inwalidacja listy albumów, by pokazać zapisane pliki. */
	onComplete?: () => void;
}

/** Dzieli pliki na paczki: max liczba plików LUB max sumaryczny rozmiar (co pierwsze). */
function buildChunks(files: File[]): File[][] {
	const chunks: File[][] = [];
	let current: File[] = [];
	let currentBytes = 0;
	for (const file of files) {
		const wouldOverflow =
			current.length >= MAX_FILES_PER_CHUNK ||
			currentBytes + file.size > MAX_BYTES_PER_CHUNK;
		if (current.length > 0 && wouldOverflow) {
			chunks.push(current);
			current = [];
			currentBytes = 0;
		}
		current.push(file);
		currentBytes += file.size;
	}
	if (current.length > 0) chunks.push(current);
	return chunks;
}

/**
 * Sekwencyjny upload w paczkach ze stanem postępu dla `UploadProgress`.
 * `start` jest stabilne (refy) — bezpieczne w zależnościach `useCallback`.
 */
export function useChunkedUpload(params: UseChunkedUploadParams) {
	const [state, setState] = useState<UploadProgressState | null>(null);
	const paramsRef = useRef(params);
	paramsRef.current = params;
	const runningRef = useRef(false);
	const clearTimerRef = useRef<number | null>(null);

	const start = useCallback(async (files: File[]) => {
		if (runningRef.current || files.length === 0) return;
		runningRef.current = true;
		if (clearTimerRef.current !== null) {
			window.clearTimeout(clearTimerRef.current);
			clearTimerRef.current = null;
		}

		const total = files.length;
		const totalBytes = files.reduce((sum, f) => sum + f.size, 0) || 1;
		const chunks = buildChunks(files);
		let bytesDone = 0; // bajty z w pełni wysłanych paczek
		let savedFiles = 0; // pliki z potwierdzonych (zapisanych) paczek

		// Ważony postęp: udział wysyłki (bajty) + udział zapisu (potwierdzone pliki).
		const compute = (inflightBytes: number) => {
			const uploadFraction = (bytesDone + inflightBytes) / totalBytes;
			const saveFraction = savedFiles / total;
			return (uploadFraction * UPLOAD_WEIGHT + saveFraction * SAVE_WEIGHT) * 100;
		};

		setState({ percent: 0, savedCount: 0, totalCount: total, phase: 'uploading' });

		try {
			for (const chunkFiles of chunks) {
				const chunkBytes = chunkFiles.reduce((sum, f) => sum + f.size, 0);
				await paramsRef.current.upload(chunkFiles, (bytePct) => {
					const inflight = (chunkBytes * bytePct) / 100;
					setState({
						// <100 aż do potwierdzenia zapisu ostatniej paczki
						percent: Math.min(99, compute(inflight)),
						savedCount: savedFiles,
						totalCount: total,
						phase: bytePct >= 100 ? 'saving' : 'uploading',
					});
				});
				// Odpowiedź wróciła = ta paczka zapisana na dysku VPS.
				bytesDone += chunkBytes;
				savedFiles += chunkFiles.length;
				const done = savedFiles >= total;
				setState({
					percent: done ? 100 : Math.min(99, compute(0)),
					savedCount: savedFiles,
					totalCount: total,
					phase: done ? 'done' : 'saving',
				});
			}
			paramsRef.current.onComplete?.();
		} catch (error) {
			// Część paczek mogła się już zapisać — odśwież, by je pokazać, i zgłoś błąd.
			paramsRef.current.onComplete?.();
			toast.error(getApiErrorMessage(error));
		} finally {
			runningRef.current = false;
			clearTimerRef.current = window.setTimeout(() => {
				setState(null);
				clearTimerRef.current = null;
			}, 800);
		}
	}, []);

	return { state, start };
}
