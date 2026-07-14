import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PanelLayout } from './PanelLayout';
import { usePanelAuthStore } from '../../store/panelAuthStore';
import type { CurrentUserInfo } from '../../types/panel';

const { usePanelMeMock } = vi.hoisted(() => ({ usePanelMeMock: vi.fn() }));

// Podmieniamy TYLKO zapytanie o sesję — reszta panelu (Sidebar, TopBar, routing) jest prawdziwa.
vi.mock('../../hooks/usePanelAuth', async (importOriginal) => {
	const actual = await importOriginal<typeof import('../../hooks/usePanelAuth')>();
	return { ...actual, usePanelMe: usePanelMeMock };
});

const admin: CurrentUserInfo = {
	id: 'u-1',
	name: 'Ala',
	email: 'ala@photodrive.dev',
	roles: ['ADMIN'],
	changePasswordOnNextLogin: false,
};

const photographer: CurrentUserInfo = {
	id: 'u-2',
	name: 'Bartek',
	email: 'bartek@photodrive.dev',
	roles: ['PHOTOGRAPHER'],
	changePasswordOnNextLogin: false,
};

/** `/user/me` w danym stanie — dokładnie to, czym PanelLayout się kieruje. */
function sessionQuery(
	state: Partial<{ data: CurrentUserInfo; isLoading: boolean; isError: boolean }>,
) {
	usePanelMeMock.mockReturnValue({
		data: state.data,
		isLoading: state.isLoading ?? false,
		isError: state.isError ?? false,
		refetch: vi.fn(),
	});
}

/** Prawdziwy routing panelu: admin i fotograf mają osobne bramki. */
function renderPanel(initialPath: string) {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false } },
	});
	render(
		<QueryClientProvider client={queryClient}>
			<MemoryRouter initialEntries={[initialPath]}>
				<Routes>
					<Route element={<PanelLayout requiredRole='ADMIN' />}>
						<Route path='/admin' element={<p>Panel administratora</p>} />
					</Route>
					<Route element={<PanelLayout requiredRole='PHOTOGRAPHER' />}>
						<Route path='/photographer' element={<p>Panel fotografa</p>} />
					</Route>
					<Route path='/panel-login' element={<p>Ekran logowania do panelu</p>} />
				</Routes>
			</MemoryRouter>
		</QueryClientProvider>,
	);
}

/**
 * PanelLayout jest jedyną bramką panelu: sprawdza sesję, wymuszoną zmianę hasła i rolę.
 * Każda z tych trzech decyzji ma tu swój test, bo pomyłka oznacza albo wyciek widoku
 * nie dla tej roli, albo wyrzucanie zalogowanego użytkownika na ekran logowania.
 */
describe('PanelLayout', () => {
	beforeEach(() => {
		usePanelAuthStore.getState().clear();
	});

	it('waits for the session check instead of redirecting to the login screen', () => {
		// Given - /user/me is still in flight (e.g. right after F5 on a subpage)
		sessionQuery({ isLoading: true });

		// When
		renderPanel('/admin');

		// Then - a premature redirect would drop the user off the page they reloaded (B.23)
		expect(screen.queryByText('Ekran logowania do panelu')).not.toBeInTheDocument();
		expect(screen.queryByText('Panel administratora')).not.toBeInTheDocument();
	});

	it('sends the user to the login screen when there is no valid session', () => {
		// Given - the cookie expired or was never there
		sessionQuery({ isError: true });

		// When
		renderPanel('/admin');

		// Then
		expect(screen.getByText('Ekran logowania do panelu')).toBeInTheDocument();
	});

	it('blocks the whole panel until the starting password is changed', () => {
		// Given - a freshly created account, still on the generated password
		sessionQuery({ data: { ...admin, changePasswordOnNextLogin: true } });

		// When - the admin opens a route their role fully allows
		renderPanel('/admin');

		// Then - they get the password screen instead of the panel
		expect(screen.getByRole('heading', { name: 'Ustaw nowe hasło' })).toBeInTheDocument();
		expect(screen.queryByText('Panel administratora')).not.toBeInTheDocument();
	});

	it('keeps a photographer out of the admin routes', () => {
		// Given - a photographer session
		sessionQuery({ data: photographer });

		// When - they land on an admin-only route (typed URL, stale bookmark)
		renderPanel('/admin');

		// Then - they are moved to their own panel instead of seeing the admin view
		expect(screen.getByText('Panel fotografa')).toBeInTheDocument();
		expect(screen.queryByText('Panel administratora')).not.toBeInTheDocument();
	});

	it('renders the route for a user whose role matches', () => {
		// Given
		sessionQuery({ data: admin });

		// When
		renderPanel('/admin');

		// Then - the panel chrome and the route content are both there
		expect(screen.getByText('Panel administratora')).toBeInTheDocument();
		expect(screen.getByRole('button', { name: /wyloguj/i })).toBeInTheDocument();
	});
});
