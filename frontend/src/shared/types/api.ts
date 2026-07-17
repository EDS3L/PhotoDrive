export interface FileDto {
	fileId: string;
	fileName: string;
	sizeBytes: number;
	contentType: string;
	uploadedAt: string;
	visible: boolean;
	hasWatermark: boolean;
}

export interface AlbumDto {
	albumId: string;
	name: string;
	photographId: string | null;
	clientId: string | null;
	ttd: string | null;
	files: FileDto[];
	isPublic: boolean;
	/** Etykieta zakładki portfolio (pełny Unicode); null → strona pokazuje name. */
	displayName: string | null;
	/** Kolejność zakładki portfolio na stronie publicznej. */
	displayOrder: number;
}

export interface LoginRequest {
	email: string;
	password: string;
}

export interface ApiError {
	message: string;
	status: number;
}

export interface WatermarkStatusDto {
	configured: boolean;
	/** ISO timestamp ostatniej podmiany loga — cache-buster podglądu; null gdy brak. */
	updatedAt: string | null;
}

/** Slot strony wizytówki (panel admina) — każdy slot z backendu, także pusty. */
export interface SiteSlotDto {
	slot: string;
	configured: boolean;
	/** ISO timestamp ostatniej podmiany zdjęcia — wersja do cache-bustera; null gdy pusty. */
	updatedAt: string | null;
}
