import { StatusBadge } from '../../components/shared/StatusBadge';
import {
	AlbumDetailView,
	type AlbumDetailConfig,
} from '../../components/album-detail/AlbumDetailView';
import {
	useAdminAlbums,
	useDeleteAlbum,
	useRemoveFiles,
	useSetFilesVisible,
	useAddWatermark,
	useSwapFiles,
	useRenameFile,
	useSetAlbumPublic,
	useSetAlbumTtd,
	useDownloadAlbum,
} from '../../hooks/useAdminAlbums';
import {
	getPhotoUrl,
	getAlbumFileNames,
	uploadFiles,
} from '../../api/adminApi';
import type { AlbumDto } from '@/shared/types/api';

/** Album administracyjny (portfolio) — admin jest w nim jednocześnie fotografem i klientem. */
function isAdminAlbum(album: AlbumDto) {
	return album.photographId === album.clientId;
}

function AlbumTypeField({ album }: { album: AlbumDto }) {
	const isAdmin = isAdminAlbum(album);
	return (
		<div>
			<p className='text-[10px] uppercase tracking-widest text-muted'>Typ</p>
			<StatusBadge variant={isAdmin ? 'accent' : 'default'}>
				{isAdmin ? 'Administracyjny' : 'Klienta'}
			</StatusBadge>
		</div>
	);
}

/** Publikacja w portfolio dotyczy tylko albumów administracyjnych. */
function AlbumPublicToggle({ album }: { album: AlbumDto }) {
	const publicMutation = useSetAlbumPublic();
	if (!isAdminAlbum(album)) return null;

	return (
		<div>
			<p className='text-[10px] uppercase tracking-widest text-muted mb-1'>
				Publiczny
			</p>
			<button
				onClick={() =>
					publicMutation.mutate({
						albumId: album.albumId,
						isPublic: !album.isPublic,
					})
				}
				className={`relative inline-flex h-6 w-11 items-center transition-colors ${
					album.isPublic ? 'bg-accent' : 'bg-border'
				}`}
			>
				<span
					className={`inline-block h-4 w-4 bg-foreground transition-transform ${
						album.isPublic ? 'translate-x-6' : 'translate-x-1'
					}`}
				/>
			</button>
		</div>
	);
}

const config: AlbumDetailConfig = {
	albumsQueryKey: ['panel', 'admin-albums'],
	albumsListPath: '/admin/albums',
	api: { getPhotoUrl, getAlbumFileNames, uploadFiles },
	hooks: {
		useAlbums: useAdminAlbums,
		useDeleteAlbum,
		useRemoveFiles,
		useSetFilesVisible,
		useAddWatermark,
		useSwapFiles,
		useRenameFile,
		useSetAlbumTtd,
		useDownloadAlbum,
	},
	// Album administracyjny nie ma klienta, więc TTD go nie dotyczy.
	canSetTtd: (album) => !isAdminAlbum(album),
	renderInfoBefore: (album) => <AlbumTypeField album={album} />,
	renderInfoAfter: (album) => <AlbumPublicToggle album={album} />,
};

export default function AdminAlbumDetail() {
	return <AlbumDetailView config={config} />;
}
