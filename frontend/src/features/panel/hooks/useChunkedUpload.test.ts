import { describe, it, expect, vi, beforeEach } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import { useChunkedUpload } from './useChunkedUpload';
import { useToastStore } from '@/shared/store/toastStore';

/** Wysyłka jednej paczki — ten sam kontrakt, którego hook oczekuje od warstwy API. */
type UploadFn = (
	files: File[],
	onProgress: (percent: number) => void,
) => Promise<void>;

/** File of a declared size — the hook only reads `size`, so we avoid allocating real megabytes. */
function fileOf(name: string, sizeBytes: number): File {
	const file = new File(['x'], name, { type: 'image/jpeg' });
	Object.defineProperty(file, 'size', { value: sizeBytes });
	return file;
}

function filesOf(count: number, sizeBytes = 1_000): File[] {
	return Array.from({ length: count }, (_, i) => fileOf(`foto-${i}.jpg`, sizeBytes));
}

const MB = 1024 * 1024;

/** Promise resolved from the outside — lets a test hold a chunk "in flight". */
function deferred() {
	let resolve!: () => void;
	let reject!: (reason: unknown) => void;
	const promise = new Promise<void>((res, rej) => {
		resolve = res;
		reject = rej;
	});
	return { promise, resolve, reject };
}

/**
 * Pasek uploadu obiecuje użytkownikowi, że 100% = pliki SĄ na dysku VPS
 * (a nie „bajty poszły w kabel"). Te testy pilnują właśnie tej obietnicy
 * oraz tego, że żaden plik nie ginie po drodze.
 */
