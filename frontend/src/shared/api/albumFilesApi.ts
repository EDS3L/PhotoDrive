import { apiClient } from '@/lib/apiClient';

/**
 * Wspólne operacje na albumie i jego plikach — JEDNO źródło dla panelu (admin/fotograf)
 * i strefy klienta. Wcześniej te funkcje były przeklejone w trzech modułach API
 * (`adminApi`/`photographerApi`/`clientZoneApi`), co groziło rozjechaniem endpointów (F.2).
 * Moduły feature'owe re-eksportują stąd to, czego potrzebują, więc konsumenci importują
 * dalej ze swoich modułów — a implementacja żyje tylko tutaj.
 */

export async function getAlbumFileNames(albumId: string): Promise<string[]> {
	const response = await apiClient.get<string[]>(`/album/${albumId}/file-names`);
	return response.data;
}

export async function setAlbumTtd(albumId: string, ttd: string): Promise<void> {
	await apiClient.patch(`/album/${albumId}/setTtd`, { ttd });
}

export async function deleteAlbum(albumId: string): Promise<void> {
	await apiClient.delete(`/album/${albumId}/delete`);
}

export async function uploadFiles(
	albumId: string,
	files: File[],
	onProgress?: (percent: number) => void,
): Promise<void> {
	const formData = new FormData();
	files.forEach((file) => formData.append('files', file));
	await apiClient.post(`/album/upload/${albumId}/files`, formData, {
		headers: { 'Content-Type': 'multipart/form-data' },
		onUploadProgress: (e) => {
			if (!onProgress || !e.total) return;
			onProgress(Math.round((e.loaded / e.total) * 100));
		},
	});
}

export async function removeFiles(
	albumId: string,
	fileIds: string[],
): Promise<void> {
	await apiClient.post(`/album/${albumId}/remove`, {
		fileIdList: fileIds,
	});
}

export async function setFilesVisible(
	albumId: string,
	fileIds: string[],
	visible: boolean,
): Promise<void> {
	await apiClient.patch(
		`/album/${albumId}/files/setVisible`,
		{ idList: fileIds },
		{ params: { visible } },
	);
}

export async function addWatermark(
	albumId: string,
	fileIds: string[],
	hasWatermark: boolean,
): Promise<void> {
	await apiClient.post(
		`/album/${albumId}/files/addWatermark`,
		{ filesUUIDList: fileIds },
		{ params: { hasWatermark } },
	);
}

export async function swapFiles(
	sourceAlbumId: string,
	targetAlbumId: string,
	fileIds: string[],
): Promise<void> {
	await apiClient.patch(`/album/${sourceAlbumId}/album/${targetAlbumId}/swap`, {
		fileIdList: fileIds,
	});
}

export async function renameFile(
	albumId: string,
	fileId: string,
	newName: string,
): Promise<void> {
	await apiClient.put(`/album/${albumId}/rename/${fileId}`, {
		newFileName: newName,
	});
}

export function getPhotoUrl(
	albumId: string,
	fileName: string,
	width?: number,
	height?: number,
): string {
	const params = new URLSearchParams();
	if (width) params.set('width', String(width));
	if (height) params.set('height', String(height));
	const query = params.toString();
	return `/api/album/${albumId}/photo/${encodeURIComponent(fileName)}${query ? `?${query}` : ''}`;
}

export async function downloadAlbum(
	albumId: string,
	fileList: string[],
): Promise<Blob> {
	const response = await apiClient.post(
		`/album/${albumId}/download`,
		{ fileList },
		{ responseType: 'arraybuffer' },
	);
	return new Blob([response.data], { type: 'application/zip' });
}
