export function panelPathForRole(roles: string[]): string | null {
	if (roles.includes('ADMIN')) return '/admin';
	if (roles.includes('PHOTOGRAPHER')) return '/photographer';
	return null;
}

export function redirectNonClientToPanel(roles: string[]): boolean {
	const path = panelPathForRole(roles);
	if (path) {
		window.location.href = path;
		return true;
	}
	return false;
}
