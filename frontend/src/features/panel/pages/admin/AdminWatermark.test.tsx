import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AdminWatermark from './AdminWatermark';
import { useToastStore } from '@/shared/store/toastStore';
import type {
	getWatermarkStatus,
	uploadWatermark,
	deleteWatermark,
} from '../../api/watermarkApi';

const { statusMock, uploadMock, deleteMock } = vi.hoisted(() => ({
	statusMock: vi.fn<typeof getWatermarkStatus>(),
	uploadMock: vi.fn<typeof uploadWatermark>(),
	deleteMock: vi.fn<typeof deleteWatermark>(),
}));

vi.mock('../../api/watermarkApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('../../api/watermarkApi')>();
	return {
		...actual,
		getWatermarkStatus: statusMock,
		uploadWatermark: uploadMock,
		deleteWatermark: deleteMock,
	};
});

function renderPage() {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
	});
	return render(
		<QueryClientProvider client={queryClient}>
			<AdminWatermark />
		</QueryClientProvider>,
	);
}

function pngFile(name = 'logo.png'): File {
	return new File(['png-bytes'], name, { type: 'image/png' });
}

describe('AdminWatermark', () => {
	beforeEach(() => {
		statusMock.mockReset().mockResolvedValue({ configured: false, updatedAt: null });
		uploadMock.mockReset().mockResolvedValue(undefined);
		deleteMock.mockReset().mockResolvedValue(undefined);
		// Toasty to modułowy singleton — bez resetu przeciekają między testami.
		useToastStore.setState({ toasts: [] });
	});

	it('Without a logo there is nothing to delete, so only uploading is offered', async () => {
		// Given
		statusMock.mockResolvedValue({ configured: false, updatedAt: null });

		// When
		renderPage();

		// Then - offering "Usuń" would produce a 404 from the backend
		expect(await screen.findByRole('button', { name: /Wgraj logo/ })).toBeInTheDocument();
		expect(screen.queryByRole('button', { name: /Usuń/ })).not.toBeInTheDocument();
		expect(screen.getByText(/Brak znaku wodnego/)).toBeInTheDocument();
	});

	it('A configured logo is previewed with its version in the URL, so a replaced logo is not served from cache', async () => {
		// Given
		statusMock.mockResolvedValue({
			configured: true,
			updatedAt: '2026-07-25T10:00:00Z',
		});

		// When
		renderPage();

		// Then - the version stamp is what invalidates the browser cache (F.12 pattern)
		const preview = await screen.findByRole('img', { name: 'Znak wodny platformy' });
		expect(preview).toHaveAttribute('src', expect.stringContaining('v='));
		expect(preview.getAttribute('src')).toContain('2026-07-25');
	});

	it('With a logo present the action is a replacement rather than a first upload, so the label matches what will happen', async () => {
		// Given
		statusMock.mockResolvedValue({
			configured: true,
			updatedAt: '2026-07-25T10:00:00Z',
		});

		// When
		renderPage();

		// Then
		expect(await screen.findByRole('button', { name: /Podmień logo/ })).toBeInTheDocument();
		expect(screen.queryByRole('button', { name: /Wgraj logo/ })).not.toBeInTheDocument();
	});

	it('Uploading a logo sends the chosen file and confirms it was saved', async () => {
		// Given
		renderPage();
		await screen.findByRole('button', { name: /Wgraj logo/ });
		const logo = pngFile();

		// When
		await userEvent.upload(document.querySelector('input[type="file"]')!, logo);

		// Then
		await vi.waitFor(() => expect(uploadMock).toHaveBeenCalledWith(logo));
		await vi.waitFor(() =>
			expect(
				useToastStore.getState().toasts.some((t) => t.message.includes('zapisany')),
			).toBe(true),
		);
	});

	it('Replacing an existing logo says "replaced" and not "saved", because the two are different events for the admin', async () => {
		// Given
		statusMock.mockResolvedValue({
			configured: true,
			updatedAt: '2026-07-25T10:00:00Z',
		});
		renderPage();
		await screen.findByRole('button', { name: /Podmień logo/ });

		// When
		await userEvent.upload(document.querySelector('input[type="file"]')!, pngFile());

		// Then
		await vi.waitFor(() =>
			expect(
				useToastStore
					.getState()
					.toasts.some((t) => t.message.includes('podmieniony')),
			).toBe(true),
		);
	});

	it('The file input only accepts PNG, because a transparent background is what makes a tiled watermark usable', async () => {
		// Given
		renderPage();
		await screen.findByRole('button', { name: /Wgraj logo/ });

		// Then
		expect(document.querySelector('input[type="file"]')).toHaveAttribute(
			'accept',
			'image/png',
		);
	});

	it('The same file can be chosen twice in a row, because the input is cleared after each pick (F.13)', async () => {
		// Given
		renderPage();
		await screen.findByRole('button', { name: /Wgraj logo/ });
		const input = document.querySelector('input[type="file"]') as HTMLInputElement;
		const logo = pngFile();

		// When - the identical file is picked twice
		await userEvent.upload(input, logo);
		await userEvent.upload(input, logo);

		// Then - without clearing `value` the browser fires no second change event
		await vi.waitFor(() => expect(uploadMock).toHaveBeenCalledTimes(2));
	});

	it('Deleting the logo asks for confirmation first, because photographers lose the watermark option with it', async () => {
		// Given
		statusMock.mockResolvedValue({
			configured: true,
			updatedAt: '2026-07-25T10:00:00Z',
		});
		renderPage();

		// When
		await userEvent.click(await screen.findByRole('button', { name: /Usuń/ }));

		// Then - nothing is sent until the admin confirms
		expect(screen.getByText('Usunąć znak wodny?')).toBeInTheDocument();
		expect(deleteMock).not.toHaveBeenCalled();
	});

	it('Confirming the deletion sends it and reports success', async () => {
		// Given
		statusMock.mockResolvedValue({
			configured: true,
			updatedAt: '2026-07-25T10:00:00Z',
		});
		renderPage();
		await userEvent.click(await screen.findByRole('button', { name: /Usuń/ }));

		// When
		const dialog = screen.getByRole('dialog');
		await userEvent.click(within(dialog).getByRole('button', { name: 'Usuń' }));

		// Then
		await vi.waitFor(() => expect(deleteMock).toHaveBeenCalled());
		await vi.waitFor(() =>
			expect(
				useToastStore.getState().toasts.some((t) => t.message.includes('usunięty')),
			).toBe(true),
		);
	});

	it('Abandoning the confirmation deletes nothing, so a misclick is harmless', async () => {
		// Given
		statusMock.mockResolvedValue({
			configured: true,
			updatedAt: '2026-07-25T10:00:00Z',
		});
		renderPage();
		await userEvent.click(await screen.findByRole('button', { name: /Usuń/ }));

		// When
		await userEvent.click(screen.getByRole('button', { name: 'Anuluj' }));

		// Then
		expect(deleteMock).not.toHaveBeenCalled();
		expect(screen.queryByText('Usunąć znak wodny?')).not.toBeInTheDocument();
	});
});
