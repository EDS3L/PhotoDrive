import { useQuery } from '@tanstack/react-query';
import { getPublicAlbums } from '../../lib/publicApi';

/** Publiczne albumy (zakładki portfolio) — posortowane przez backend wg displayOrder. */
export function usePublicAlbums() {
	return useQuery({
		queryKey: ['public-albums'],
		queryFn: getPublicAlbums,
		// Krótki cache jak reszta strony publicznej: zmiany z panelu widoczne w ~pół minuty.
		staleTime: 30 * 1000,
	});
}
