import axios from 'axios';

const publicClient = axios.create({
	baseURL: '/api/public',
	headers: {
		'Content-Type': 'application/json',
	},
});

export interface PublicPhotoDto {
	fileId: string;
	fileName: string;
}

export interface PublicAlbumPhotosResponse {
	albumId: string;
	name: string;
	photos: PublicPhotoDto[];
}

export function getPublicPhotosByAlbumName(
	name: string,
): Promise<PublicAlbumPhotosResponse> {
	return publicClient
		.get<PublicAlbumPhotosResponse>(
			`/album/by-name/${encodeURIComponent(name)}`,
		)
		.then((res) => res.data);
}

export function getPublicPhotoUrl(albumId: string, fileName: string): string {
	return `/api/public/album/${albumId}/photo/${encodeURIComponent(fileName)}`;
}
