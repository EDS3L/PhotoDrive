import { useQuery, useMutation } from '@tanstack/react-query';
import { queryClient } from '@/lib/queryClient';
import {
	getAllAlbums,
	getAllAlbumsWithoutTtd,
	createAdminAlbum,
	setAlbumPublic,
	setAlbumTtd,
	deleteAlbum,
	removeFiles,
	setFilesVisible,
	addWatermark,
	swapFiles,
	renameFile,
	downloadAlbum,
} from '../api/adminApi';

export function useAdminAlbums() {
	return useQuery({
		queryKey: ['panel', 'admin-albums'],
		queryFn: getAllAlbums,
	});
}

export function useAdminAlbumsWithoutTtd() {
	return useQuery({
		queryKey: ['panel', 'admin-albums-no-ttd'],
		queryFn: getAllAlbumsWithoutTtd,
	});
}

export function useCreateAdminAlbum() {
	return useMutation({
		mutationFn: (name: string) => createAdminAlbum(name),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'admin-albums'] });
		},
	});
}

export function useSetAlbumPublic() {
	return useMutation({
		mutationFn: ({
			albumId,
			isPublic,
		}: {
			albumId: string;
			isPublic: boolean;
		}) => setAlbumPublic(albumId, isPublic),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'admin-albums'] });
		},
	});
}

export function useSetAlbumTtd() {
	return useMutation({
		mutationFn: ({ albumId, ttd }: { albumId: string; ttd: string }) =>
			setAlbumTtd(albumId, ttd),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'admin-albums'] });
			queryClient.invalidateQueries({ queryKey: ['panel', 'admin-albums-no-ttd'] });
		},
	});
}

export function useDeleteAlbum() {
	return useMutation({
		mutationFn: (albumId: string) => deleteAlbum(albumId),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'admin-albums'] });
			queryClient.invalidateQueries({ queryKey: ['panel', 'admin-albums-no-ttd'] });
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
			queryClient.invalidateQueries({ queryKey: ['panel', 'admin-albums'] });
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
			queryClient.invalidateQueries({ queryKey: ['panel', 'admin-albums'] });
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
			queryClient.invalidateQueries({ queryKey: ['panel', 'admin-albums'] });
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
			queryClient.invalidateQueries({ queryKey: ['panel', 'admin-albums'] });
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
			queryClient.invalidateQueries({ queryKey: ['panel', 'admin-albums'] });
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
