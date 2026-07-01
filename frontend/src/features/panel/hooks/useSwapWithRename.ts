import { useState } from 'react';
import { toast } from '@/shared/store/toastStore';

export interface SwapFileRef {
	fileID: string;
	fileName: string;
}

export interface RenameEntry {
	fileID: string;
	originalName: string;
	newName: string;
}

interface SwapContext {
	sourceAlbumId: string;
	targetAlbumId: string;
	files: SwapFileRef[];
}

/** Proponuje nazwę wolną od kolizji: `foto.jpg` → `foto_1.jpg` (rosnąco). */
export function suggestNonCollidingName(
	fileName: string,
	taken: Set<string>,
): string {
	const dot = fileName.lastIndexOf('.');
	const base = dot > 0 ? fileName.slice(0, dot) : fileName;
	const ext = dot > 0 ? fileName.slice(dot) : '';
	let i = 1;
	let candidate = `${base}_${i}${ext}`;
	while (taken.has(candidate)) {
		i += 1;
		candidate = `${base}_${i}${ext}`;
	}
	return candidate;
}

/**
 * Orchestruje przenoszenie zdjęć z obsługą kolizji nazw:
 * pobiera SAME NAZWY z albumu docelowego (lekkie query), wykrywa kolizje,
 * a przy ich braku od razu robi swap. Gdy są — udostępnia edytowalny plan
 * zmiany nazw; po zatwierdzeniu zmienia nazwy (w źródle) i dopiero przenosi.
 */
export function useSwapWithRename(opts: {
	getFileNames: (albumId: string) => Promise<string[]>;
	rename: (vars: {
		albumId: string;
		fileId: string;
		newName: string;
	}) => Promise<unknown>;
	swap: (vars: {
		sourceAlbumId: string;
		targetAlbumId: string;
		fileIds: string[];
	}) => Promise<unknown>;
	onDone: () => void;
}) {
	const { getFileNames, rename, swap, onDone } = opts;

	const [isChecking, setIsChecking] = useState(false);
	const [isSubmitting, setIsSubmitting] = useState(false);
	const [renames, setRenames] = useState<RenameEntry[] | null>(null);
	const [ctx, setCtx] = useState<SwapContext | null>(null);

	async function runSwap(context: SwapContext) {
		await swap({
			sourceAlbumId: context.sourceAlbumId,
			targetAlbumId: context.targetAlbumId,
			fileIds: context.files.map((f) => f.fileID),
		});
	}

	async function start(context: SwapContext) {
		if (context.files.length === 0) return;
		setIsChecking(true);
		try {
			const targetNames = await getFileNames(context.targetAlbumId);
			const taken = new Set(targetNames);
			const collisions = context.files.filter((f) => taken.has(f.fileName));

			if (collisions.length === 0) {
				await runSwap(context);
				onDone();
				return;
			}

			// domyślne propozycje, unikające nazw w celu i między sobą
			const reserved = new Set(targetNames);
			const entries: RenameEntry[] = collisions.map((f) => {
				const suggested = suggestNonCollidingName(f.fileName, reserved);
				reserved.add(suggested);
				return {
					fileID: f.fileID,
					originalName: f.fileName,
					newName: suggested,
				};
			});
			setCtx(context);
			setRenames(entries);
		} catch {
			toast.error('Nie udało się sprawdzić nazw w albumie docelowym.');
		} finally {
			setIsChecking(false);
		}
	}

	function setNewName(fileID: string, value: string) {
		setRenames(
			(prev) =>
				prev?.map((r) => (r.fileID === fileID ? { ...r, newName: value } : r)) ??
				null,
		);
	}

	async function confirm() {
		if (!ctx || !renames) return;
		setIsSubmitting(true);
		try {
			for (const r of renames) {
				await rename({
					albumId: ctx.sourceAlbumId,
					fileId: r.fileID,
					newName: r.newName,
				});
			}
			await runSwap(ctx);
			setRenames(null);
			setCtx(null);
			onDone();
		} catch {
			// błąd pokazuje globalny handler mutacji (toast); zostawiamy dialog otwarty
		} finally {
			setIsSubmitting(false);
		}
	}

	function cancel() {
		setRenames(null);
		setCtx(null);
	}

	return {
		start,
		renames,
		setNewName,
		confirm,
		cancel,
		isChecking,
		isSubmitting,
	};
}
