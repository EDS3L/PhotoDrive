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

export function usePublicAlbumPhotos(
	albumName: string,
	width: number = PUBLIC_PHOTO_SIZE.full,
) {
	return useQuery<PublicPhoto[]>({
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
		staleTime: 30 * 1000,
	});
}
