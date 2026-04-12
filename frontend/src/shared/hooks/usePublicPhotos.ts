import { useQuery } from '@tanstack/react-query';
import {
	getPublicPhotosByAlbumName,
	getPublicPhotoUrl,
} from '../../lib/publicApi';

interface PublicPhoto {
	fileId: string;
	fileName: string;
	url: string;
}

export function usePublicAlbumPhotos(albumName: string) {
	return useQuery<PublicPhoto[]>({
		queryKey: ['public-album-photos', albumName],
		queryFn: async () => {
			const response = await getPublicPhotosByAlbumName(albumName);
			return response.photos.map((p) => ({
				fileId: p.fileId,
				fileName: p.fileName,
				url: getPublicPhotoUrl(response.albumId, p.fileName),
			}));
		},
		enabled: albumName.length > 0,
		staleTime: 5 * 60 * 1000,
	});
}
