import { useQuery, useMutation } from '@tanstack/react-query';
import { queryClient } from '@/lib/queryClient';
import {
	getAssignedAlbums,
	getAssignedAlbumsWithoutTtd,
	createClientAlbum,
	setAlbumTtd,
	deleteAlbum,
	uploadFiles,
	removeFiles,
	setFilesVisible,
	addWatermark,
	swapFiles,
	renameFile,
	downloadAlbum,
} from '../api/photographerApi';

export function usePhotographerAlbums() {
	return useQuery({
		queryKey: ['panel', 'photographer-albums'],
		queryFn: getAssignedAlbums,
	});
}

export function usePhotographerAlbumsWithoutTtd() {
	return useQuery({
		queryKey: ['panel', 'photographer-albums-no-ttd'],
		queryFn: getAssignedAlbumsWithoutTtd,
	});
}

export function useCreateClientAlbum() {
	return useMutation({
		mutationFn: ({ clientId, name }: { clientId: string; name: string }) =>
			createClientAlbum(clientId, name),
		onSuccess: () => {
			queryClient.invalidateQueries({
				queryKey: ['panel', 'photographer-albums'],
			});
		},
	});
}

export function useSetAlbumTtd() {
	return useMutation({
		mutationFn: ({ albumId, ttd }: { albumId: string; ttd: string }) =>
			setAlbumTtd(albumId, ttd),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'photographer-albums'] });
			queryClient.invalidateQueries({ queryKey: ['panel', 'photographer-albums-no-ttd'] });
		},
	});
}

export function useDeleteAlbum() {
	return useMutation({
		mutationFn: (albumId: string) => deleteAlbum(albumId),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'photographer-albums'] });
			queryClient.invalidateQueries({ queryKey: ['panel', 'photographer-albums-no-ttd'] });
		},
	});
}

export function useUploadFiles() {
	return useMutation({
		mutationFn: ({ albumId, files }: { albumId: string; files: File[] }) =>
			uploadFiles(albumId, files),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'photographer-albums'] });
		},
	});
}

export function useRemoveFiles() {
	return useMutation({
		mutationFn: ({
			albumId,
			fileIds,
		}: {
			albumId: string;
			fileIds: string[];
		}) => removeFiles(albumId, fileIds),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'photographer-albums'] });
		},
	});
}

export function useSetFilesVisible() {
	return useMutation({
		mutationFn: ({
			albumId,
			fileIds,
			visible,
		}: {
			albumId: string;
			fileIds: string[];
			visible: boolean;
		}) => setFilesVisible(albumId, fileIds, visible),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'photographer-albums'] });
		},
	});
}

export function useAddWatermark() {
	return useMutation({
		mutationFn: ({
			albumId,
			fileIds,
			hasWatermark,
		}: {
			albumId: string;
			fileIds: string[];
			hasWatermark: boolean;
		}) => addWatermark(albumId, fileIds, hasWatermark),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'photographer-albums'] });
		},
	});
}

export function useSwapFiles() {
	return useMutation({
		mutationFn: ({
			sourceAlbumId,
			targetAlbumId,
			fileIds,
		}: {
			sourceAlbumId: string;
			targetAlbumId: string;
			fileIds: string[];
		}) => swapFiles(sourceAlbumId, targetAlbumId, fileIds),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'photographer-albums'] });
		},
	});
}

export function useRenameFile() {
	return useMutation({
		mutationFn: ({
			albumId,
			fileId,
			newName,
		}: {
			albumId: string;
			fileId: string;
			newName: string;
		}) => renameFile(albumId, fileId, newName),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'photographer-albums'] });
		},
	});
}

export function useDownloadAlbum() {
	return useMutation({
		mutationFn: ({
			albumId,
			fileList,
		}: {
			albumId: string;
			fileList: string[];
		}) => downloadAlbum(albumId, fileList),
	});
}
