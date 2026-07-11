import { useQuery, useMutation } from '@tanstack/react-query';
import { queryClient } from '@/lib/queryClient';
import {
	getWatermarkStatus,
	uploadWatermark,
	deleteWatermark,
} from '../api/watermarkApi';

export function useWatermarkStatus() {
	return useQuery({
		queryKey: ['panel', 'watermark-status'],
		queryFn: getWatermarkStatus,
	});
}

export function useUploadWatermark() {
	return useMutation({
		mutationFn: (file: File) => uploadWatermark(file),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'watermark-status'] });
		},
	});
}

export function useDeleteWatermark() {
	return useMutation({
		mutationFn: deleteWatermark,
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'watermark-status'] });
		},
	});
}
