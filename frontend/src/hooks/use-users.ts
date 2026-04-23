import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import type {
	UserDto,
	CreateUserRequest,
	RoleRequest,
	PasswordRequest,
	EmailRequest,
	AssignUserRequest,
} from '@/types/user';
import { normalizeUser } from '@/types/user';

export function useUsers() {
	return useQuery({
		queryKey: ['users'],
		queryFn: async () => {
			const res = await api.get<UserDto[]>('/user/all');
			return res.data.map(normalizeUser);
		},
	});
}

export function useActiveUsers() {
	return useQuery({
		queryKey: ['users', 'active'],
		queryFn: async () => {
			const res = await api.get<UserDto[]>('/user/activeUsers');
			return res.data.map(normalizeUser);
		},
	});
}

export function useAssignedUsers() {
	return useQuery({
		queryKey: ['users', 'assigned'],
		queryFn: async () => {
			const res = await api.get<UserDto[]>('/user/getAssignedUsers');
			return res.data.map(normalizeUser);
		},
	});
}

export function useCreateUser() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: async (data: CreateUserRequest) => {
			const res = await api.post<UserDto>('/user/add', data);
			return res.data;
		},
		onSuccess: () => {
			qc.invalidateQueries({ queryKey: ['users'] });
		},
	});
}

export function useAddRole() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: async ({
			userId,
			...data
		}: RoleRequest & { userId: string }) => {
			const res = await api.patch<UserDto>(`/user/${userId}/addRole`, data);
			return res.data;
		},
		onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
	});
}

export function useRemoveRole() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: async ({
			userId,
			...data
		}: RoleRequest & { userId: string }) => {
			const res = await api.patch<UserDto>(`/user/${userId}/removeRole`, data);
			return res.data;
		},
		onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
	});
}

export function useChangePassword() {
	return useMutation({
		mutationFn: ({ userId, ...data }: PasswordRequest & { userId: string }) =>
			api.patch(`/user/${userId}/changPassword`, data),
	});
}

export function useChangeEmail() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: async ({
			userId,
			...data
		}: EmailRequest & { userId: string }) => {
			const res = await api.patch<UserDto>(`/user/${userId}/changeEmail`, data);
			return res.data;
		},
		onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
	});
}

export function useActivateUser() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: async (userId: string) => {
			const res = await api.patch<UserDto>(
				`/user/${userId}/activateUser`,
				true,
			);
			return res.data;
		},
		onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
	});
}

export function useDeactivateUser() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: async (userId: string) => {
			const res = await api.patch<UserDto>(
				`/user/${userId}/deactivateUser`,
				false,
			);
			return res.data;
		},
		onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
	});
}

export function useAssignUsers() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({ userId, ...data }: AssignUserRequest & { userId: string }) =>
			api.patch(`/user/${userId}/assignUsers`, data),
		onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
	});
}

export function useRemoveUsers() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({ userId, ...data }: AssignUserRequest & { userId: string }) =>
			api.patch(`/user/${userId}/removeUsers`, data),
		onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
	});
}
