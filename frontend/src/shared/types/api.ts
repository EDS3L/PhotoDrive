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
	displayName: string | null;
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
	updatedAt: string | null;
}

export interface SiteSlotDto {
	slot: string;
	configured: boolean;
	updatedAt: string | null;
}
