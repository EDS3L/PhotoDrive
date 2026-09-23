export type Inline = string | { b: string };

export type Block =
	| { k: 'p' | 'li' | 'li2' | 'h3' | 'code'; c: Inline[] }
	| { k: 'table'; rows: string[][] };

export interface StudyQuestion {
	n: number;
	title: string;
	short: Block[];
	long: Block[];
}
