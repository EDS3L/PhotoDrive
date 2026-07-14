import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AxiosError } from 'axios';
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { ForcePasswordChange } from './ForcePasswordChange';

function renderScreen(props: Partial<Parameters<typeof ForcePasswordChange>[0]> = {}) {
	const changePassword = props.changePassword ?? vi.fn().mockResolvedValue(undefined);
	const onDone = props.onDone ?? vi.fn();
	render(
		<ForcePasswordChange
			userId='u-1'
			changePassword={changePassword}
			onDone={onDone}
			presetCurrentPassword={props.presetCurrentPassword}
		/>,
	);
	return { changePassword, onDone, user: userEvent.setup() };
}

const submitButton = () => screen.getByRole('button', { name: /zmień hasło/i });

/**
 * Bramka pierwszego logowania: dopóki `/user/me` zwraca `changePasswordOnNextLogin`,
 * użytkownik nie zobaczy nic poza tym ekranem. Testy pilnują, żeby (a) nie dało się
 * przez niego przejść byle czym i (b) dało się przez niego przejść naprawdę.
 */
describe('ForcePasswordChange', () => {
	it('rejects a password shorter than the required minimum, without asking the backend', async () => {
		// Given - the password from the welcome e-mail is already in memory
		const { changePassword, user } = renderScreen({ presetCurrentPassword: 'startowe123' });

		// When - a 7-character password is submitted
		await user.type(screen.getByLabelText('Nowe hasło'), 'krotki');
		await user.type(screen.getByLabelText('Potwierdź nowe hasło'), 'krotki');
		await user.click(submitButton());

		// Then - the rule is explained on the spot and no request is wasted
		expect(await screen.findByText('Nowe hasło musi mieć min. 8 znaków.')).toBeInTheDocument();
		expect(changePassword).not.toHaveBeenCalled();
	});

	it('rejects a confirmation that does not match, so a typo cannot lock the account', async () => {
		// Given
		const { changePassword, user } = renderScreen({ presetCurrentPassword: 'startowe123' });

		// When
		await user.type(screen.getByLabelText('Nowe hasło'), 'nowehaslo123');
		await user.type(screen.getByLabelText('Potwierdź nowe hasło'), 'nowehaslo124');
		await user.click(submitButton());

		// Then - a mismatched password would leave the user unable to log in again
		expect(await screen.findByText('Hasła nie są takie same.')).toBeInTheDocument();
		expect(changePassword).not.toHaveBeenCalled();
	});

	it('reuses the password from login instead of asking for it again', async () => {
		// Given - the user has just logged in with the generated password
		const { changePassword, onDone, user } = renderScreen({
			presetCurrentPassword: 'startowe123',
		});

		// Then - the "starting password" field is not even shown
		expect(screen.queryByLabelText('Hasło startowe (z maila)')).not.toBeInTheDocument();

		// When
		await user.type(screen.getByLabelText('Nowe hasło'), 'nowehaslo123');
		await user.type(screen.getByLabelText('Potwierdź nowe hasło'), 'nowehaslo123');
		await user.click(submitButton());

		// Then - the change goes through with the remembered password
		await waitFor(() =>
			expect(changePassword).toHaveBeenCalledWith('u-1', 'startowe123', 'nowehaslo123'),
		);
		expect(onDone).toHaveBeenCalledOnce();
	});

	it('asks for the starting password when it is no longer in memory after a page reload', async () => {
		// Given - after F5 the session is restored from the cookie, but the password is gone
		const { changePassword, user } = renderScreen();

		// When - the user retypes it from the e-mail
		await user.type(screen.getByLabelText('Hasło startowe (z maila)'), 'startowe123');
		await user.type(screen.getByLabelText('Nowe hasło'), 'nowehaslo123');
		await user.type(screen.getByLabelText('Potwierdź nowe hasło'), 'nowehaslo123');
		await user.click(submitButton());

		// Then - the gate survives a reload instead of dead-ending the user
		await waitFor(() =>
			expect(changePassword).toHaveBeenCalledWith('u-1', 'startowe123', 'nowehaslo123'),
		);
	});

	it('shows the reason from the backend and keeps the user on the screen', async () => {
		// Given - the backend refuses to set the same password again
		const config = { headers: {} } as InternalAxiosRequestConfig;
		const rejected = new AxiosError('Request failed', 'ERR_BAD_REQUEST', config, undefined, {
			status: 400,
			data: { message: 'Nowe hasło nie może być takie samo jak dotychczasowe.' },
			statusText: '',
			headers: {},
			config,
		} as AxiosResponse);
		const changePassword = vi.fn().mockRejectedValue(rejected);
		const { onDone, user } = renderScreen({
			presetCurrentPassword: 'startowe123',
			changePassword,
		});

		// When
		await user.type(screen.getByLabelText('Nowe hasło'), 'startowe123');
		await user.type(screen.getByLabelText('Potwierdź nowe hasło'), 'startowe123');
		await user.click(submitButton());

		// Then - the user learns what to fix and is NOT let into the app
		expect(
			await screen.findByText('Nowe hasło nie może być takie samo jak dotychczasowe.'),
		).toBeInTheDocument();
		expect(onDone).not.toHaveBeenCalled();
	});

	it('keeps the submit button disabled until every field is filled in', () => {
		// Given - a freshly opened screen after a reload
		renderScreen();

		// When / Then - nothing to submit yet
		expect(submitButton()).toBeDisabled();
	});
});
