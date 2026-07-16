import { apiClient } from '@/lib/apiClient';
import type { SiteSlotDto } from '@/shared/types/api';

/**
 * Sloty strony wizytówki: pojedyncze zdjęcia sekcji strony publicznej (hero, „o mnie"...).
 * Zarządza wyłącznie admin; strona publiczna czyta je przez /api/public/site.
 */

export async function getSiteSlots(): Promise<SiteSlotDto[]> {
	const response = await apiClient.get<SiteSlotDto[]>('/site/slots');
	return response.data;
}

export async function uploadSiteSlotImage(
	slot: string,
	file: File,
): Promise<void> {
	const formData = new FormData();
	formData.append('file', file);
	await apiClient.put(`/site/slots/${slot}`, formData, {
		headers: { 'Content-Type': 'multipart/form-data' },
	});
}

export async function deleteSiteSlotImage(slot: string): Promise<void> {
	await apiClient.delete(`/site/slots/${slot}`);
}

/**
 * Podgląd w panelu korzysta z publicznego URL-a (zdjęcie slotu jest z definicji publiczne);
 * `version` = updatedAt — serwer wysyła `immutable`, więc to zmiana URL-a odświeża podgląd.
 */
export function getSiteSlotPreviewUrl(slot: string, version: string): string {
	return `/api/public/site/photo/${slot}?v=${encodeURIComponent(version)}`;
}
