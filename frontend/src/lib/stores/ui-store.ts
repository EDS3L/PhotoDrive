import { create } from 'zustand';

interface UiState {
	sidebarOpen: boolean;
	toggleSidebar: () => void;
	setSidebarOpen: (open: boolean) => void;
	activeModal: string | null;
	modalData: unknown;
	openModal: (name: string, data?: unknown) => void;
	closeModal: () => void;
	selectedFiles: string[];
	toggleFileSelection: (fileId: string) => void;
	selectAllFiles: (fileIds: string[]) => void;
	clearSelection: () => void;
	isSelectionMode: boolean;
	selectionMode: boolean;
	setSelectionMode: (mode: boolean) => void;
	toggleSelectionMode: () => void;
	toggleFile: (fileId: string) => void;
}

export const useUiStore = create<UiState>((set, get) => ({
	sidebarOpen: false,
	toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
	setSidebarOpen: (open) => set({ sidebarOpen: open }),

	activeModal: null,
	modalData: null,
	openModal: (name, data = undefined) =>
		set({ activeModal: name, modalData: data ?? null }),
	closeModal: () => set({ activeModal: null, modalData: null }),

	selectedFiles: [],
	toggleFileSelection: (fileId) => {
		const { selectedFiles } = get();
		set({
			selectedFiles: selectedFiles.includes(fileId)
				? selectedFiles.filter((id) => id !== fileId)
				: [...selectedFiles, fileId],
		});
	},
	selectAllFiles: (fileIds) => set({ selectedFiles: fileIds }),
	clearSelection: () => set({ selectedFiles: [], isSelectionMode: false }),
	isSelectionMode: false,
	get selectionMode() {
		return this.isSelectionMode;
	},
	setSelectionMode: (mode) =>
		set({
			isSelectionMode: mode,
			selectedFiles: mode ? get().selectedFiles : [],
		}),
	toggleSelectionMode: () => {
		const current = get().isSelectionMode;
		set({
			isSelectionMode: !current,
			selectedFiles: !current ? get().selectedFiles : [],
		});
	},
	toggleFile: (fileId) => {
		const { selectedFiles } = get();
		set({
			selectedFiles: selectedFiles.includes(fileId)
				? selectedFiles.filter((id) => id !== fileId)
				: [...selectedFiles, fileId],
		});
	},
}));
