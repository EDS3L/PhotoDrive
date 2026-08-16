import { useCallback, useRef, useState } from 'react';
import { toast } from '@/shared/store/toastStore';
import { getApiErrorMessage } from '@/lib/queryClient';

const MAX_FILES_PER_CHUNK = 20;
const MAX_BYTES_PER_CHUNK = 100 * 1024 * 1024;

const UPLOAD_WEIGHT = 0.5;
const SAVE_WEIGHT = 0.5;

export type UploadPhase = 'uploading' | 'saving' | 'done';

export interface UploadProgressState {
	percent: number;
	savedCount: number;
	totalCount: number;
	phase: UploadPhase;
}

interface UseChunkedUploadParams {
	upload: (files: File[], onProgress: (percent: number) => void) => Promise<void>;
	onComplete?: () => void;
}

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
		let bytesDone = 0;
		let savedFiles = 0;

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
						percent: Math.min(99, compute(inflight)),
						savedCount: savedFiles,
						totalCount: total,
						phase: bytePct >= 100 ? 'saving' : 'uploading',
					});
				});
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
