import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AdminUsers from './AdminUsers';
import type {
	getAllUsers,
	createUser,
	activateUser,
	deactivateUser,
	assignUsersToPhotographer,
	removeUsersFromPhotographer,
	getPhotographerAssignedUsers,
} from '../../api/adminApi';
import type { UserInfo } from '../../types/panel';

const {
	getAllUsersMock,
	createUserMock,
	activateMock,
	deactivateMock,
	assignMock,
	removeMock,
	assignedUsersMock,
} = vi.hoisted(() => ({
	getAllUsersMock: vi.fn<typeof getAllUsers>(),
	createUserMock: vi.fn<typeof createUser>(),
	activateMock: vi.fn<typeof activateUser>(),
	deactivateMock: vi.fn<typeof deactivateUser>(),
	assignMock: vi.fn<typeof assignUsersToPhotographer>(),
	removeMock: vi.fn<typeof removeUsersFromPhotographer>(),
	assignedUsersMock: vi.fn<typeof getPhotographerAssignedUsers>(),
}));

// Podmieniamy TYLKO wywołania HTTP — hooki RQ, filtry i logika strony zostają prawdziwe.
vi.mock('../../api/adminApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('../../api/adminApi')>();
	return {
		...actual,
		getAllUsers: getAllUsersMock,
		createUser: createUserMock,
		activateUser: activateMock,
		deactivateUser: deactivateMock,
		assignUsersToPhotographer: assignMock,
		removeUsersFromPhotographer: removeMock,
		getPhotographerAssignedUsers: assignedUsersMock,
	};
});

function user(overrides: Partial<UserInfo>): UserInfo {
	return {
		id: 'u1',
		name: 'Jan Kowalski',
		email: 'jan@example.com',
		roles: ['CLIENT'],
		isActive: true,
		changePasswordOnNextLogin: false,
		assignedUsers: [],
		...overrides,
	};
}

function renderPage() {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
	});
	return render(
		<QueryClientProvider client={queryClient}>
			<AdminUsers />
		</QueryClientProvider>,
	);
}

/** Otwiera szczegóły użytkownika (klik w wiersz) i zwraca modal. */
async function openDetail(name: string): Promise<HTMLElement> {
	await userEvent.click(await screen.findByText(name));
	return screen.getByRole('dialog', { name });
}

/**
 * Modal przypisywania otwiera się NAD modalem szczegółów, więc w DOM-ie są wtedy dwa
 * dialogi — bierzemy ten po nazwie dostępnej (Modal ustawia `aria-labelledby`).
 */
async function openAssignDialog(photographerName: string): Promise<HTMLElement> {
	await openDetail(photographerName);
	await userEvent.click(await screen.findByRole('button', { name: /Przypisz/ }));
	return screen.getByRole('dialog', { name: 'Przypisz klientów' });
}

