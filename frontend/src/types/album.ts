import type { FileDto } from './file';

/** Raw DTO from backend */
export interface AlbumDto {
	albumId: string;
	name: string;
	photographId: string | null;
	clientId: string | null;
	ttd: string | null;
	photos: Record<string, FileDto>;
	albumPath: string | { value: string } | null;
	isPublic?: boolean;
}

/** Flattened album for frontend consumption */
export interface Album {
	albumId: string;
	name: string;
	photographId: string | null;
	clientId: string | null;
	ttd: string | null;
	photoCount: number;
	isPublic: boolean;
}

export function normalizeAlbum(dto: AlbumDto): Album {
	return {
		albumId: dto.albumId,
		name: dto.name,
		photographId: dto.photographId,
		clientId: dto.clientId,
		ttd: dto.ttd,
		photoCount: dto.photos ? Object.keys(dto.photos).length : 0,
		isPublic: dto.isPublic ?? false,
	};
}

export interface AlbumResponse {
	albumId: string;
	name: string;
	photographerId: string | null;
	clientId: string | null;
	files: FileResponse[];
}

export interface FileResponse {
	fileId: string;
	fileName: string;
	sizeBytes: number;
	contentType: string;
}

export interface CreateAlbumRequest {
	name: string;
	isPublic?: boolean;
}

export interface CreateClientAlbumRequest {
	name: string;
}

export interface ChangeVisibleRequest {
	idList: string[];
}

export interface ChangeWatermarkRequest {
	filesUUIDList: string[];
}

export interface SwapFileRequest {
	fileIdList: string[];
}

export interface DownloadFilesRequest {
	fileList: string[];
}

export interface RenameFileRequest {
	newFileName: string;
}

export interface RemoveFilesRequest {
	fileIdList: string[];
}
