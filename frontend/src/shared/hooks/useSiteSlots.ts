import { useQuery } from '@tanstack/react-query';
import {
	getPublicSiteSlots,
	getSiteSlotPhotoUrl,
	type SiteSlotKey,
} from '../../lib/publicApi';

export type SiteSlotUrls = Partial<Record<SiteSlotKey, string>>;

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
		staleTime: 30 * 1000,
	});
}