describe('AdminUsers', () => {
	beforeEach(() => {
		getAllUsersMock.mockReset().mockResolvedValue([]);
		createUserMock.mockReset();
		activateMock.mockReset().mockResolvedValue(undefined);
		deactivateMock.mockReset().mockResolvedValue(undefined);
		assignMock.mockReset().mockResolvedValue(undefined);
		removeMock.mockReset().mockResolvedValue(undefined);
		assignedUsersMock.mockReset().mockResolvedValue([]);
	});

	it('The role filter matches the role a user actually holds, so a photographer never appears under clients', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'u1', name: 'Foto Graf', roles: ['PHOTOGRAPHER'] }),
			user({ id: 'u2', name: 'Klient Testowy', roles: ['CLIENT'] }),
			user({ id: 'u3', name: 'Admin Glowny', roles: ['ADMIN'] }),
		]);
		renderPage();
		await screen.findByText('Foto Graf');

		// When
		await userEvent.selectOptions(
			screen.getAllByRole('combobox')[0],
			'PHOTOGRAPHER',
		);

		// Then
		expect(screen.getByText('Foto Graf')).toBeInTheDocument();
		expect(screen.queryByText('Klient Testowy')).not.toBeInTheDocument();
		expect(screen.queryByText('Admin Glowny')).not.toBeInTheDocument();
	});

	it('The status filter splits users into active and inactive with nothing left over', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'u1', name: 'Aktywny Jan', isActive: true }),
			user({ id: 'u2', name: 'Zablokowany Piotr', isActive: false }),
		]);
		renderPage();
		await screen.findByText('Aktywny Jan');
		const statusSelect = screen.getAllByRole('combobox')[1];

		// When / Then
		await userEvent.selectOptions(statusSelect, 'ACTIVE');
		expect(screen.getByText('Aktywny Jan')).toBeInTheDocument();
		expect(screen.queryByText('Zablokowany Piotr')).not.toBeInTheDocument();

		await userEvent.selectOptions(statusSelect, 'INACTIVE');
		expect(screen.getByText('Zablokowany Piotr')).toBeInTheDocument();
		expect(screen.queryByText('Aktywny Jan')).not.toBeInTheDocument();
	});

	it('The counter reflects the filtered result, not the total, so it describes what is on screen', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'u1', name: 'Jan Kowalski', roles: ['CLIENT'] }),
			user({ id: 'u2', name: 'Foto Graf', roles: ['PHOTOGRAPHER'] }),
		]);
		renderPage();
		expect(await screen.findByText('2 użytkowników')).toBeInTheDocument();

		// When
		await userEvent.selectOptions(screen.getAllByRole('combobox')[0], 'CLIENT');

		// Then
		expect(screen.getByText('1 użytkowników')).toBeInTheDocument();
	});

	it('Search covers the e-mail too, because the admin usually gets the address and not the name', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'u1', name: 'Jan Kowalski', email: 'jan@firma.pl' }),
			user({ id: 'u2', name: 'Anna Nowak', email: 'anna@example.com' }),
		]);
		renderPage();
		await screen.findByText('Jan Kowalski');

		// When
		await userEvent.type(
			screen.getByPlaceholderText('Szukaj użytkownika...'),
			'firma',
		);

		// Then
		expect(screen.getByText('Jan Kowalski')).toBeInTheDocument();
		expect(screen.queryByText('Anna Nowak')).not.toBeInTheDocument();
	});

	it('Deactivating an active user calls the deactivate endpoint, and activating an inactive one calls activate — they are not one toggle', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'u1', name: 'Aktywny Jan', isActive: true }),
			user({ id: 'u2', name: 'Zablokowany Piotr', isActive: false }),
		]);
		renderPage();
		await screen.findByText('Aktywny Jan');

		// When
		await userEvent.click(screen.getByRole('button', { name: 'Dezaktywuj' }));
		await userEvent.click(screen.getByRole('button', { name: 'Aktywuj' }));

		// Then - the backend exposes two endpoints; calling the wrong one silently does
		// the opposite of what the admin clicked
		await vi.waitFor(() => {
			expect(deactivateMock).toHaveBeenCalledWith('u1', false);
			expect(activateMock).toHaveBeenCalledWith('u2', true);
		});
	});

	it('Clicking the row action does not open the details, so a single click cannot both toggle and navigate', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'u1', name: 'Aktywny Jan', isActive: true }),
		]);
		renderPage();
		await screen.findByText('Aktywny Jan');

		// When
		await userEvent.click(screen.getByRole('button', { name: 'Dezaktywuj' }));

		// Then - without stopPropagation the row handler would fire as well
		expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
	});

	it('Clicking a row opens that user\'s details', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'u1', name: 'Jan Kowalski', email: 'jan@example.com' }),
		]);
		renderPage();

		// When
		const dialog = await openDetail('Jan Kowalski');

		// Then
		expect(within(dialog).getByText('jan@example.com')).toBeInTheDocument();
	});

	it('Assigned clients are shown only for a photographer, because only a photographer can have any', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'u1', name: 'Klient Testowy', roles: ['CLIENT'] }),
		]);
		renderPage();

		// When
		const dialog = await openDetail('Klient Testowy');

		// Then - and the assigned-clients query is never fired for a non-photographer
		expect(within(dialog).queryByText(/Przypisani klienci/)).not.toBeInTheDocument();
		expect(assignedUsersMock).not.toHaveBeenCalled();
	});

	it("A photographer's details list their clients and fetch them by that photographer's id", async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'foto-1', name: 'Foto Graf', roles: ['PHOTOGRAPHER'] }),
		]);
		assignedUsersMock.mockResolvedValue([
			user({ id: 'k1', name: 'Anna Klientka', email: 'anna@example.com' }),
		]);
		renderPage();

		// When
		await openDetail('Foto Graf');

		// Then
		expect(await screen.findByText('Anna Klientka')).toBeInTheDocument();
		expect(assignedUsersMock).toHaveBeenCalledWith('foto-1');
		expect(screen.getByText(/Przypisani klienci \(1\)/)).toBeInTheDocument();
	});

	it('The assign dialog offers only active clients that are not assigned yet, so nobody is offered twice or while blocked', async () => {
		// Given - one already assigned, one inactive, one photographer, one valid candidate
		getAllUsersMock.mockResolvedValue([
			user({ id: 'foto-1', name: 'Foto Graf', roles: ['PHOTOGRAPHER'] }),
			user({ id: 'k1', name: 'Juz Przypisany', roles: ['CLIENT'] }),
			user({ id: 'k2', name: 'Nieaktywny Klient', roles: ['CLIENT'], isActive: false }),
			user({ id: 'k3', name: 'Wolny Klient', roles: ['CLIENT'] }),
			user({ id: 'foto-2', name: 'Inny Fotograf', roles: ['PHOTOGRAPHER'] }),
		]);
		assignedUsersMock.mockResolvedValue([user({ id: 'k1', name: 'Juz Przypisany' })]);
		renderPage();

		// When
		const dialog = await openAssignDialog('Foto Graf');

		// Then - assigning an inactive account or a second photographer is a backend 400;
		// offering it in the UI turns a rule into a mystery error
		expect(within(dialog).getByText('Wolny Klient')).toBeInTheDocument();
		expect(within(dialog).queryByText('Nieaktywny Klient')).not.toBeInTheDocument();
		expect(within(dialog).queryByText('Inny Fotograf')).not.toBeInTheDocument();
		expect(within(dialog).queryByLabelText('Juz Przypisany')).not.toBeInTheDocument();
	});

	it('Assignment stays disabled until at least one client is ticked, because an empty assignment is a pointless request', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'foto-1', name: 'Foto Graf', roles: ['PHOTOGRAPHER'] }),
			user({ id: 'k3', name: 'Wolny Klient', roles: ['CLIENT'] }),
		]);
		renderPage();
		const dialog = await openAssignDialog('Foto Graf');
		const submit = within(dialog).getByRole('button', { name: /Przypisz \(/ });

		// Then
		expect(submit).toBeDisabled();

		// When
		await userEvent.click(within(dialog).getByRole('checkbox'));

		// Then
		expect(submit).toBeEnabled();
	});

	it('Ticked clients are sent together in one call, so assigning five people is one request and not five', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'foto-1', name: 'Foto Graf', roles: ['PHOTOGRAPHER'] }),
			user({ id: 'k1', name: 'Klient Pierwszy', roles: ['CLIENT'] }),
			user({ id: 'k2', name: 'Klient Drugi', roles: ['CLIENT'] }),
		]);
		renderPage();
		const dialog = await openAssignDialog('Foto Graf');

		// When
		const boxes = within(dialog).getAllByRole('checkbox');
		await userEvent.click(boxes[0]);
		await userEvent.click(boxes[1]);
		await userEvent.click(
			within(dialog).getByRole('button', { name: /Przypisz \(/ }),
		);

		// Then
		await vi.waitFor(() =>
			expect(assignMock).toHaveBeenCalledWith('foto-1', ['k1', 'k2']),
		);
	});

	it('The assign dialog can be searched by name or e-mail, because a long client list is unusable without it (B.10)', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'foto-1', name: 'Foto Graf', roles: ['PHOTOGRAPHER'] }),
			user({ id: 'k1', name: 'Anna Kowalska', email: 'anna@example.com', roles: ['CLIENT'] }),
			user({ id: 'k2', name: 'Piotr Nowak', email: 'piotr@firma.pl', roles: ['CLIENT'] }),
		]);
		renderPage();
		const dialog = await openAssignDialog('Foto Graf');

		// When
		await userEvent.type(
			within(dialog).getByPlaceholderText('Szukaj po nazwie lub e-mailu...'),
			'firma',
		);

		// Then
		expect(within(dialog).getByText('Piotr Nowak')).toBeInTheDocument();
		expect(within(dialog).queryByText('Anna Kowalska')).not.toBeInTheDocument();
	});

	it('A search matching nobody says so, instead of silently showing an empty box', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'foto-1', name: 'Foto Graf', roles: ['PHOTOGRAPHER'] }),
			user({ id: 'k1', name: 'Anna Kowalska', roles: ['CLIENT'] }),
		]);
		renderPage();
		const dialog = await openAssignDialog('Foto Graf');

		// When
		await userEvent.type(
			within(dialog).getByPlaceholderText('Szukaj po nazwie lub e-mailu...'),
			'zzz',
		);

		// Then
		expect(within(dialog).getByText(/Brak klientów pasujących/)).toBeInTheDocument();
	});

	it('Detaching a client sends just that one client, so the other assignments survive', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'foto-1', name: 'Foto Graf', roles: ['PHOTOGRAPHER'] }),
		]);
		assignedUsersMock.mockResolvedValue([
			user({ id: 'k1', name: 'Anna Klientka' }),
			user({ id: 'k2', name: 'Piotr Klient' }),
		]);
		renderPage();
		await openDetail('Foto Graf');
		await screen.findByText('Anna Klientka');

		// When - the unlink button of the FIRST client
		const row = screen.getByText('Anna Klientka').closest('div.flex')!;
		await userEvent.click(within(row as HTMLElement).getByRole('button'));

		// Then
		await vi.waitFor(() =>
			expect(removeMock).toHaveBeenCalledWith('foto-1', ['k1']),
		);
	});

	it('A new account defaults to the client role, because clients are the common case and the choice is explicit for photographers', async () => {
		// Given
		createUserMock.mockResolvedValue(user({ id: 'nowy' }));
		renderPage();
		await userEvent.click(
			await screen.findByRole('button', { name: /Dodaj użytkownika/ }),
		);
		const dialog = screen.getByRole('dialog');

		// When - the role is not touched
		await userEvent.type(within(dialog).getByLabelText('Imię i Nazwisko'), 'Jan');
		await userEvent.type(within(dialog).getByLabelText('Email'), 'jan@example.com');
		await userEvent.click(within(dialog).getByRole('button', { name: /Utwórz konto/ }));

		// Then
		await vi.waitFor(() => expect(createUserMock).toHaveBeenCalled());
		expect(createUserMock.mock.calls[0][0]).toEqual({
			name: 'Jan',
			email: 'jan@example.com',
			role: 'CLIENT',
		});
	});

	it('Picking the photographer tile creates a photographer, so the role tiles are not decoration', async () => {
		// Given
		createUserMock.mockResolvedValue(user({ id: 'nowy' }));
		renderPage();
		await userEvent.click(
			await screen.findByRole('button', { name: /Dodaj użytkownika/ }),
		);
		const dialog = screen.getByRole('dialog');

		// When
		await userEvent.type(within(dialog).getByLabelText('Imię i Nazwisko'), 'Foto');
		await userEvent.type(within(dialog).getByLabelText('Email'), 'foto@example.com');
		await userEvent.click(within(dialog).getByRole('button', { name: /Fotograf/ }));
		await userEvent.click(within(dialog).getByRole('button', { name: /Utwórz konto/ }));

		// Then
		await vi.waitFor(() => expect(createUserMock).toHaveBeenCalled());
		expect(createUserMock.mock.calls[0][0].role).toBe('PHOTOGRAPHER');
	});

	it('Creating an account never sends a password, because the server generates and mails it (A5)', async () => {
		// Given
		createUserMock.mockResolvedValue(user({ id: 'nowy' }));
		renderPage();
		await userEvent.click(
			await screen.findByRole('button', { name: /Dodaj użytkownika/ }),
		);
		const dialog = screen.getByRole('dialog');

		// Then - no password field is even offered
		expect(within(dialog).queryByLabelText(/Hasło/)).not.toBeInTheDocument();

		// When
		await userEvent.type(within(dialog).getByLabelText('Imię i Nazwisko'), 'Jan');
		await userEvent.type(within(dialog).getByLabelText('Email'), 'jan@example.com');
		await userEvent.click(within(dialog).getByRole('button', { name: /Utwórz konto/ }));

		// Then
		await vi.waitFor(() => expect(createUserMock).toHaveBeenCalled());
		expect(createUserMock.mock.calls[0][0]).not.toHaveProperty('password');
	});

	it('Creating stays blocked until both a name and an e-mail are given', async () => {
		// Given
		renderPage();
		await userEvent.click(
			await screen.findByRole('button', { name: /Dodaj użytkownika/ }),
		);
		const dialog = screen.getByRole('dialog');
		const submit = within(dialog).getByRole('button', { name: /Utwórz konto/ });

		// Then
		expect(submit).toBeDisabled();

		// When
		await userEvent.type(within(dialog).getByLabelText('Imię i Nazwisko'), 'Jan');
		expect(submit).toBeDisabled();
		await userEvent.type(within(dialog).getByLabelText('Email'), 'jan@example.com');

		// Then
		expect(submit).toBeEnabled();
	});

	it('A filter matching nobody explains itself, so an empty table is never mistaken for a broken page', async () => {
		// Given
		getAllUsersMock.mockResolvedValue([
			user({ id: 'u1', name: 'Jan Kowalski', roles: ['CLIENT'] }),
		]);
		renderPage();
		await screen.findByText('Jan Kowalski');

		// When
		await userEvent.selectOptions(screen.getAllByRole('combobox')[0], 'ADMIN');

		// Then
		expect(screen.getByText('Brak użytkowników')).toBeInTheDocument();
	});
});
