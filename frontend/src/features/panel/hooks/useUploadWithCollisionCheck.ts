import { useCallback, useState } from 'react';
import { toast } from '@/shared/store/toastStore';
import { useChunkedUpload } from './useChunkedUpload';
import { suggestNonCollidingName } from './useSwapWithRename';

export type CollisionAction = 'rename' | 'skip';

export interface CollisionEntry {
	id: string;
	file: File;
	originalName: string;
	action: CollisionAction;
	newName: string;
}

interface UseUploadWithCollisionCheckParams {
	getFileNames: (albumId: string) => Promise<string[]>;
	upload: (files: File[], onProgress: (percent: number) => void) => Promise<void>;
	onComplete?: () => void;
}

export function useUploadWithCollisionCheck({
	getFileNames,
	upload,
	onComplete,
}: UseUploadWithCollisionCheckParams) {
	const { state, start } = useChunkedUpload({ upload, onComplete });
	const [collisions, setCollisions] = useState<CollisionEntry[] | null>(null);
	const [safeFiles, setSafeFiles] = useState<File[]>([]);
	const [isChecking, setIsChecking] = useState(false);

	const begin = useCallback(
		async (albumId: string, files: File[]) => {
			if (files.length === 0) return;
			setIsChecking(true);
			try {
				const existing = await getFileNames(albumId);
				const taken = new Set(existing);
				const colliding = files.filter((f) => taken.has(f.name));

				if (colliding.length === 0) {
					start(files);
					return;
				}

				const reserved = new Set(existing);
				const entries: CollisionEntry[] = colliding.map((file, index) => {
					const suggested = suggestNonCollidingName(file.name, reserved);
					reserved.add(suggested);
					return {
						id: `${file.name}#${index}`,
						file,
						originalName: file.name,
						action: 'rename',
						newName: suggested,
					};
				});
				setSafeFiles(files.filter((f) => !taken.has(f.name)));
				setCollisions(entries);
			} catch {
				start(files);
			} finally {
				setIsChecking(false);
			}
		},
		[getFileNames, start],
	);

	const setAction = useCallback((id: string, action: CollisionAction) => {
		setCollisions(
			(prev) =>
				prev?.map((e) => (e.id === id ? { ...e, action } : e)) ?? null,
		);
	}, []);

	const setNewName = useCallback((id: string, newName: string) => {
		setCollisions(
			(prev) =>
				prev?.map((e) => (e.id === id ? { ...e, newName } : e)) ?? null,
		);
	}, []);

	const resolve = useCallback(() => {
		if (!collisions) return;
		const renamed = collisions
			.filter((e) => e.action === 'rename')
			.map(
				(e) =>
					new File([e.file], e.newName.trim(), {
						type: e.file.type,
						lastModified: e.file.lastModified,
					}),
			);
		const finalFiles = [...safeFiles, ...renamed];
		setCollisions(null);
		setSafeFiles([]);
		if (finalFiles.length === 0) {
			toast.info('Wszystkie pliki pominięto — nic do wgrania.');
			return;
		}
		start(finalFiles);
	}, [collisions, safeFiles, start]);

	const cancel = useCallback(() => {
		setCollisions(null);
		setSafeFiles([]);
	}, []);

	return {
		state,
		collisions,
		isChecking,
		begin,
		setAction,
		setNewName,
		resolve,
		cancel,
	};
}
