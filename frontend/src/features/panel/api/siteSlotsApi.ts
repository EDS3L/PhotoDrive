import { apiClient } from '@/lib/apiClient';
import type { SiteSlotDto } from '@/shared/types/api';

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

export function getSiteSlotPreviewUrl(slot: string, version: string): string {
	return `/api/public/site/photo/${slot}?v=${encodeURIComponent(version)}`;
}
