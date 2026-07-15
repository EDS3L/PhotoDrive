/**
 * Strefa klienta jest dla roli CLIENT. Admin i fotograf logują się TYM SAMYM endpointem
 * (`/auth/login` ustawia cookie każdej roli), więc bez tego wpadaliby do strefy klienta —
 * admin zobaczyłby błąd 403 na liście albumów, fotograf swoje własne albumy. Zamiast tego
 * odsyłamy ich do właściwego panelu (B.8/A7). Panel i tak sam sprawdzi sesję (`PanelLayout`
 * woła `/user/me`), więc pełna nawigacja jest bezpieczna.
 */
export function panelPathForRole(roles: string[]): string | null {
	if (roles.includes('ADMIN')) return '/admin';
	if (roles.includes('PHOTOGRAPHER')) return '/photographer';
	return null;
}

/**
 * Jeśli rola należy do panelu — przekierowuje tam i zwraca `true` (wołający ma NIE ustawiać
 * sesji klienta). Dla CLIENT nic nie robi i zwraca `false`.
 */
export function redirectNonClientToPanel(roles: string[]): boolean {
	const path = panelPathForRole(roles);
	if (path) {
		window.location.href = path;
		return true;
	}
	return false;
}
