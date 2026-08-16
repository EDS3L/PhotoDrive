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
	displayName: string | null;
	photoCount: number;
}

export function getPublicAlbums(): Promise<PublicAlbumDto[]> {
	return publicClient.get<PublicAlbumDto[]>('/album/all').then((res) => res.data);
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

export const PUBLIC_PHOTO_SIZE = {
	tile: 1200,
	full: 2048,
} as const;

export function getPublicPhotoUrl(
	albumId: string,
	fileName: string,
	width: number = PUBLIC_PHOTO_SIZE.full,
): string {
	return `/api/public/album/${albumId}/photo/${encodeURIComponent(fileName)}?width=${width}`;
}

export type SiteSlotKey =
	| 'HOME_HERO'
	| 'HOME_INTRO'
	| 'HOME_CTA'
	| 'ABOUT_BIO'
	| 'ABOUT_EQUIPMENT'
	| 'CLIENT_LOGIN'
	| 'PANEL_LOGIN';

export interface PublicSiteSlotDto {
	slot: SiteSlotKey;
	version: number;
}

export function getPublicSiteSlots(): Promise<PublicSiteSlotDto[]> {
	return publicClient
		.get<PublicSiteSlotDto[]>('/site/slots')
		.then((res) => res.data);
}

export interface ContactRequest {
	name: string;
	email: string;
	phone?: string;
	sessionType: string;
	message: string;
}

export function sendContactMessage(payload: ContactRequest): Promise<void> {
	return publicClient.post('/contact', payload).then(() => undefined);
}

export function getSiteSlotPhotoUrl(
	slot: SiteSlotKey,
	version: number,
): string {
	return `/api/public/site/photo/${slot}?v=${version}`;
}
