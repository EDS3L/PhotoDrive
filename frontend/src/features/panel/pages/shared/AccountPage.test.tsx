import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AccountPage from './AccountPage';
import { usePanelAuthStore } from '../../store/panelAuthStore';
import type {
	changePassword,
	changeEmail,
	getMe,
} from '../../api/panelAuthApi';
import type { CurrentUserInfo } from '../../types/panel';

const { changePasswordMock, changeEmailMock, getMeMock } = vi.hoisted(() => ({
	changePasswordMock: vi.fn<typeof changePassword>(),
	changeEmailMock: vi.fn<typeof changeEmail>(),
	getMeMock: vi.fn<typeof getMe>(),
}));

vi.mock('../../api/panelAuthApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('../../api/panelAuthApi')>();
	return {
		...actual,
		changePassword: changePasswordMock,
		changeEmail: changeEmailMock,
		getMe: getMeMock,
	};
});

function currentUser(overrides: Partial<CurrentUserInfo> = {}): CurrentUserInfo {
	return {
		id: 'admin-1',
		name: 'Admin Glowny',
		email: 'admin@photodrive.pl',
		roles: ['ADMIN'],
		changePasswordOnNextLogin: false,
		...overrides,
	};
}

async function fillPasswordForm(current: string, next: string, confirm: string) {
	await userEvent.type(screen.getByLabelText('Aktualne hasło'), current);
	await userEvent.type(screen.getByLabelText('Nowe hasło'), next);
	await userEvent.type(screen.getByLabelText('Potwierdź nowe hasło'), confirm);
	await userEvent.click(screen.getByRole('button', { name: /Zmień hasło/ }));
}

