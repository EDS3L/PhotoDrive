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

/**
 * Rozmiary wariantów zdjęć na stronie publicznej. Backend i tak zacina dłuższy bok na 2560 px
 * (A9) — te wartości mówią tylko, ile NAPRAWDĘ potrzebujemy, żeby nie ciągnąć zbyt dużych plików.
 */
export const PUBLIC_PHOTO_SIZE = {
	/**
	 * Kafelek w siatce portfolio. Uwaga: kafelek jest KWADRATOWY (`object-cover`), a zdjęcia są
	 * poziome — przeglądarka kadruje, więc realnie wypełnia go KRÓTSZY bok. Przy 4:3 i limicie
	 * 1200 na dłuższym boku krótszy ma 900 px, co starcza na ekran o podwójnej gęstości.
	 * Mniejsza wartość (800) była widocznie rozmyta, bo obraz był powiększany do rozmiaru kafelka.
	 */
	tile: 1200,
	/** Duże zdjęcie (strona główna, sekcje „o mnie") — ma wyglądać świetnie na dużym ekranie. */
	full: 2048,
} as const;

export function getPublicPhotoUrl(
	albumId: string,
	fileName: string,
	width: number = PUBLIC_PHOTO_SIZE.full,
): string {
	return `/api/public/album/${albumId}/photo/${encodeURIComponent(fileName)}?width=${width}`;
}
