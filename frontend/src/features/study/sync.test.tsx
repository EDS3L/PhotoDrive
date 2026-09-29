import { describe, it, expect, beforeEach, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getStudyProgress, saveStudyProgress } from '@/lib/publicApi';
import StudyPage from './index';
import { LEARNED_STORAGE_KEY, PUSH_DELAY_MS, SYNC_CODE_STORAGE_KEY } from './lib/useLearned';
import type { StudyQuestion } from './types';

vi.mock('@/lib/publicApi', async (importOriginal) => ({
	...(await importOriginal<typeof import('@/lib/publicApi')>()),
	getStudyProgress: vi.fn<typeof getStudyProgress>(),
	saveStudyProgress: vi.fn<typeof saveStudyProgress>(),
}));

const getMock = vi.mocked(getStudyProgress);
const saveMock = vi.mocked(saveStudyProgress);

const CODE = 'k7Qp2xWmZ-a_9LrT0bNc4e';

const questions: StudyQuestion[] = [
	{ n: 1, title: 'Systemy liczbowe', hints: ['podstawy'], short: [{ k: 'p', c: ['Krótka 1.'] }], long: [] },
	{ n: 2, title: 'Pętle', hints: ['while'], short: [{ k: 'p', c: ['Krótka 2.'] }], long: [] },
];

const mark = (n: number) => screen.getByRole('button', { name: `Pytanie ${n} nauczone` });
const syncPanel = () => screen.getByRole('region', { name: 'Synchronizacja postępu' });

