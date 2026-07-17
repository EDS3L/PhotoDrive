import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { triggerBlobDownload } from './downloadBlob';

describe('triggerBlobDownload', () => {
	let createObjectURL: ReturnType<typeof vi.fn>;
	let revokeObjectURL: ReturnType<typeof vi.fn>;

	beforeEach(() => {
		vi.useFakeTimers();
		// jsdom nie implementuje URL.createObjectURL — podstawiamy własne, sterowalne.
		createObjectURL = vi.fn(() => 'blob:fake-url');
		revokeObjectURL = vi.fn();
		URL.createObjectURL = createObjectURL as unknown as typeof URL.createObjectURL;
		URL.revokeObjectURL = revokeObjectURL as unknown as typeof URL.revokeObjectURL;
	});

	afterEach(() => {
		vi.useRealTimers();
	});

	it('clicks a temporary <a download> built from the blob and cleans it up — one place instead of two drifting copies (F.3)', () => {
		// Given - a zip blob and a human-readable filename (never the UUID)
		const blob = new Blob(['zip-bytes'], { type: 'application/zip' });
		let clickedWith: { href: string | null; download: string; inDom: boolean } | null = null;
		const clickSpy = vi
			.spyOn(HTMLAnchorElement.prototype, 'click')
			.mockImplementation(function (this: HTMLAnchorElement) {
				// Stan łapiemy W MOMENCIE kliknięcia — potem element jest już usunięty z DOM.
				clickedWith = {
					href: this.getAttribute('href'),
					download: this.download,
					inDom: document.body.contains(this),
				};
			});

		// When
		triggerBlobDownload(blob, 'Wesele Ani.zip');

		// Then - the object URL is built from THIS blob, and the anchor carried the filename while attached
		expect(createObjectURL).toHaveBeenCalledWith(blob);
		expect(clickedWith).toEqual({
			href: 'blob:fake-url',
			download: 'Wesele Ani.zip',
			inDom: true,
		});
		// ...and the anchor is gone right after, so it never litters the DOM
		expect(document.querySelector('a[download]')).toBeNull();

		// Then - the object URL is revoked only on the next tick (never leaked, never revoked too early)
		expect(revokeObjectURL).not.toHaveBeenCalled();
		vi.runAllTimers();
		expect(revokeObjectURL).toHaveBeenCalledWith('blob:fake-url');

		clickSpy.mockRestore();
	});
});
