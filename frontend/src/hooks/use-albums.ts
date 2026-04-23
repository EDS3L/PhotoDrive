import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { api } from '@/lib/api';
import type {
	AlbumDto,
	AlbumResponse,
	CreateAlbumRequest,
	CreateClientAlbumRequest,
	ChangeVisibleRequest,
	ChangeWatermarkRequest,
	SwapFileRequest,
	DownloadFilesRequest,
	RenameFileRequest,
	RemoveFilesRequest,
} from '@/types/album';
import { normalizeAlbum } from '@/types/album';
import type { UploadResponse } from '@/types/file';

const publicApi = axios.create({
	baseURL: import.meta.env.VITE_API_URL ?? '/api',
});

// ── Queries ──

export function useAlbums() {
	return useQuery({
		queryKey: ['albums'],
		queryFn: async () => {
			const res = await api.get<AlbumDto[]>('/album/all');
			return res.data.map(normalizeAlbum);
		},
	});
}

export function useAssignedAlbums() {
	return useQuery({
		queryKey: ['albums', 'assigned'],
		queryFn: async () => {
			const res = await api.get<AlbumDto[]>('/album/getAllAssignedAlbums');
			return res.data.map(normalizeAlbum);
		},
	});
}

export function useAlbumsWithoutTtd() {
	return useQuery({
		queryKey: ['albums', 'withoutTtd'],
		queryFn: async () => {
			const res = await api.get<AlbumDto[]>('/album/all/withoutTdd');
			return res.data.map(normalizeAlbum);
		},
	});
}

export function useAssignedAlbumsWithoutTtd() {
	return useQuery({
		queryKey: ['albums', 'assigned', 'withoutTtd'],
		queryFn: async () => {
			const res = await api.get<AlbumDto[]>(
				'/album/allAssignedAlbum/withoutTdd',
			);
			return res.data.map(normalizeAlbum);
		},
	});
}

export function useAlbumPhotos(
	albumId: string | undefined,
	options?: { width?: number; height?: number; showOnlyVisible?: boolean },
) {
	return useQuery({
		queryKey: ['albums', albumId, 'photos', options],
		queryFn: async () => {
			const params = new URLSearchParams();
			if (options?.width) params.set('width', String(options.width));
			if (options?.height) params.set('height', String(options.height));
			if (options?.showOnlyVisible) params.set('showOnlyVisable', 'true');
			const res = await api.get<string[]>(
				`/album/${albumId}/file/url/all?${params}`,
			);
			return res.data;
		},
		enabled: !!albumId,
	});
}

// ── Mutations ──

export function useCreateAdminAlbum() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: async (data: CreateAlbumRequest) => {
			const res = await api.post<AlbumResponse>('/album/admin/create', data);
			return res.data;
		},
		onSuccess: () => qc.invalidateQueries({ queryKey: ['albums'] }),
	});
}

export function useCreateClientAlbum() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: async ({
			clientId,
			...data
		}: CreateClientAlbumRequest & { clientId: string }) => {
			const res = await api.post<AlbumResponse>(
				`/album/client/${clientId}/create`,
				data,
			);
			return res.data;
		},
		onSuccess: () => qc.invalidateQueries({ queryKey: ['albums'] }),
	});
}

export function useUploadFiles() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: async ({
			albumId,
			files,
		}: {
			albumId: string;
			files: File[];
		}) => {
			const formData = new FormData();
			files.forEach((f) => formData.append('files', f));
			const res = await api.post<UploadResponse>(
				`/album/upload/${albumId}/files`,
				formData,
				{
					headers: { 'Content-Type': 'multipart/form-data' },
				},
			);
			return res.data;
		},
		onSuccess: (_data, { albumId }) =>
			qc.invalidateQueries({ queryKey: ['albums', albumId] }),
	});
}

export function useDeleteAlbum() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: (albumId: string) => api.delete(`/album/${albumId}/delete`),
		onSuccess: () => qc.invalidateQueries({ queryKey: ['albums'] }),
	});
}

