import { useQuery } from '@tanstack/react-query';
import { getPublicAlbums } from '../../lib/publicApi';

export function usePublicAlbums() {
	return useQuery({
		queryKey: ['public-albums'],
		queryFn: getPublicAlbums,
		staleTime: 30 * 1000,
	});
}
