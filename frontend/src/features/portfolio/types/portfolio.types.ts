export type PortfolioCategory = 'sluby' | 'plener' | 'portret' | 'reportaz';

export interface PortfolioPhoto {
	id: string;
	src: string;
	alt: string;
	category: PortfolioCategory;
}

export interface PortfolioTab {
	id: PortfolioCategory;
	label: string;
	icon: string;
}
