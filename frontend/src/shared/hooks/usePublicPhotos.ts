import { useQuery } from '@tanstack/react-query';
import {
	getPublicPhotosByAlbumName,
	getPublicPhotoUrl,
	PUBLIC_PHOTO_SIZE,
} from '../../lib/publicApi';

interface PublicPhoto {
	fileId: string;
	fileName: string;
	url: string;
}

/**
 * @param width rozmiar wariantu (patrz `PUBLIC_PHOTO_SIZE`) — siatka portfolio prosi o kafelek,
 * strona główna o duże zdjęcie. Domyślnie duże, bo tak wygląda większość użyć.
 */
export function usePublicAlbumPhotos(
	albumName: string,
	width: number = PUBLIC_PHOTO_SIZE.full,
) {
	return useQuery<PublicPhoto[]>({
		// width w kluczu: inaczej siatka i strona główna dzieliłyby jeden cache i jedna z nich
		// dostałaby URL-e z cudzym rozmiarem.
		queryKey: ['public-album-photos', albumName, width],
		queryFn: async () => {
			const response = await getPublicPhotosByAlbumName(albumName);
			return response.photos.map((p) => ({
				fileId: p.fileId,
				fileName: p.fileName,
				url: getPublicPhotoUrl(response.albumId, p.fileName, width),
			}));
		},
		enabled: albumName.length > 0,
		// Krótki cache, żeby zmiany w panelu (dodanie/usunięcie zdjęcia) pojawiały
		// się na stronie publicznej w ~pół minuty, nie po 5 min.
		staleTime: 30 * 1000,
	});
}