describe('StudyPage progress sync', () => {
	beforeEach(() => {
		localStorage.clear();
		window.history.replaceState(null, '', '/nauka');
		getMock.mockReset();
		saveMock.mockReset().mockImplementation((_code, learned) =>
			Promise.resolve({ learned, updatedAt: Date.now() }),
		);
	});

	it('Without sync the page never talks to the server and says progress lives on this device only', () => {
		// When
		render(<StudyPage questions={questions} />);

		// Then
		expect(syncPanel()).toHaveTextContent('Postęp jest zapisany tylko na tym urządzeniu');
		expect(getMock).not.toHaveBeenCalled();
		expect(saveMock).not.toHaveBeenCalled();
	});

	it('Enabling sync uploads this device progress under a fresh code and offers a link to open on the other device', async () => {
		// Given
		const user = userEvent.setup();
		localStorage.setItem(LEARNED_STORAGE_KEY, JSON.stringify([2]));
		render(<StudyPage questions={questions} />);

		// When
		await user.click(screen.getByRole('button', { name: 'Włącz synchronizację' }));

		// Then
		await waitFor(() => expect(syncPanel()).toHaveTextContent('Synchronizacja: zsynchronizowano'));
		const code = localStorage.getItem(SYNC_CODE_STORAGE_KEY)!;
		expect(code).toMatch(/^[A-Za-z0-9_-]{22}$/);
		expect(saveMock).toHaveBeenCalledWith(code, [2]);

		// When
		await user.click(screen.getByRole('button', { name: 'Skopiuj link synchronizacji' }));

		// Then
		expect(await navigator.clipboard.readText()).toBe(`${window.location.origin}/nauka#sync=${code}`);
	});

	it('Opening a sync link on another device replaces its progress with the server state and remembers the code', async () => {
		// Given
		localStorage.setItem(LEARNED_STORAGE_KEY, JSON.stringify([2]));
		window.history.replaceState(null, '', `/nauka#sync=${CODE}`);
		getMock.mockResolvedValue({ learned: [1], updatedAt: 5 });

		// When
		render(<StudyPage questions={questions} />);

		// Then
		await waitFor(() => expect(mark(1)).toHaveAttribute('aria-pressed', 'true'));
		expect(mark(2)).toHaveAttribute('aria-pressed', 'false');
		expect(getMock).toHaveBeenCalledWith(CODE);
		expect(saveMock).not.toHaveBeenCalled();
		expect(localStorage.getItem(SYNC_CODE_STORAGE_KEY)).toBe(CODE);
		expect(window.location.hash).toBe('');
		expect(screen.getByRole('status')).toHaveTextContent('postęp pobrany z serwera');
	});

	it('Marking questions with sync on sends the new progress to the server, one save per burst of clicks', async () => {
		// Given
		const user = userEvent.setup();
		localStorage.setItem(SYNC_CODE_STORAGE_KEY, CODE);
		getMock.mockResolvedValue({ learned: [], updatedAt: 5 });
		render(<StudyPage questions={questions} />);
		await waitFor(() => expect(syncPanel()).toHaveTextContent('zsynchronizowano'));

		// When
		await user.click(mark(1));
		await user.click(mark(2));

		// Then
		await waitFor(() => expect(saveMock).toHaveBeenCalledWith(CODE, [1, 2]));
		expect(saveMock).toHaveBeenCalledTimes(1);
	});

	it('Returning to the tab pulls the progress saved meanwhile on the other device', async () => {
		// Given
		localStorage.setItem(SYNC_CODE_STORAGE_KEY, CODE);
		getMock.mockResolvedValueOnce({ learned: [], updatedAt: 1 });
		render(<StudyPage questions={questions} />);
		await waitFor(() => expect(syncPanel()).toHaveTextContent('zsynchronizowano'));
		getMock.mockResolvedValueOnce({ learned: [2], updatedAt: 2 });

		// When
		act(() => {
			window.dispatchEvent(new Event('focus'));
		});

		// Then
		await waitFor(() => expect(mark(2)).toHaveAttribute('aria-pressed', 'true'));
	});

	it('Without a connection the change stays on the device and is sent, not overwritten, when the tab is visited again', async () => {
		// Given
		const user = userEvent.setup();
		localStorage.setItem(SYNC_CODE_STORAGE_KEY, CODE);
		getMock.mockResolvedValue({ learned: [], updatedAt: 1 });
		saveMock.mockRejectedValueOnce(new Error('offline'));
		render(<StudyPage questions={questions} />);
		await waitFor(() => expect(syncPanel()).toHaveTextContent('zsynchronizowano'));

		// When
		await user.click(mark(1));

		// Then
		await waitFor(() => expect(syncPanel()).toHaveTextContent('brak połączenia'));
		expect(mark(1)).toHaveAttribute('aria-pressed', 'true');

		// When
		act(() => {
			window.dispatchEvent(new Event('focus'));
		});

		// Then
		await waitFor(() => expect(syncPanel()).toHaveTextContent('zsynchronizowano'));
		expect(saveMock).toHaveBeenLastCalledWith(CODE, [1]);
		expect(getMock).toHaveBeenCalledTimes(1);
		expect(mark(1)).toHaveAttribute('aria-pressed', 'true');
	});

	it('Joining a code that has nothing on the server uploads this device progress instead of wiping it', async () => {
		// Given
		const user = userEvent.setup();
		localStorage.setItem(LEARNED_STORAGE_KEY, JSON.stringify([1]));
		getMock.mockResolvedValue({ learned: [], updatedAt: null });
		render(<StudyPage questions={questions} />);

		// When
		await user.type(screen.getByRole('textbox', { name: 'Link lub kod synchronizacji' }), CODE);
		await user.click(screen.getByRole('button', { name: 'Połącz' }));

		// Then
		await waitFor(() => expect(saveMock).toHaveBeenCalledWith(CODE, [1]));
		expect(mark(1)).toHaveAttribute('aria-pressed', 'true');
		expect(localStorage.getItem(SYNC_CODE_STORAGE_KEY)).toBe(CODE);
	});

	it('Pasting something that is not a sync link says so and keeps sync off', async () => {
		// Given
		const user = userEvent.setup();
		render(<StudyPage questions={questions} />);

		// When
		await user.type(screen.getByRole('textbox', { name: 'Link lub kod synchronizacji' }), 'https://photodrive.dev/nauka');
		await user.click(screen.getByRole('button', { name: 'Połącz' }));

		// Then
		expect(screen.getByRole('status')).toHaveTextContent('To nie jest poprawny link ani kod synchronizacji');
		expect(localStorage.getItem(SYNC_CODE_STORAGE_KEY)).toBeNull();
		expect(getMock).not.toHaveBeenCalled();
	});

	it('A damaged sync link keeps this device progress and says so', () => {
		// Given
		localStorage.setItem(LEARNED_STORAGE_KEY, JSON.stringify([2]));
		window.history.replaceState(null, '', '/nauka#sync=broken');

		// When
		render(<StudyPage questions={questions} />);

		// Then
		expect(mark(2)).toHaveAttribute('aria-pressed', 'true');
		expect(screen.getByRole('status')).toHaveTextContent('Link synchronizacji jest nieprawidłowy');
		expect(getMock).not.toHaveBeenCalled();
		expect(window.location.hash).toBe('');
	});

	it('Turning sync off forgets the code and stops talking to the server, while local progress stays', async () => {
		// Given
		const user = userEvent.setup();
		localStorage.setItem(SYNC_CODE_STORAGE_KEY, CODE);
		getMock.mockResolvedValue({ learned: [2], updatedAt: 1 });
		render(<StudyPage questions={questions} />);
		await waitFor(() => expect(mark(2)).toHaveAttribute('aria-pressed', 'true'));

		// When
		await user.click(screen.getByRole('button', { name: 'Wyłącz synchronizację' }));
		await user.click(mark(1));
		await new Promise((resolve) => setTimeout(resolve, PUSH_DELAY_MS + 100));

		// Then
		expect(localStorage.getItem(SYNC_CODE_STORAGE_KEY)).toBeNull();
		expect(saveMock).not.toHaveBeenCalled();
		expect(mark(2)).toHaveAttribute('aria-pressed', 'true');
		expect(JSON.parse(localStorage.getItem(LEARNED_STORAGE_KEY)!)).toEqual([2, 1]);
	});

	it('When the clipboard is unavailable the sync link is shown for manual copying', async () => {
		// Given
		const user = userEvent.setup();
		localStorage.setItem(SYNC_CODE_STORAGE_KEY, CODE);
		getMock.mockResolvedValue({ learned: [], updatedAt: 1 });
		render(<StudyPage questions={questions} />);
		vi.spyOn(navigator.clipboard, 'writeText').mockRejectedValueOnce(new Error('denied'));

		// When
		await user.click(screen.getByRole('button', { name: 'Skopiuj link synchronizacji' }));

		// Then
		expect(screen.getByRole('textbox', { name: 'Link synchronizacji' })).toHaveValue(
			`${window.location.origin}/nauka#sync=${CODE}`,
		);
	});
});
