import { useQuery, useMutation } from '@tanstack/react-query';
import { queryClient } from '@/lib/queryClient';
import {
	getAllUsers,
	createUser,
	activateUser,
	deactivateUser,
	assignUsersToPhotographer,
	removeUsersFromPhotographer,
	getPhotographerAssignedUsers,
} from '../api/adminApi';

export function useAllUsers() {
	return useQuery({
		queryKey: ['panel', 'users'],
		queryFn: getAllUsers,
	});
}

export function useCreateUser() {
	return useMutation({
		mutationFn: createUser,
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'users'] });
		},
	});
}

export function useActivateUser() {
	return useMutation({
		mutationFn: ({ id, active }: { id: string; active: boolean }) =>
			activateUser(id, active),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'users'] });
		},
	});
}

export function useDeactivateUser() {
	return useMutation({
		mutationFn: ({ id, active }: { id: string; active: boolean }) =>
			deactivateUser(id, active),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'users'] });
		},
	});
}

export function useAssignUsers() {
	return useMutation({
		mutationFn: ({
			photographerId,
			userIds,
		}: {
			photographerId: string;
			userIds: string[];
		}) => assignUsersToPhotographer(photographerId, userIds),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'users'] });
			queryClient.invalidateQueries({
				queryKey: ['panel', 'photographer-clients'],
			});
		},
	});
}

export function useRemoveAssignedUsers() {
	return useMutation({
		mutationFn: ({
			photographerId,
			userIds,
		}: {
			photographerId: string;
			userIds: string[];
		}) => removeUsersFromPhotographer(photographerId, userIds),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['panel', 'users'] });
			queryClient.invalidateQueries({
				queryKey: ['panel', 'photographer-clients'],
			});
		},
	});
}

export function usePhotographerAssignedUsers(photographerId: string | null) {
	return useQuery({
		queryKey: ['panel', 'photographer-clients', photographerId],
		queryFn: () => getPhotographerAssignedUsers(photographerId!),
		enabled: !!photographerId,
	});
}
