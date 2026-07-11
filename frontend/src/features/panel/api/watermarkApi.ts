import { apiClient } from '@/lib/apiClient';
import type { WatermarkStatusDto } from '@/shared/types/api';

/**
 * Jeden globalny znak wodny platformy: status widzi admin i fotograf (steruje
 * widocznością akcji watermark w UI), zarządza nim wyłącznie admin.
 */

export async function getWatermarkStatus(): Promise<WatermarkStatusDto> {
	const response = await apiClient.get<WatermarkStatusDto>('/watermark/status');
	return response.data;
}

/** URL podglądu loga (tylko admin); `version` = updatedAt jako cache-buster. */
export function getWatermarkImageUrl(version: string | null): string {
	return `/api/watermark${version ? `?v=${encodeURIComponent(version)}` : ''}`;
}

export async function uploadWatermark(file: File): Promise<void> {
	const formData = new FormData();
	formData.append('file', file);
	await apiClient.put('/watermark', formData, {
		headers: { 'Content-Type': 'multipart/form-data' },
	});
}

export async function deleteWatermark(): Promise<void> {
	await apiClient.delete('/watermark');
}
