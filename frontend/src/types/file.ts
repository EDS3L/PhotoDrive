export interface FileDto {
	fileID: string;
	fileName: string;
	sizeBytes: number;
	contentType: string;
	isVisible?: boolean;
	hasWatermark?: boolean;
}

export interface UploadResponse {
	responseFile: UploadResponseFile[];
	message: string;
}

export interface UploadResponseFile {
	id: string;
	fileName: string;
}
