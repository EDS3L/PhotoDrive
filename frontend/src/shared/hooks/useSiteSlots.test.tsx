import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { ReactNode } from 'react';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useSiteSlots } from './useSiteSlots';
import type { getPublicSiteSlots } from '@/lib/publicApi';

const { getPublicSiteSlotsMock } = vi.hoisted(() => ({
	getPublicSiteSlotsMock: vi.fn<typeof getPublicSiteSlots>(),
}));

// Podmieniamy TYLKO listing — budowa URL-a (getSiteSlotPhotoUrl) zostaje prawdziwa,
// bo to właśnie jej kontrakt (wersja w URL-u) jest tu regułą pod testem.
vi.mock('@/lib/publicApi', async (importOriginal) => {
	const actual = await importOriginal<typeof import('@/lib/publicApi')>();
	return { ...actual, getPublicSiteSlots: getPublicSiteSlotsMock };
});

function renderSlots() {
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false } },
	});
	const wrapper = ({ children }: { children: ReactNode }) => (
		<QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
	);
	return renderHook(() => useSiteSlots(), { wrapper });
}

describe('useSiteSlots', () => {
	beforeEach(() => {
		getPublicSiteSlotsMock.mockReset();
	});

	it('Every configured slot resolves to a URL carrying its version, so replacing a photo busts the immutable browser cache', async () => {
		// Given - two configured slots with distinct versions
		getPublicSiteSlotsMock.mockResolvedValue([
			{ slot: 'HOME_HERO', version: 111 },
			{ slot: 'ABOUT_BIO', version: 222 },
		]);

		// When
		const { result } = renderSlots();
		await waitFor(() => expect(result.current.isSuccess).toBe(true));

		// Then - the version is part of the URL (the server marks the image immutable)
		expect(result.current.data?.HOME_HERO).toBe(
			'/api/public/site/photo/HOME_HERO?v=111',
		);
		expect(result.current.data?.ABOUT_BIO).toBe(
			'/api/public/site/photo/ABOUT_BIO?v=222',
		);
	});

	it('A slot with no photo is absent from the map, so its section falls back to the placeholder instead of a broken img', async () => {
		// Given - the backend lists only slots that actually have a photo
		getPublicSiteSlotsMock.mockResolvedValue([
			{ slot: 'HOME_HERO', version: 111 },
		]);

		// When
		const { result } = renderSlots();
		await waitFor(() => expect(result.current.isSuccess).toBe(true));

		// Then - `slots?.HOME_CTA ?? placeholder(...)` in the section must hit the placeholder branch
		expect(result.current.data?.HOME_CTA).toBeUndefined();
	});
});
