export interface FileDto {
	fileID: string;
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
	photographId: string;
	clientId: string;
	ttd: string | null;
	files: FileDto[];
	albumPath: string;
	isPublic: boolean;
}

export interface LoginRequest {
	email: string;
	password: string;
}

export interface ApiError {
	message: string;
	status: number;
}
