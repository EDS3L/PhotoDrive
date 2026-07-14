import { describe, it, expect, vi, beforeEach } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import { useUploadWithCollisionCheck } from './useUploadWithCollisionCheck';
import { useToastStore } from '@/shared/store/toastStore';

function jpg(name: string): File {
	return new File(['x'], name, { type: 'image/jpeg' });
}

/** Names of the files handed to the upload layer, flattened across chunks. */
function uploadedNames(upload: ReturnType<typeof vi.fn>): string[] {
	return upload.mock.calls.flatMap((call) => (call[0] as File[]).map((f) => f.name));
}

/**
 * Backend przy kolizji nazw i tak zapisze plik pod inną nazwą — cicho. Ten hook
 * istnieje po to, żeby fotograf DOWIEDZIAŁ SIĘ o kolizji i sam zdecydował:
 * zmienić nazwę czy pominąć plik. Testy pilnują, że decyzja jest respektowana.
 */
describe('useUploadWithCollisionCheck', () => {
	function setup(existing: string[] | Error) {
		const upload = vi.fn().mockResolvedValue(undefined);
		const getFileNames = vi.fn(async () => {
			if (existing instanceof Error) throw existing;
			return existing;
		});
		const onComplete = vi.fn();
		const hook = renderHook(() =>
			useUploadWithCollisionCheck({ getFileNames, upload, onComplete }),
		);
		return { ...hook, upload, getFileNames, onComplete };
	}

	beforeEach(() => {
		useToastStore.setState({ toasts: [] });
	});

	it('uploads immediately when no name is taken in the album', async () => {
		// Given - the album holds unrelated photos
		const { result, upload } = setup(['stare.jpg']);

		// When
		await act(async () => {
			await result.current.begin('album-1', [jpg('nowe.jpg')]);
		});

		// Then - no dialog gets in the photographer's way
		expect(result.current.collisions).toBeNull();
		await waitFor(() => expect(uploadedNames(upload)).toEqual(['nowe.jpg']));
	});

	it('holds the whole upload back when at least one name collides', async () => {
		// Given - one of the two photos already exists in the album
		const { result, upload } = setup(['foto.jpg']);

		// When
		await act(async () => {
			await result.current.begin('album-1', [jpg('foto.jpg'), jpg('inne.jpg')]);
		});

		// Then - nothing is sent before the photographer decides
		expect(upload).not.toHaveBeenCalled();
		expect(result.current.collisions).toHaveLength(1);
		expect(result.current.collisions?.[0]).toMatchObject({
			originalName: 'foto.jpg',
			action: 'rename',
			newName: 'foto_1.jpg',
		});
	});

	it('proposes names free both in the album and among the other proposals', async () => {
		// Given - the album already holds the obvious replacement name as well
		const { result } = setup(['foto.jpg', 'foto_1.jpg']);

		// When
		await act(async () => {
			await result.current.begin('album-1', [jpg('foto.jpg')]);
		});

		// Then - the proposal skips the taken counter instead of colliding again
		expect(result.current.collisions?.[0].newName).toBe('foto_2.jpg');
	});

	it('sends the renamed file under its new name, together with the non-colliding ones', async () => {
		// Given - a pending decision on the colliding photo
		const { result, upload } = setup(['foto.jpg']);
		await act(async () => {
			await result.current.begin('album-1', [jpg('foto.jpg'), jpg('inne.jpg')]);
		});

		// When - the photographer accepts the rename
		await act(async () => {
			result.current.resolve();
		});

		// Then - the photo travels under the new name, the original is not overwritten
		await waitFor(() =>
			expect(uploadedNames(upload).sort()).toEqual(['foto_1.jpg', 'inne.jpg']),
		);
		expect(result.current.collisions).toBeNull();
	});

	it('respects a name typed by the photographer over the proposed one', async () => {
		// Given
		const { result, upload } = setup(['foto.jpg']);
		await act(async () => {
			await result.current.begin('album-1', [jpg('foto.jpg')]);
		});
		const id = result.current.collisions![0].id;

		// When
		act(() => result.current.setNewName(id, 'plaza-zachod.jpg'));
		await act(async () => {
			result.current.resolve();
		});

		// Then
		await waitFor(() => expect(uploadedNames(upload)).toEqual(['plaza-zachod.jpg']));
	});

	it('skips a photo marked as "skip" and uploads only the rest', async () => {
		// Given - two collisions
		const { result, upload } = setup(['foto.jpg', 'plaza.jpg']);
		await act(async () => {
			await result.current.begin('album-1', [jpg('foto.jpg'), jpg('plaza.jpg')]);
		});
		const skipped = result.current.collisions!.find((c) => c.originalName === 'foto.jpg')!;

		// When - one of them is dropped
		act(() => result.current.setAction(skipped.id, 'skip'));
		await act(async () => {
			result.current.resolve();
		});

		// Then - only the renamed one is sent
		await waitFor(() => expect(uploadedNames(upload)).toEqual(['plaza_1.jpg']));
	});

	it('sends nothing and says so when every file was skipped', async () => {
		// Given
		const { result, upload } = setup(['foto.jpg']);
		await act(async () => {
			await result.current.begin('album-1', [jpg('foto.jpg')]);
		});
		const only = result.current.collisions![0];

		// When
		act(() => result.current.setAction(only.id, 'skip'));
		await act(async () => {
			result.current.resolve();
		});

		// Then - an empty upload would look like a silent failure, so we explain instead
		expect(upload).not.toHaveBeenCalled();
		expect(useToastStore.getState().toasts[0]).toMatchObject({
			variant: 'info',
			message: 'Wszystkie pliki pominięto — nic do wgrania.',
		});
	});

	it('uploads anyway when the album names cannot be read, so a failed check never blocks the work', async () => {
		// Given - the collision check itself fails
		const { result, upload } = setup(new Error('offline'));

		// When
		await act(async () => {
			await result.current.begin('album-1', [jpg('foto.jpg')]);
		});

		// Then - the backend still gives the file a unique name, so nothing is lost
		await waitFor(() => expect(uploadedNames(upload)).toEqual(['foto.jpg']));
		expect(result.current.collisions).toBeNull();
	});

	it('cancelling the dialog uploads nothing at all', async () => {
		// Given - one colliding and one safe photo
		const { result, upload } = setup(['foto.jpg']);
		await act(async () => {
			await result.current.begin('album-1', [jpg('foto.jpg'), jpg('inne.jpg')]);
		});

		// When
		act(() => result.current.cancel());

		// Then - cancel means cancel: even the safe file waits for a new decision
		expect(upload).not.toHaveBeenCalled();
		expect(result.current.collisions).toBeNull();
	});

	it('does nothing when no file was picked', async () => {
		// Given
		const { result, getFileNames, upload } = setup([]);

		// When
		await act(async () => {
			await result.current.begin('album-1', []);
		});

		// Then
		expect(getFileNames).not.toHaveBeenCalled();
		expect(upload).not.toHaveBeenCalled();
	});
});