describe('AccountPage', () => {
	beforeEach(() => {
		changePasswordMock.mockReset().mockResolvedValue(undefined);
		changeEmailMock.mockReset().mockResolvedValue(undefined);
		getMeMock.mockReset().mockResolvedValue(currentUser());
		// Store jest modułowym singletonem — bez ustawienia usera strona nie ma czyje konto pokazać.
		usePanelAuthStore.getState().setUser(currentUser());
	});

	it('The account shows who is logged in, so the admin knows which account they are editing', () => {
		// When
		render(<AccountPage />);

		// Then
		expect(screen.getByText(/Admin Glowny/)).toBeInTheDocument();
		expect(screen.getByText(/admin@photodrive\.pl/)).toBeInTheDocument();
		expect(screen.getByText(/ADMIN/)).toBeInTheDocument();
	});

	it('A mismatched confirmation blocks the change, so a typo cannot set a password the user does not know', async () => {
		// Given
		render(<AccountPage />);

		// When
		await fillPasswordForm('stare123', 'noweHaslo1', 'inneHaslo1');

		// Then - nothing reaches the server
		expect(await screen.findByText('Hasła muszą być takie same')).toBeInTheDocument();
		expect(changePasswordMock).not.toHaveBeenCalled();
	});

	it('A password shorter than eight characters is rejected in the form, matching the rule the backend enforces', async () => {
		// Given
		render(<AccountPage />);

		// When
		await fillPasswordForm('stare123', 'krotkie', 'krotkie');

		// Then
		expect(await screen.findByText('Min. 8 znaków')).toBeInTheDocument();
		expect(changePasswordMock).not.toHaveBeenCalled();
	});

	it('The current password is required, because it is what proves the owner is asking', async () => {
		// Given
		render(<AccountPage />);

		// When - the current password is left empty
		await userEvent.type(screen.getByLabelText('Nowe hasło'), 'noweHaslo1');
		await userEvent.type(screen.getByLabelText('Potwierdź nowe hasło'), 'noweHaslo1');
		await userEvent.click(screen.getByRole('button', { name: /Zmień hasło/ }));

		// Then
		expect(await screen.findByText('Wymagane')).toBeInTheDocument();
		expect(changePasswordMock).not.toHaveBeenCalled();
	});

	it('A valid change sends the current and the new password for the logged-in user', async () => {
		// Given
		render(<AccountPage />);

		// When
		await fillPasswordForm('stare123', 'noweHaslo1', 'noweHaslo1');

		// Then
		await vi.waitFor(() =>
			expect(changePasswordMock).toHaveBeenCalledWith(
				'admin-1',
				'stare123',
				'noweHaslo1',
			),
		);
		expect(await screen.findByText('Hasło zostało zmienione.')).toBeInTheDocument();
	});

	it('A rejected password change reports the likely cause and shows no success, so the user is not misled', async () => {
		// Given - the server refuses (wrong current password)
		changePasswordMock.mockRejectedValue(new Error('400'));
		render(<AccountPage />);

		// When
		await fillPasswordForm('zle-stare', 'noweHaslo1', 'noweHaslo1');

		// Then
		expect(
			await screen.findByText(/Nie udało się zmienić hasła/),
		).toBeInTheDocument();
		expect(screen.queryByText('Hasło zostało zmienione.')).not.toBeInTheDocument();
	});

	it('The password fields are emptied after a success, so the old password does not sit in the form', async () => {
		// Given
		render(<AccountPage />);

		// When
		await fillPasswordForm('stare123', 'noweHaslo1', 'noweHaslo1');
		await screen.findByText('Hasło zostało zmienione.');

		// Then
		expect(screen.getByLabelText('Aktualne hasło')).toHaveValue('');
		expect(screen.getByLabelText('Nowe hasło')).toHaveValue('');
	});

	it('A malformed e-mail never reaches the server, so an unreachable address cannot be saved', async () => {
		// Given
		render(<AccountPage />);
		const input = screen.getByLabelText('Nowy adres email');

		// When
		await userEvent.type(input, 'nie-email');
		await userEvent.click(screen.getByRole('button', { name: /Zmień email/ }));

		// Then - the field is `type=email`, so the browser blocks the submit BEFORE the zod
		// resolver runs (jsdom does the same); the zod message is therefore the second line
		// of defence, and what matters here is that no request is made
		expect(input).toBeInvalid();
		expect(changeEmailMock).not.toHaveBeenCalled();
	});

	it('A changed e-mail is re-read from the server and put in the store, so the panel stops showing the old address', async () => {
		// Given
		changeEmailMock.mockResolvedValue(undefined);
		getMeMock.mockResolvedValue(currentUser({ email: 'nowy@photodrive.pl' }));
		render(<AccountPage />);

		// When
		await userEvent.type(
			screen.getByLabelText('Nowy adres email'),
			'nowy@photodrive.pl',
		);
		await userEvent.click(screen.getByRole('button', { name: /Zmień email/ }));

		// Then - without the re-read the header would keep the stale address until a reload
		await vi.waitFor(() =>
			expect(changeEmailMock).toHaveBeenCalledWith('admin-1', 'nowy@photodrive.pl'),
		);
		await vi.waitFor(() =>
			expect(usePanelAuthStore.getState().user?.email).toBe('nowy@photodrive.pl'),
		);
		expect(
			await screen.findByText('Adres email został zmieniony.'),
		).toBeInTheDocument();
	});

	it('A rejected e-mail change leaves the stored address untouched, so the panel never claims a change that failed', async () => {
		// Given
		changeEmailMock.mockRejectedValue(new Error('400'));
		render(<AccountPage />);

		// When
		await userEvent.type(
			screen.getByLabelText('Nowy adres email'),
			'zajety@photodrive.pl',
		);
		await userEvent.click(screen.getByRole('button', { name: /Zmień email/ }));

		// Then
		expect(
			await screen.findByText('Nie udało się zmienić adresu email.'),
		).toBeInTheDocument();
		expect(usePanelAuthStore.getState().user?.email).toBe('admin@photodrive.pl');
		expect(getMeMock).not.toHaveBeenCalled();
	});
});