describe('useChunkedUpload', () => {
	beforeEach(() => {
		useToastStore.setState({ toasts: [] });
	});

	it('splits the upload into chunks of at most 20 files', async () => {
		// Given
		const upload = vi.fn<UploadFn>().mockResolvedValue(undefined);
		const { result } = renderHook(() => useChunkedUpload({ upload }));

		// When
		await act(async () => {
			await result.current.start(filesOf(25));
		});

		// Then - two requests instead of one 25-file giant
		expect(upload).toHaveBeenCalledTimes(2);
		expect(upload.mock.calls[0][0]).toHaveLength(20);
		expect(upload.mock.calls[1][0]).toHaveLength(5);
	});

	it('closes a chunk early when the size budget would be exceeded', async () => {
		// Given - three files of 40 MB against a 100 MB budget
		const upload = vi.fn<UploadFn>().mockResolvedValue(undefined);
		const { result } = renderHook(() => useChunkedUpload({ upload }));

		// When
		await act(async () => {
			await result.current.start([
				fileOf('a.jpg', 40 * MB),
				fileOf('b.jpg', 40 * MB),
				fileOf('c.jpg', 40 * MB),
			]);
		});

		// Then - the third file starts a new chunk; a 120 MB request is never sent
		expect(upload.mock.calls[0][0]).toHaveLength(2);
		expect(upload.mock.calls[1][0]).toHaveLength(1);
	});

	it('still sends a file that on its own exceeds the chunk budget', async () => {
		// Given - a single photo larger than the whole chunk limit
		const upload = vi.fn<UploadFn>().mockResolvedValue(undefined);
		const { result } = renderHook(() => useChunkedUpload({ upload }));

		// When
		await act(async () => {
			await result.current.start([fileOf('huge.jpg', 150 * MB), fileOf('small.jpg', 1 * MB)]);
		});

		// Then - it travels alone rather than being silently dropped
		expect(upload).toHaveBeenCalledTimes(2);
		expect(upload.mock.calls[0][0].map((f) => f.name)).toEqual(['huge.jpg']);
		expect(upload.mock.calls[1][0].map((f) => f.name)).toEqual(['small.jpg']);
	});

	it('sending the bytes alone fills at most half the bar, because the save is not confirmed yet', async () => {
		// Given - a chunk whose bytes are fully sent, but whose response has not arrived
		const inFlight = deferred();
		const upload = vi.fn<UploadFn>(async (_files, onProgress) => {
			onProgress(100);
			await inFlight.promise;
		});
		const { result } = renderHook(() => useChunkedUpload({ upload }));

		// When
		await act(async () => {
			void result.current.start(filesOf(3));
		});

		// Then - the bar waits for the server; the phase tells the user we are saving
		expect(result.current.state?.percent).toBe(50);
		expect(result.current.state?.phase).toBe('saving');
		expect(result.current.state?.savedCount).toBe(0);

		// Cleanup - let the pending chunk finish
		await act(async () => {
			inFlight.resolve();
		});
	});

	it('reaches 100% only once the last chunk is confirmed as saved', async () => {
		// Given - 25 files = two chunks; the second one is held in flight
		const second = deferred();
		const upload = vi
			.fn<UploadFn>()
			.mockImplementationOnce(async (_files, onProgress) => {
				onProgress(100);
			})
			.mockImplementationOnce(async (_files, onProgress) => {
				onProgress(100);
				await second.promise;
			});
		const { result } = renderHook(() => useChunkedUpload({ upload }));

		// When - the first chunk is saved, the second is still travelling
		await act(async () => {
			void result.current.start(filesOf(25));
		});

		// Then - progress is honest: 20 of 25 saved, bar below 100
		expect(result.current.state?.savedCount).toBe(20);
		expect(result.current.state?.totalCount).toBe(25);
		expect(result.current.state?.percent).toBeLessThan(100);

		// When - the server confirms the last chunk
		await act(async () => {
			second.resolve();
		});

		// Then
		expect(result.current.state?.percent).toBe(100);
		expect(result.current.state?.savedCount).toBe(25);
		expect(result.current.state?.phase).toBe('done');
	});

	it('stops after a failed chunk but still refreshes the album, so saved photos are not lost from view', async () => {
		// Given - 45 files = three chunks; the second one fails
		const onComplete = vi.fn();
		const upload = vi
			.fn<UploadFn>()
			.mockResolvedValueOnce(undefined)
			.mockRejectedValueOnce(new Error('Połączenie przerwane'))
			.mockResolvedValueOnce(undefined);
		const { result } = renderHook(() => useChunkedUpload({ upload, onComplete }));

		// When
		await act(async () => {
			await result.current.start(filesOf(45));
		});

		// Then - the third chunk is not attempted...
		expect(upload).toHaveBeenCalledTimes(2);
		// ...the list is refreshed anyway (the first chunk IS on disk)...
		expect(onComplete).toHaveBeenCalledTimes(1);
		// ...and the user learns why it stopped
		const toasts = useToastStore.getState().toasts;
		expect(toasts).toHaveLength(1);
		expect(toasts[0]).toMatchObject({ variant: 'error', message: 'Połączenie przerwane' });
	});

	it('ignores a second upload started while one is still running', async () => {
		// Given - an upload in flight
		const inFlight = deferred();
		const upload = vi.fn<UploadFn>(async () => {
			await inFlight.promise;
		});
		const { result } = renderHook(() => useChunkedUpload({ upload }));
		await act(async () => {
			void result.current.start([fileOf('first.jpg', 1_000)]);
		});

		// When - the user drops more files before the first batch finishes
		await act(async () => {
			void result.current.start([fileOf('second.jpg', 1_000)]);
		});

		// Then - the second batch is not interleaved into the running one
		expect(upload).toHaveBeenCalledTimes(1);
		expect(upload.mock.calls[0][0].map((f) => f.name)).toEqual(['first.jpg']);

		// Cleanup
		await act(async () => {
			inFlight.resolve();
		});
	});

	it('does nothing when there are no files to send', async () => {
		// Given
		const upload = vi.fn<UploadFn>();
		const onComplete = vi.fn();
		const { result } = renderHook(() => useChunkedUpload({ upload, onComplete }));

		// When
		await act(async () => {
			await result.current.start([]);
		});

		// Then - no empty request, no progress bar
		expect(upload).not.toHaveBeenCalled();
		expect(onComplete).not.toHaveBeenCalled();
		expect(result.current.state).toBeNull();
	});

	it('hides the progress bar shortly after the upload is done', async () => {
		// Given
		vi.useFakeTimers();
		try {
			const upload = vi.fn<UploadFn>().mockResolvedValue(undefined);
			const { result } = renderHook(() => useChunkedUpload({ upload }));
			await act(async () => {
				await result.current.start(filesOf(2));
			});
			expect(result.current.state?.percent).toBe(100);

			// When - the user has had a moment to see the full bar
			await act(async () => {
				vi.advanceTimersByTime(800);
			});

			// Then
			expect(result.current.state).toBeNull();
		} finally {
			vi.useRealTimers();
		}
	});
});
