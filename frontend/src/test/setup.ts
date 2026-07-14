import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Bez `globals: true` React Testing Library nie rejestruje sprzątania samo —
// robimy to jawnie, żeby DOM jednego testu nie wyciekał do następnego.
afterEach(() => {
	cleanup();
});
