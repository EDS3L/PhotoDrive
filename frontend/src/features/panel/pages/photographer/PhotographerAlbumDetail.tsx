import {
	AlbumDetailView,
	type AlbumDetailConfig,
} from '../../components/album-detail/AlbumDetailView';
import {
	usePhotographerAlbums,
	useDeleteAlbum,
	useRemoveFiles,
	useSetFilesVisible,
	useAddWatermark,
	useSwapFiles,
	useRenameFile,
	useSetAlbumTtd,
	useDownloadAlbum,
} from '../../hooks/usePhotographerAlbums';
import {
	getPhotoUrl,
	getAlbumFileNames,
	uploadFiles,
} from '../../api/photographerApi';

const config: AlbumDetailConfig = {
	albumsQueryKey: ['panel', 'photographer-albums'],
	albumsListPath: '/photographer/albums',
	api: { getPhotoUrl, getAlbumFileNames, uploadFiles },
	hooks: {
		useAlbums: usePhotographerAlbums,
		useDeleteAlbum,
		useRemoveFiles,
		useSetFilesVisible,
		useAddWatermark,
		useSwapFiles,
		useRenameFile,
		useSetAlbumTtd,
		useDownloadAlbum,
	},
};

export default function PhotographerAlbumDetail() {
	return <AlbumDetailView config={config} />;
}
