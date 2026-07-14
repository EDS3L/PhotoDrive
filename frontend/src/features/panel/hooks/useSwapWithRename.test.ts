import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { Mock } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import { suggestNonCollidingName, useSwapWithRename } from './useSwapWithRename';
import { useToastStore } from '@/shared/store/toastStore';

describe('suggestNonCollidingName', () => {
	it('appends the first free counter to the base name', () => {
		// Given / When
		const suggested = suggestNonCollidingName('foto.jpg', new Set());

		// Then
		expect(suggested).toBe('foto_1.jpg');
	});

	it('keeps counting up until the name is actually free', () => {
		// Given - the two obvious candidates are already taken
		const taken = new Set(['foto_1.jpg', 'foto_2.jpg']);

		// When
		const suggested = suggestNonCollidingName('foto.jpg', taken);

		// Then
		expect(suggested).toBe('foto_3.jpg');
	});

	it('appends the counter before the extension, never after it', () => {
		// Given - a name with several dots
		// When
		const suggested = suggestNonCollidingName('sesja.2024.final.jpg', new Set());

		// Then - the file stays a JPG
		expect(suggested).toBe('sesja.2024.final_1.jpg');
	});

	it('handles a name without an extension', () => {
		// Given / When
		const suggested = suggestNonCollidingName('skan', new Set());

		// Then
		expect(suggested).toBe('skan_1');
	});
});

/** Kontrakty mutacji, których hook używa — te same, co w warstwie API panelu. */
type RenameFn = (vars: {
	albumId: string;
	fileId: string;
	newName: string;
}) => Promise<unknown>;
type SwapFn = (vars: {
	sourceAlbumId: string;
	targetAlbumId: string;
	fileIds: string[];
}) => Promise<unknown>;

/**
 * Przenoszenie zdjęć między albumami: backend odrzuca plik, gdy nazwa jest zajęta
 * w albumie docelowym. Hook ma to wyłapać PRZED przeniesieniem i dać fotografowi
 * kontrolę nad nazwą, zamiast zwracać błąd w połowie operacji.
 */
