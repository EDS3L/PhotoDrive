import { useQuery } from '@tanstack/react-query';
import {
	getPublicSiteSlots,
	getSiteSlotPhotoUrl,
	type SiteSlotKey,
} from '../../lib/publicApi';

export type SiteSlotUrls = Partial<Record<SiteSlotKey, string>>;

/**
 * Zdjęcia slotów strony wizytówki — JEDEN request na całą stronę (wszystkie sekcje
 * czytają ten sam cache RQ), zamiast osobnego zapytania per sekcja jak przy magicznych
 * albumach. Slot bez zdjęcia jest w mapie nieobecny — sekcja pokazuje swój placeholder.
 */
export function useSiteSlots() {
	return useQuery<SiteSlotUrls>({
		queryKey: ['public-site-slots'],
		queryFn: async () => {
			const slots = await getPublicSiteSlots();
			const urls: SiteSlotUrls = {};
			for (const { slot, version } of slots) {
				urls[slot] = getSiteSlotPhotoUrl(slot, version);
			}
			return urls;
		},
		// Krótki cache, żeby podmiana zdjęcia w panelu pojawiła się na stronie w ~pół minuty.
		staleTime: 30 * 1000,
	});
}
