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

export interface PublicAlbumDto {
	albumId: string;
	name: string;
	photoCount: number;
}

export function getPublicPhotos(albumId: string): Promise<PublicPhotoDto[]> {
	return publicClient
		.get<PublicPhotoDto[]>(`/album/${albumId}/photos`)
		.then((res) => res.data);
}

export function getPublicAlbums(): Promise<PublicAlbumDto[]> {
	return publicClient
		.get<PublicAlbumDto[]>('/album/all')
		.then((res) => res.data);
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
