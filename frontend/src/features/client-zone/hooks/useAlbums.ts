import { useQuery } from '@tanstack/react-query';
import { getAssignedAlbums } from '../api/clientZoneApi';
import { useAuthStore } from '@/app/store/authStore';

export function useAlbums() {
	const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

	return useQuery({
		queryKey: ['albums'],
		queryFn: getAssignedAlbums,
		enabled: isAuthenticated,
	});
}
