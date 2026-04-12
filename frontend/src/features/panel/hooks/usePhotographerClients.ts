import { useQuery, useMutation } from '@tanstack/react-query';
import { queryClient } from '@/lib/queryClient';
import { getAssignedClients, createClient } from '../api/photographerApi';

export function usePhotographerClients() {
	return useQuery({
		queryKey: ['panel', 'photographer-clients'],
		queryFn: getAssignedClients,
	});
}

export function useCreateClient() {
	return useMutation({
		mutationFn: createClient,
		onSuccess: () => {
			queryClient.invalidateQueries({
				queryKey: ['panel', 'photographer-clients'],
			});
			queryClient.invalidateQueries({ queryKey: ['panel', 'users'] });
		},
	});
}