describe('useSwapWithRename', () => {
	const photo = (fileID: string, fileName: string) => ({ fileID, fileName });

	function setup(overrides: {
		getFileNames?: () => Promise<string[]>;
		rename?: Mock<RenameFn>;
		swap?: Mock<SwapFn>;
	} = {}) {
		const rename = overrides.rename ?? vi.fn<RenameFn>().mockResolvedValue(undefined);
		const swap = overrides.swap ?? vi.fn<SwapFn>().mockResolvedValue(undefined);
		const getFileNames = vi.fn(overrides.getFileNames ?? (async () => []));
		const onDone = vi.fn();
		const hook = renderHook(() =>
			useSwapWithRename({ getFileNames, rename, swap, onDone }),
		);
		return { ...hook, rename, swap, getFileNames, onDone };
	}

	beforeEach(() => {
		useToastStore.setState({ toasts: [] });
	});

	it('moves the photos straight away when no name is taken in the target album', async () => {
		// Given - the target album holds unrelated names
		const { result, swap, onDone } = setup({
			getFileNames: async () => ['inne.jpg'],
		});

		// When
		await act(async () => {
			await result.current.start({
				sourceAlbumId: 'src',
				targetAlbumId: 'dst',
				files: [photo('f1', 'foto.jpg'), photo('f2', 'plaza.jpg')],
			});
		});

		// Then - no dialog is shown, the move just happens
		expect(swap).toHaveBeenCalledWith({
			sourceAlbumId: 'src',
			targetAlbumId: 'dst',
			fileIds: ['f1', 'f2'],
		});
		expect(result.current.renames).toBeNull();
		expect(onDone).toHaveBeenCalledOnce();
	});

	it('holds the move back and proposes free names when the target album already has them', async () => {
		// Given - 'foto.jpg' is taken in the target, and so is the obvious replacement
		const { result, swap } = setup({
			getFileNames: async () => ['foto.jpg', 'foto_1.jpg'],
		});

		// When
		await act(async () => {
			await result.current.start({
				sourceAlbumId: 'src',
				targetAlbumId: 'dst',
				files: [photo('f1', 'foto.jpg'), photo('f2', 'wolna.jpg')],
			});
		});

		// Then - nothing is moved until the photographer decides...
		expect(swap).not.toHaveBeenCalled();
		// ...and only the colliding photo needs a decision, with a name that is really free
		expect(result.current.renames).toEqual([
			{ fileID: 'f1', originalName: 'foto.jpg', newName: 'foto_2.jpg' },
		]);
	});

	it('proposed names avoid each other, not just the names already in the album', async () => {
		// Given - two photos collide and would both fall back to the same suggestion
		const { result } = setup({
			getFileNames: async () => ['a.jpg', 'b.jpg'],
		});

		// When
		await act(async () => {
			await result.current.start({
				sourceAlbumId: 'src',
				targetAlbumId: 'dst',
				files: [photo('f1', 'a.jpg'), photo('f2', 'b.jpg')],
			});
		});

		// Then - the suggestions are distinct
		const proposed = result.current.renames?.map((r) => r.newName);
		expect(proposed).toEqual(['a_1.jpg', 'b_1.jpg']);
		expect(new Set(proposed).size).toBe(proposed?.length);
	});

	it('renames in the source album BEFORE moving, so the move cannot collide', async () => {
		// Given - a confirmed rename plan
		const calls: string[] = [];
		const rename = vi.fn<RenameFn>(async () => {
			calls.push('rename');
		});
		const swap = vi.fn<SwapFn>(async () => {
			calls.push('swap');
		});
		const { result, onDone } = setup({
			getFileNames: async () => ['foto.jpg'],
			rename,
			swap,
		});
		await act(async () => {
			await result.current.start({
				sourceAlbumId: 'src',
				targetAlbumId: 'dst',
				files: [photo('f1', 'foto.jpg')],
			});
		});

		// When - the photographer accepts the proposed name
		await act(async () => {
			await result.current.confirm();
		});

		// Then - order matters: rename first, move second
		expect(calls).toEqual(['rename', 'swap']);
		expect(rename).toHaveBeenCalledWith({
			albumId: 'src',
			fileId: 'f1',
			newName: 'foto_1.jpg',
		});
		expect(swap).toHaveBeenCalledWith({
			sourceAlbumId: 'src',
			targetAlbumId: 'dst',
			fileIds: ['f1'],
		});
		expect(result.current.renames).toBeNull();
		expect(onDone).toHaveBeenCalledOnce();
	});

	it('uses the name typed by the photographer instead of the proposed one', async () => {
		// Given
		const { result, rename } = setup({ getFileNames: async () => ['foto.jpg'] });
		await act(async () => {
			await result.current.start({
				sourceAlbumId: 'src',
				targetAlbumId: 'dst',
				files: [photo('f1', 'foto.jpg')],
			});
		});

		// When - the suggestion is overwritten
		act(() => result.current.setNewName('f1', 'plaza-zachod.jpg'));
		await act(async () => {
			await result.current.confirm();
		});

		// Then
		expect(rename).toHaveBeenCalledWith({
			albumId: 'src',
			fileId: 'f1',
			newName: 'plaza-zachod.jpg',
		});
	});

	it('keeps the dialog open when a rename fails, so the photographer can correct the name', async () => {
		// Given - the backend rejects the new name
		const rename = vi.fn<RenameFn>().mockRejectedValue(new Error('Nazwa zajęta'));
		const { result, swap } = setup({
			getFileNames: async () => ['foto.jpg'],
			rename,
			swap: vi.fn<SwapFn>(),
		});
		await act(async () => {
			await result.current.start({
				sourceAlbumId: 'src',
				targetAlbumId: 'dst',
				files: [photo('f1', 'foto.jpg')],
			});
		});

		// When
		await act(async () => {
			await result.current.confirm();
		});

		// Then - the photos are NOT moved and the plan survives for a retry
		expect(swap).not.toHaveBeenCalled();
		expect(result.current.renames).not.toBeNull();
		await waitFor(() => expect(result.current.isSubmitting).toBe(false));
	});

	it('does not move anything when the target album names cannot be read', async () => {
		// Given - the collision check itself fails
		const { result, swap } = setup({
			getFileNames: async () => {
				throw new Error('offline');
			},
		});

		// When
		await act(async () => {
			await result.current.start({
				sourceAlbumId: 'src',
				targetAlbumId: 'dst',
				files: [photo('f1', 'foto.jpg')],
			});
		});

		// Then - moving blindly could overwrite the client's photos, so we stop and say so
		expect(swap).not.toHaveBeenCalled();
		expect(useToastStore.getState().toasts[0]).toMatchObject({ variant: 'error' });
	});

	it('cancelling drops the plan without moving anything', async () => {
		// Given
		const { result, swap, rename } = setup({ getFileNames: async () => ['foto.jpg'] });
		await act(async () => {
			await result.current.start({
				sourceAlbumId: 'src',
				targetAlbumId: 'dst',
				files: [photo('f1', 'foto.jpg')],
			});
		});

		// When
		act(() => result.current.cancel());

		// Then
		expect(result.current.renames).toBeNull();
		expect(rename).not.toHaveBeenCalled();
		expect(swap).not.toHaveBeenCalled();
	});

	it('does nothing when no photo is selected', async () => {
		// Given
		const { result, getFileNames, swap } = setup();

		// When
		await act(async () => {
			await result.current.start({
				sourceAlbumId: 'src',
				targetAlbumId: 'dst',
				files: [],
			});
		});

		// Then - no pointless round-trip to the backend
		expect(getFileNames).not.toHaveBeenCalled();
		expect(swap).not.toHaveBeenCalled();
	});
});
