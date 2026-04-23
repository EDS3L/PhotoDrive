import { useCallback, useState, type DragEvent } from 'react';

interface FileDropzoneProps {
	onFiles: (files: File[]) => void;
	accept?: string;
	maxFiles?: number;
	disabled?: boolean;
}

const ACCEPTED_TYPES = [
	'image/jpeg',
	'image/png',
	'image/bmp',
	'image/webp',
	'image/tiff',
	'image/heic',
];

export function FileDropzone({
	onFiles,
	accept,
	maxFiles,
	disabled,
}: FileDropzoneProps) {
	const [isDragOver, setIsDragOver] = useState(false);

	const handleDrop = useCallback(
		(e: DragEvent) => {
			e.preventDefault();
			setIsDragOver(false);
			if (disabled) return;
			const files = Array.from(e.dataTransfer.files).filter((f) =>
				ACCEPTED_TYPES.some(
					(t) => f.type === t || f.name.toLowerCase().endsWith('.heic'),
				),
			);
			const limited = maxFiles ? files.slice(0, maxFiles) : files;
			if (limited.length > 0) onFiles(limited);
		},
		[onFiles, maxFiles, disabled],
	);

	const handleFileInput = useCallback(
		(e: React.ChangeEvent<HTMLInputElement>) => {
			if (disabled) return;
			const files = Array.from(e.target.files ?? []);
			const limited = maxFiles ? files.slice(0, maxFiles) : files;
			if (limited.length > 0) onFiles(limited);
			e.target.value = '';
		},
		[onFiles, maxFiles, disabled],
	);

	return (
		<div
			onDragOver={(e) => {
				e.preventDefault();
				if (!disabled) setIsDragOver(true);
			}}
			onDragLeave={() => setIsDragOver(false)}
			onDrop={handleDrop}
			className={`relative flex flex-col items-center justify-center h-[400px] border-2 border-dashed
        transition-all duration-300 cursor-pointer
        ${isDragOver ? 'border-primary bg-primary/5' : 'border-outline-variant/30 hover:border-primary/50'}
        ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
		>
			<span className='material-symbols-outlined text-5xl text-on-surface-variant/50 mb-4'>
				cloud_upload
			</span>
			<p className='text-on-surface-variant text-sm mb-1'>
				Przeciągnij pliki tutaj
			</p>
			<p className='text-on-surface-variant/50 text-xs mb-4'>lub</p>
			<label
				className='px-6 py-2.5 border border-on-surface/30 text-on-surface text-[11px] uppercase tracking-[0.25em]
        font-medium cursor-pointer hover:bg-surface-container transition-colors'
			>
				Wybierz pliki
				<input
					type='file'
					multiple
					accept={accept ?? ACCEPTED_TYPES.join(',')}
					onChange={handleFileInput}
					className='hidden'
					disabled={disabled}
				/>
			</label>
			<p className='text-on-surface-variant/40 text-[10px] mt-4'>
				JPG, PNG, BMP, WebP, TIFF, HEIC
			</p>
		</div>
	);
}