export function useDownloadFiles() {
	return useMutation({
		mutationFn: async ({
			albumId,
			...data
		}: DownloadFilesRequest & { albumId: string }) => {
			const res = await api.post(`/album/${albumId}/download`, data, {
				responseType: 'blob',
			});
			const url = window.URL.createObjectURL(new Blob([res.data]));
			const a = document.createElement('a');
			a.href = url;
			a.download = `${albumId}.zip`;
			a.click();
			window.URL.revokeObjectURL(url);
		},
	});
}

export function useRenameFile() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({
			albumId,
			fileId,
			...data
		}: RenameFileRequest & { albumId: string; fileId: string }) =>
			api.put(`/album/${albumId}/rename/${fileId}`, data),
		onSuccess: (_data, { albumId }) =>
			qc.invalidateQueries({ queryKey: ['albums', albumId] }),
	});
}

export function useRemoveFiles() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({
			albumId,
			...data
		}: RemoveFilesRequest & { albumId: string }) =>
			api.post(`/album/${albumId}/remove`, data),
		onSuccess: (_data, { albumId }) =>
			qc.invalidateQueries({ queryKey: ['albums', albumId] }),
	});
}

export function useSetTtd() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({ albumId, ttd }: { albumId: string; ttd: string }) =>
			api.patch(`/album/${albumId}/setTtd?ttd=${encodeURIComponent(ttd)}`),
		onSuccess: () => qc.invalidateQueries({ queryKey: ['albums'] }),
	});
}

export function useChangeVisibility() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({
			albumId,
			visible,
			...data
		}: ChangeVisibleRequest & { albumId: string; visible: boolean }) =>
			api.patch(`/album/${albumId}/files/setVisible?visible=${visible}`, data),
		onSuccess: (_data, { albumId }) =>
			qc.invalidateQueries({ queryKey: ['albums', albumId] }),
	});
}

export function useAddWatermark() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({
			albumId,
			hasWatermark,
			...data
		}: ChangeWatermarkRequest & { albumId: string; hasWatermark: boolean }) =>
			api.post(
				`/album/${albumId}/files/addWatermark?hasWatermark=${hasWatermark}`,
				data,
			),
		onSuccess: (_data, { albumId }) =>
			qc.invalidateQueries({ queryKey: ['albums', albumId] }),
	});
}

export function useSwapFiles() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({
			albumId,
			targetAlbumId,
			...data
		}: SwapFileRequest & { albumId: string; targetAlbumId: string }) =>
			api.patch(`/album/${albumId}/album/${targetAlbumId}/swap`, data),
		onSuccess: () => qc.invalidateQueries({ queryKey: ['albums'] }),
	});
}

// ── Public album hooks (no auth required) ──

export function usePublicAlbums() {
	return useQuery({
		queryKey: ['public-albums'],
		queryFn: async () => {
			const res = await publicApi.get<AlbumResponse[]>('/public/albums');
			return res.data;
		},
	});
}

export function usePublicAlbumPhotos(
	albumId: string | undefined,
	options?: { width?: number; height?: number },
) {
	return useQuery({
		queryKey: ['public-albums', albumId, 'photos', options],
		queryFn: async () => {
			const params = new URLSearchParams();
			if (options?.width) params.set('width', String(options.width));
			if (options?.height) params.set('height', String(options.height));
			const res = await publicApi.get<string[]>(
				`/public/albums/${albumId}/photos?${params}`,
			);
			return res.data;
		},
		enabled: !!albumId,
	});
}

export function useSetAlbumPublic() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({
			albumId,
			isPublic,
		}: {
			albumId: string;
			isPublic: boolean;
		}) => api.patch(`/album/${albumId}/setPublic?isPublic=${isPublic}`),
		onSuccess: () => qc.invalidateQueries({ queryKey: ['albums'] }),
	});
}
