import { useQuery, useMutation } from '@tanstack/react-query';
import { queryClient } from '@/lib/queryClient';
import {
	getSiteSlots,
	uploadSiteSlotImage,
	deleteSiteSlotImage,
} from '../api/siteSlotsApi';

export function useAdminSiteSlots() {
	return useQuery({
		queryKey: ['panel', 'site-slots'],
		queryFn: getSiteSlots,
	});
}

export function useUploadSiteSlotImage() {
	return useMutation({
		mutationFn: ({ slot, file }: { slot: string; file: File }) =>
			uploadSiteSlotImage(slot, file),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'site-slots'] });
		},
	});
}

export function useDeleteSiteSlotImage() {
	return useMutation({
		mutationFn: (slot: string) => deleteSiteSlotImage(slot),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'site-slots'] });
		},
	});
}
