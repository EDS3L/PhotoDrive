import { apiClient } from '@/lib/apiClient';
import type { WatermarkStatusDto } from '@/shared/types/api';

export async function getWatermarkStatus(): Promise<WatermarkStatusDto> {
	const response = await apiClient.get<WatermarkStatusDto>('/watermark/status');
	return response.data;
}

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
